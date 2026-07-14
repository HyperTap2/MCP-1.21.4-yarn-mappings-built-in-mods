package net.irisshaders.iris.pbr.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.loader.AtlasPBRLoader;
import net.irisshaders.iris.pbr.util.TextureManipulationUtil;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.Sprite.TickableAnimation;
import net.minecraft.client.texture.SpriteContents.AnimationFrame;
import net.minecraft.client.texture.SpriteContents.AnimatorImpl;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;

public class PBRAtlasTexture extends AbstractTexture implements PBRDumpable {
   protected final SpriteAtlasTexture atlasTexture;
   protected final PBRType type;
   protected final Identifier id;
   protected final Map<Identifier, AtlasPBRLoader.PBRTextureAtlasSprite> texturesByName = new HashMap<>();
   protected final List<TickableAnimation> animatedTextures = new ArrayList<>();
   protected int width;
   protected int height;
   protected int mipLevel;

   public PBRAtlasTexture(SpriteAtlasTexture atlasTexture, PBRType type) {
      this.atlasTexture = atlasTexture;
      this.type = type;
      this.id = Identifier.of(atlasTexture.getId().getNamespace(), atlasTexture.getId().getPath().replace(".png", "") + type.getSuffix() + ".png");
      this.setFilter(false, true);
   }

   public static void syncAnimation(AnimatorImpl source, AnimatorImpl target) {
      List<AnimationFrame> sourceFrames = source.getAnimationInfo().getFrames();
      int ticks = 0;

      for (int f = 0; f < source.getFrame(); f++) {
         ticks += sourceFrames.get(f).time();
      }

      List<AnimationFrame> targetFrames = target.getAnimationInfo().getFrames();
      int cycleTime = 0;
      int frameCount = targetFrames.size();

      for (AnimationFrame frame : targetFrames) {
         cycleTime += frame.time();
      }

      ticks %= cycleTime;
      int targetFrame = 0;

      while (true) {
         int time = targetFrames.get(targetFrame).time();
         if (ticks < time) {
            target.setFrame(targetFrame);
            target.setSubFrame(ticks + source.getSubFrame());
            return;
         }

         targetFrame++;
         ticks -= time;
      }
   }

   protected static void dumpSpriteNames(Path dir, String fileName, Map<Identifier, AtlasPBRLoader.PBRTextureAtlasSprite> sprites) {
      Path path = dir.resolve(fileName + ".txt");

      try (BufferedWriter writer = Files.newBufferedWriter(path)) {
         for (Entry<Identifier, AtlasPBRLoader.PBRTextureAtlasSprite> entry : sprites.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
            AtlasPBRLoader.PBRTextureAtlasSprite sprite = entry.getValue();
            writer.write(
               String.format(
                  Locale.ROOT,
                  "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                  entry.getKey(),
                  sprite.getX(),
                  sprite.getY(),
                  sprite.getContents().getWidth(),
                  sprite.getContents().getHeight()
               )
            );
         }
      } catch (IOException e) {
         Iris.logger.warn("Failed to write file {}", path, e);
      }
   }

   public PBRType getType() {
      return this.type;
   }

   public Identifier getAtlasId() {
      return this.id;
   }

   public void addSprite(AtlasPBRLoader.PBRTextureAtlasSprite sprite) {
      this.texturesByName.put(sprite.getContents().getId(), sprite);
   }

   @Nullable
   public AtlasPBRLoader.PBRTextureAtlasSprite getSprite(Identifier id) {
      return this.texturesByName.get(id);
   }

   public void clear() {
      this.animatedTextures.forEach(TickableAnimation::close);
      this.texturesByName.clear();
      this.animatedTextures.clear();
   }

   public void upload(int atlasWidth, int atlasHeight, int mipLevel) {
      int glId = this.getGlId();
      TextureUtil.prepareImage(glId, mipLevel, atlasWidth, atlasHeight);
      TextureManipulationUtil.fillWithColor(glId, mipLevel, this.type.getDefaultValue());
      this.width = atlasWidth;
      this.height = atlasHeight;
      this.mipLevel = mipLevel;

      for (AtlasPBRLoader.PBRTextureAtlasSprite sprite : this.texturesByName.values()) {
         try {
            this.uploadSprite(sprite);
         } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.create(throwable, "Stitching texture atlas");
            CrashReportSection crashReportCategory = crashReport.addElement("Texture being stitched together");
            crashReportCategory.add("Atlas path", this.id);
            crashReportCategory.add("Sprite", sprite);
            throw new CrashException(crashReport);
         }
      }

      PBRAtlasHolder pbrHolder = this.atlasTexture.getOrCreatePBRHolder();
      switch (this.type) {
         case NORMAL:
            pbrHolder.setNormalAtlas(this);
            break;
         case SPECULAR:
            pbrHolder.setSpecularAtlas(this);
      }
   }

   public boolean tryUpload(int atlasWidth, int atlasHeight, int mipLevel) {
      try {
         this.upload(atlasWidth, atlasHeight, mipLevel);
         return true;
      } catch (Throwable t) {
         return false;
      }
   }

   protected void uploadSprite(AtlasPBRLoader.PBRTextureAtlasSprite sprite) {
      TickableAnimation spriteTicker = sprite.createAnimation();
      if (spriteTicker != null) {
         this.animatedTextures.add(spriteTicker);
         AnimatorImpl sourceTicker = sprite.getBaseSprite().getContents().getCreatedTicker();
         AnimatorImpl targetTicker = sprite.getContents().getCreatedTicker();
         if (sourceTicker != null && targetTicker != null) {
            syncAnimation(sourceTicker, targetTicker);
            SpriteContents.Animation animation = targetTicker.getAnimationInfo();
            animation.invokeUploadFrame(
               sprite.getX(), sprite.getY(), animation.getFrames().get(targetTicker.getFrame()).index()
            );
            return;
         }
      }

      sprite.upload();
   }

   public void cycleAnimationFrames() {
      this.bindTexture();

      for (TickableAnimation ticker : this.animatedTextures) {
         ticker.tick();
      }
   }

   public void close() {
      PBRAtlasHolder pbrHolder = this.atlasTexture.getPBRHolder();
      if (pbrHolder != null) {
         switch (this.type) {
            case NORMAL:
               pbrHolder.setNormalAtlas(null);
               break;
            case SPECULAR:
               pbrHolder.setSpecularAtlas(null);
         }
      }

      this.clear();
   }

   public void save(Identifier id, Path path) {
      String fileName = id.toUnderscoreSeparatedString();
      TextureUtil.writeAsPNG(path, fileName, this.getGlId(), this.mipLevel, this.width, this.height);
      dumpSpriteNames(path, fileName, this.texturesByName);
   }

   @Override
   public Identifier getDefaultDumpLocation() {
      return this.id;
   }
}
