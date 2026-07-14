package net.minecraft.client.texture;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceReloader.Synchronizer;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;

public abstract class SpriteAtlasHolder implements ResourceReloader, AutoCloseable {
   private final SpriteAtlasTexture atlas;
   private final Identifier sourcePath;
   private final Set<ResourceMetadataSerializer<?>> metadataReaders;

   public SpriteAtlasHolder(TextureManager textureManager, Identifier atlasId, Identifier sourcePath) {
      this(textureManager, atlasId, sourcePath, SpriteLoader.METADATA_SERIALIZERS);
   }

   public SpriteAtlasHolder(TextureManager textureManager, Identifier atlasId, Identifier sourcePath, Set<ResourceMetadataSerializer<?>> metadataReaders) {
      this.sourcePath = sourcePath;
      this.atlas = new SpriteAtlasTexture(atlasId);
      textureManager.registerTexture(this.atlas.getId(), this.atlas);
      this.metadataReaders = metadataReaders;
   }

   protected Sprite getSprite(Identifier objectId) {
      return this.atlas.getSprite(objectId);
   }

   public final CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
      return SpriteLoader.fromAtlas(this.atlas)
         .load(manager, this.sourcePath, 0, prepareExecutor, this.metadataReaders)
         .thenCompose(SpriteLoader.StitchResult::whenComplete)
         .<SpriteLoader.StitchResult>thenCompose(synchronizer::whenPrepared)
         .thenAcceptAsync(this::afterReload, applyExecutor);
   }

   private void afterReload(SpriteLoader.StitchResult stitchResult) {
      ScopedProfiler scopedProfiler = Profilers.get().scoped("upload");

      try {
         this.atlas.upload(stitchResult);
      } catch (Throwable var6) {
         if (scopedProfiler != null) {
            try {
               scopedProfiler.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (scopedProfiler != null) {
         scopedProfiler.close();
      }
   }

   @Override
   public void close() {
      this.atlas.clear();
   }
}
