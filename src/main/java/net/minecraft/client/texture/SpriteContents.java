package net.minecraft.client.texture;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.pbr.texture.PBRSpriteHolder;
import net.minecraft.client.resource.metadata.AnimationFrameResourceMetadata;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class SpriteContents implements
   TextureStitcher.Stitchable,
   AutoCloseable,
   SpriteContentsExtension,
   net.irisshaders.iris.pbr.SpriteContentsExtension,
   net.irisshaders.iris.pbr.texture.SpriteContentsExtension {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier id;
   final int width;
   final int height;
   private final NativeImage image;
   NativeImage[] mipmapLevelsImages;
   @Nullable
   private final SpriteContents.Animation animation;
   private final ResourceMetadata metadata;
   private boolean sodium$active;
   @Nullable
   private SpriteContents.AnimatorImpl iris$createdTicker;
   @Nullable
   private PBRSpriteHolder iris$pbrHolder;

   public SpriteContents(Identifier id, SpriteDimensions dimensions, NativeImage image, ResourceMetadata metadata) {
      this.id = id;
      this.width = dimensions.width();
      this.height = dimensions.height();
      this.metadata = metadata;
      this.animation = metadata.decode(AnimationResourceMetadata.SERIALIZER)
         .map(animationMetadata -> this.createAnimation(dimensions, image.getWidth(), image.getHeight(), animationMetadata))
         .orElse(null);
      this.image = image;
      this.mipmapLevelsImages = new NativeImage[]{this.image};
   }

   public void generateMipmaps(int mipmapLevels) {
      try {
         CustomMipmapGenerator generator = this instanceof CustomMipmapGenerator.Provider provider ? provider.getMipmapGenerator() : null;
         if (generator != null) {
            try {
               this.mipmapLevelsImages = generator.generateMipLevels(this.mipmapLevelsImages, mipmapLevels);
               return;
            } catch (Exception exception) {
               Iris.logger.error("ERROR MIPMAPPING", exception);
            }
         }

         this.mipmapLevelsImages = MipmapHelper.getMipmapLevelsImages(this.mipmapLevelsImages, mipmapLevels);
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.create(throwable, "Generating mipmaps for frame");
         CrashReportSection crashReportSection = crashReport.addElement("Sprite being mipmapped");
         crashReportSection.add("First frame", () -> {
            StringBuilder stringBuilder = new StringBuilder();
            if (stringBuilder.length() > 0) {
               stringBuilder.append(", ");
            }

            stringBuilder.append(this.image.getWidth()).append("x").append(this.image.getHeight());
            return stringBuilder.toString();
         });
         CrashReportSection crashReportSection2 = crashReport.addElement("Frame being iterated");
         crashReportSection2.add("Sprite name", this.id);
         crashReportSection2.add("Sprite size", () -> this.width + " x " + this.height);
         crashReportSection2.add("Sprite frames", () -> this.getFrameCount() + " frames");
         crashReportSection2.add("Mipmap levels", mipmapLevels);
         throw new CrashException(crashReport);
      }
   }

   private int getFrameCount() {
      return this.animation != null ? this.animation.frames.size() : 1;
   }

   @Nullable
   private SpriteContents.Animation createAnimation(SpriteDimensions dimensions, int imageWidth, int imageHeight, AnimationResourceMetadata metadata) {
      int i = imageWidth / dimensions.width();
      int j = imageHeight / dimensions.height();
      int k = i * j;
      int l = metadata.defaultFrameTime();
      List<SpriteContents.AnimationFrame> list;
      if (metadata.frames().isEmpty()) {
         list = new ArrayList<>(k);

         for (int m = 0; m < k; m++) {
            list.add(new SpriteContents.AnimationFrame(m, l));
         }
      } else {
         List<AnimationFrameResourceMetadata> list2 = metadata.frames().get();
         list = new ArrayList<>(list2.size());

         for (AnimationFrameResourceMetadata animationFrameResourceMetadata : list2) {
            list.add(new SpriteContents.AnimationFrame(animationFrameResourceMetadata.index(), animationFrameResourceMetadata.getTime(l)));
         }

         int n = 0;
         IntSet intSet = new IntOpenHashSet();

         for (Iterator<SpriteContents.AnimationFrame> iterator = list.iterator(); iterator.hasNext(); n++) {
            SpriteContents.AnimationFrame animationFrame = iterator.next();
            boolean bl = true;
            if (animationFrame.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", new Object[]{this.id, n, animationFrame.time});
               bl = false;
            }

            if (animationFrame.index < 0 || animationFrame.index >= k) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", new Object[]{this.id, n, animationFrame.index});
               bl = false;
            }

            if (bl) {
               intSet.add(animationFrame.index);
            } else {
               iterator.remove();
            }
         }

         int[] is = IntStream.range(0, k).filter(ix -> !intSet.contains(ix)).toArray();
         if (is.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.id, Arrays.toString(is));
         }
      }

      return list.size() <= 1 ? null : new SpriteContents.Animation(List.copyOf(list), i, metadata.interpolate());
   }

   void upload(int x, int y, int unpackSkipPixels, int unpackSkipRows, NativeImage[] images) {
      for (int i = 0; i < this.mipmapLevelsImages.length; i++) {
         images[i].upload(i, x >> i, y >> i, unpackSkipPixels >> i, unpackSkipRows >> i, this.width >> i, this.height >> i, false);
      }
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public Identifier getId() {
      return this.id;
   }

   public IntStream getDistinctFrameCount() {
      return this.animation != null ? this.animation.getDistinctFrameCount() : IntStream.of(1);
   }

   @Nullable
   public Animator createAnimator() {
      Animator animator = this.animation != null ? this.animation.createAnimator() : null;
      this.iris$createdTicker = animator instanceof SpriteContents.AnimatorImpl ticker ? ticker : null;
      return animator;
   }

   @Nullable
   @Override
   public SpriteContents.AnimatorImpl getCreatedTicker() {
      return this.iris$createdTicker;
   }

   @Nullable
   @Override
   public PBRSpriteHolder getPBRHolder() {
      return this.iris$pbrHolder;
   }

   @Override
   public PBRSpriteHolder getOrCreatePBRHolder() {
      if (this.iris$pbrHolder == null) {
         this.iris$pbrHolder = new PBRSpriteHolder();
      }

      return this.iris$pbrHolder;
   }

   public ResourceMetadata getMetadata() {
      return this.metadata;
   }

   @Override
   public void sodium$setActive(boolean active) {
      this.sodium$active = active;
      if (this.iris$pbrHolder != null) {
         Sprite normalSprite = this.iris$pbrHolder.getNormalSprite();
         Sprite specularSprite = this.iris$pbrHolder.getSpecularSprite();
         if (normalSprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(normalSprite);
         }

         if (specularSprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(specularSprite);
         }
      }
   }

   @Override
   public boolean sodium$isActive() {
      return this.sodium$active;
   }

   @Override
   public boolean sodium$hasAnimation() {
      return this.animation != null;
   }

   @Override
   public void close() {
      for (NativeImage nativeImage : this.mipmapLevelsImages) {
         nativeImage.close();
      }

      if (this.iris$pbrHolder != null) {
         this.iris$pbrHolder.close();
      }
   }

   @Override
   public String toString() {
      return "SpriteContents{name=" + this.id + ", frameCount=" + this.getFrameCount() + ", height=" + this.height + ", width=" + this.width + "}";
   }

   public boolean isPixelTransparent(int frame, int x, int y) {
      int i = x;
      int j = y;
      if (this.animation != null) {
         i += this.animation.getFrameX(frame) * this.width;
         j += this.animation.getFrameY(frame) * this.height;
      }

      return ColorHelper.getAlpha(this.image.getColorArgb(i, j)) == 0;
   }

   public void upload(int x, int y) {
      if (this.animation != null) {
         this.animation.upload(x, y);
      } else {
         this.upload(x, y, 0, 0, this.mipmapLevelsImages);
      }
   }

   public class Animation {
      final List<SpriteContents.AnimationFrame> frames;
      private final int frameCount;
      private final boolean interpolation;

      Animation(final List<SpriteContents.AnimationFrame> frames, final int frameCount, final boolean interpolation) {
         this.frames = frames;
         this.frameCount = frameCount;
         this.interpolation = interpolation;
      }

      int getFrameX(int frame) {
         return frame % this.frameCount;
      }

      int getFrameY(int frame) {
         return frame / this.frameCount;
      }

      void upload(int x, int y, int frame) {
         int i = this.getFrameX(frame) * SpriteContents.this.width;
         int j = this.getFrameY(frame) * SpriteContents.this.height;
         SpriteContents.this.upload(x, y, i, j, SpriteContents.this.mipmapLevelsImages);
      }

      public Animator createAnimator() {
         return SpriteContents.this.new AnimatorImpl(this, this.interpolation ? SpriteContents.this.new Interpolation() : null);
      }

      public void upload(int x, int y) {
         this.upload(x, y, this.frames.get(0).index);
      }

      public IntStream getDistinctFrameCount() {
         return this.frames.stream().mapToInt(frame -> frame.index).distinct();
      }

      public List<SpriteContents.AnimationFrame> getFrames() {
         return this.frames;
      }

      public void invokeUploadFrame(int x, int y, int frame) {
         this.upload(x, y, frame);
      }
   }

   public record AnimationFrame(int index, int time) {
   }

   public class AnimatorImpl implements Animator {
      int frame;
      int currentTime;
      final SpriteContents.Animation animation;
      @Nullable
      private final SpriteContents.Interpolation interpolation;

      AnimatorImpl(final SpriteContents.Animation animation, @Nullable final SpriteContents.Interpolation interpolation) {
         this.animation = animation;
         this.interpolation = interpolation;
      }

      public int getFrame() {
         return this.frame;
      }

      public void setFrame(int frame) {
         this.frame = frame;
      }

      public int getSubFrame() {
         return this.currentTime;
      }

      public void setSubFrame(int subFrame) {
         this.currentTime = subFrame;
      }

      public SpriteContents.Animation getAnimationInfo() {
         return this.animation;
      }

      @Override
      public void tick(int x, int y) {
         if (SodiumClientMod.options().performance.animateOnlyVisibleTextures && !SpriteContents.this.sodium$isActive()) {
            this.currentTime++;
            SpriteContents.AnimationFrame animationFrame = this.animation.frames.get(this.frame);
            if (this.currentTime >= animationFrame.time) {
               this.frame = (this.frame + 1) % this.animation.frames.size();
               this.currentTime = 0;
            }

            return;
         }

         this.currentTime++;
         SpriteContents.AnimationFrame animationFrame = this.animation.frames.get(this.frame);
         if (this.currentTime >= animationFrame.time) {
            int i = animationFrame.index;
            this.frame = (this.frame + 1) % this.animation.frames.size();
            this.currentTime = 0;
            int j = this.animation.frames.get(this.frame).index;
            if (i != j) {
               this.animation.upload(x, y, j);
            }
         } else if (this.interpolation != null) {
            this.interpolation.apply(x, y, this);
         }

         SpriteContents.this.sodium$setActive(false);
      }

      @Override
      public void close() {
         if (this.interpolation != null) {
            this.interpolation.close();
         }
      }
   }

   final class Interpolation implements AutoCloseable {
      private final NativeImage[] images = new NativeImage[SpriteContents.this.mipmapLevelsImages.length];

      Interpolation() {
         for (int i = 0; i < this.images.length; i++) {
            int j = SpriteContents.this.width >> i;
            int k = SpriteContents.this.height >> i;
            this.images[i] = new NativeImage(j, k, false);
         }
      }

      void apply(int x, int y, SpriteContents.AnimatorImpl animator) {
         SpriteContents.Animation animation = animator.animation;
         List<SpriteContents.AnimationFrame> list = animation.frames;
         SpriteContents.AnimationFrame animationFrame = list.get(animator.frame);
         float f = (float)animator.currentTime / animationFrame.time;
         int i = animationFrame.index;
         int j = list.get((animator.frame + 1) % list.size()).index;
         if (i != j) {
            for (int k = 0; k < this.images.length; k++) {
               int l = SpriteContents.this.width >> k;
               int m = SpriteContents.this.height >> k;

               for (int n = 0; n < m; n++) {
                  for (int o = 0; o < l; o++) {
                     int p = this.getPixelColor(animation, i, k, o, n);
                     int q = this.getPixelColor(animation, j, k, o, n);
                     this.images[k].setColorArgb(o, n, ColorHelper.lerp(f, p, q));
                  }
               }
            }

            SpriteContents.this.upload(x, y, 0, 0, this.images);
         }
      }

      private int getPixelColor(SpriteContents.Animation animation, int frameIndex, int layer, int x, int y) {
         return SpriteContents.this.mipmapLevelsImages[layer]
            .getColorArgb(
               x + (animation.getFrameX(frameIndex) * SpriteContents.this.width >> layer),
               y + (animation.getFrameY(frameIndex) * SpriteContents.this.height >> layer)
            );
      }

      @Override
      public void close() {
         for (NativeImage nativeImage : this.images) {
            nativeImage.close();
         }
      }
   }
}
