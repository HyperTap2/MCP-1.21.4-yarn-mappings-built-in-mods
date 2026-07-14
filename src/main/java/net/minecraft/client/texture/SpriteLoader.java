package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.atlas.AtlasLoader;
import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import me.pepperbell.continuity.client.resource.AtlasLoaderInitContext;
import me.pepperbell.continuity.client.resource.AtlasLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderStitchContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;

public class SpriteLoader {
   public static final Set<ResourceMetadataSerializer<?>> METADATA_SERIALIZERS = Set.of(AnimationResourceMetadata.SERIALIZER);
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier id;
   private final int maxTextureSize;
   private final int width;
   private final int height;

   public SpriteLoader(Identifier id, int maxTextureSize, int width, int height) {
      this.id = id;
      this.maxTextureSize = maxTextureSize;
      this.width = width;
      this.height = height;
   }

   public static SpriteLoader fromAtlas(SpriteAtlasTexture atlasTexture) {
      return new SpriteLoader(atlasTexture.getId(), atlasTexture.getMaxTextureSize(), atlasTexture.getWidth(), atlasTexture.getHeight());
   }

   public SpriteLoader.StitchResult stitch(List<SpriteContents> sprites, int mipLevel, Executor executor) {
      int i = this.maxTextureSize;
      TextureStitcher<SpriteContents> textureStitcher = new TextureStitcher<>(i, i, mipLevel);
      int j = Integer.MAX_VALUE;
      int k = 1 << mipLevel;

      for (SpriteContents spriteContents : sprites) {
         j = Math.min(j, Math.min(spriteContents.getWidth(), spriteContents.getHeight()));
         int l = Math.min(Integer.lowestOneBit(spriteContents.getWidth()), Integer.lowestOneBit(spriteContents.getHeight()));
         if (l < k) {
            LOGGER.warn(
               "Texture {} with size {}x{} limits mip level from {} to {}",
               new Object[]{spriteContents.getId(), spriteContents.getWidth(), spriteContents.getHeight(), MathHelper.floorLog2(k), MathHelper.floorLog2(l)}
            );
            k = l;
         }

         textureStitcher.add(spriteContents);
      }

      int m = Math.min(j, k);
      int n = MathHelper.floorLog2(m);
      int l;
      if (n < mipLevel) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.id, mipLevel, n, m});
         l = n;
      } else {
         l = mipLevel;
      }

      try {
         textureStitcher.stitch();
      } catch (TextureStitcherCannotFitException textureStitcherCannotFitException) {
         CrashReport crashReport = CrashReport.create(textureStitcherCannotFitException, "Stitching");
         CrashReportSection crashReportSection = crashReport.addElement("Stitcher");
         crashReportSection.add(
            "Sprites",
            textureStitcherCannotFitException.getSprites()
               .stream()
               .map(spritex -> String.format(Locale.ROOT, "%s[%dx%d]", spritex.getId(), spritex.getWidth(), spritex.getHeight()))
               .collect(Collectors.joining(","))
         );
         crashReportSection.add("Max Texture Size", i);
         throw new CrashException(crashReport);
      }

      int o = Math.max(textureStitcher.getWidth(), this.width);
      int p = Math.max(textureStitcher.getHeight(), this.height);
      Map<Identifier, Sprite> map = this.collectStitchedSprites(textureStitcher, o, p);
      Sprite sprite = map.get(MissingSprite.getMissingSpriteId());
      CompletableFuture<Void> completableFuture;
      if (l > 0) {
         completableFuture = CompletableFuture.runAsync(() -> map.values().forEach(spritex -> spritex.getContents().generateMipmaps(l)), executor);
      } else {
         completableFuture = CompletableFuture.completedFuture(null);
      }

      SpriteLoader.StitchResult result = new SpriteLoader.StitchResult(o, p, l, sprite, map, completableFuture);
      SpriteLoaderStitchContext continuityContext = SpriteLoaderStitchContext.THREAD_LOCAL.get();
      if (continuityContext != null) {
         continuityContext.getEmissiveIdMap().forEach((id, emissiveId) -> {
            Sprite base = map.get(id);
            Sprite emissive = map.get(emissiveId);
            if (base != null && emissive != null) {
               ((SpriteExtension)base).continuity$setEmissiveSprite(emissive);
               continuityContext.markHasEmissives();
            }
         });
      }
      return result;
   }

   public static CompletableFuture<List<SpriteContents>> loadAll(SpriteOpener opener, List<Function<SpriteOpener, SpriteContents>> sources, Executor executor) {
      List<CompletableFuture<SpriteContents>> list = sources.stream()
         .map(sprite -> CompletableFuture.supplyAsync(() -> (SpriteContents)sprite.apply(opener), executor))
         .toList();
      return Util.combineSafe(list).thenApply(sprites -> sprites.stream().filter(Objects::nonNull).toList());
   }

   public CompletableFuture<SpriteLoader.StitchResult> load(ResourceManager resourceManager, Identifier path, int mipLevel, Executor executor) {
      return this.load(resourceManager, path, mipLevel, executor, METADATA_SERIALIZERS);
   }

   public CompletableFuture<SpriteLoader.StitchResult> load(
      ResourceManager resourceManager, Identifier path, int mipLevel, Executor executor, Collection<ResourceMetadataSerializer<?>> metadatas
   ) {
      SpriteOpener spriteOpener = SpriteOpener.create(metadatas);
      SpriteLoaderLoadContext continuityContext = SpriteLoaderLoadContext.THREAD_LOCAL.get();
      CompletableFuture<Set<Identifier>> extraIdsFuture = continuityContext == null ? null : continuityContext.getExtraIdsFuture(this.id);
      SpriteLoaderLoadContext.EmissiveControl emissiveControl = continuityContext == null ? null : continuityContext.getEmissiveControl(this.id);
      Supplier<List<Function<SpriteOpener, SpriteContents>>> sourceSupplier = () -> {
         if (extraIdsFuture != null) {
            AtlasLoaderInitContext.THREAD_LOCAL.set(extraIdsFuture::join);
         }
         if (emissiveControl != null) {
            AtlasLoaderLoadContext.THREAD_LOCAL.set(emissiveControl::setEmissiveIdMap);
         }
         try {
            return AtlasLoader.of(resourceManager, path).loadSources(resourceManager);
         } finally {
            AtlasLoaderInitContext.THREAD_LOCAL.remove();
            AtlasLoaderLoadContext.THREAD_LOCAL.remove();
         }
      };
      return CompletableFuture.supplyAsync(sourceSupplier, executor)
         .thenCompose(sources -> loadAll(spriteOpener, (List<Function<SpriteOpener, SpriteContents>>)sources, executor))
         .thenApply(sprites -> {
            Map<Identifier, Identifier> emissiveIds = emissiveControl == null ? null : emissiveControl.getEmissiveIdMap();
            if (emissiveIds == null) {
               return this.stitch((List<SpriteContents>)sprites, mipLevel, executor);
            }
            SpriteLoaderStitchContext.THREAD_LOCAL.set(new SpriteLoaderStitchContext() {
               @Override
               public Map<Identifier, Identifier> getEmissiveIdMap() {
                  return emissiveIds;
               }

               @Override
               public void markHasEmissives() {
                  emissiveControl.markHasEmissives();
               }
            });
            try {
               return this.stitch((List<SpriteContents>)sprites, mipLevel, executor);
            } finally {
               SpriteLoaderStitchContext.THREAD_LOCAL.remove();
            }
         });
   }

   private Map<Identifier, Sprite> collectStitchedSprites(TextureStitcher<SpriteContents> stitcher, int atlasWidth, int atlasHeight) {
      Map<Identifier, Sprite> map = new HashMap<>();
      stitcher.getStitchedSprites((info, x, y) -> map.put(info.getId(), new Sprite(this.id, info, atlasWidth, atlasHeight, x, y)));
      return map;
   }

   public record StitchResult(int width, int height, int mipLevel, Sprite missing, Map<Identifier, Sprite> regions, CompletableFuture<Void> readyForUpload) {
      public CompletableFuture<SpriteLoader.StitchResult> whenComplete() {
         return this.readyForUpload.thenApply(void_ -> this);
      }
   }
}
