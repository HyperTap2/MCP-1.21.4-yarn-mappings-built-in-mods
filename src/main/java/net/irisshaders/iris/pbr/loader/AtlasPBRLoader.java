package net.irisshaders.iris.pbr.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.format.TextureFormatLoader;
import net.irisshaders.iris.pbr.mipmap.ChannelMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.LinearBlendFunction;
import net.irisshaders.iris.pbr.texture.PBRAtlasTexture;
import net.irisshaders.iris.pbr.texture.PBRSpriteHolder;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.pbr.util.ImageManipulationUtil;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.client.texture.SpriteDimensions;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.metadata.ResourceMetadata;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class AtlasPBRLoader implements PBRTextureLoader<SpriteAtlasTexture> {
   public static final ChannelMipmapGenerator LINEAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(
      LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE
   );

   public void load(SpriteAtlasTexture atlas, ResourceManager resourceManager, PBRTextureLoader.PBRTextureConsumer pbrTextureConsumer) {
      int atlasWidth = atlas.getWidth();
      int atlasHeight = atlas.getHeight();
      int mipLevel = atlas.getMipLevel();
      PBRAtlasTexture normalAtlas = null;
      PBRAtlasTexture specularAtlas = null;

      for (Sprite sprite : atlas.getTexturesByName().values()) {
         AtlasPBRLoader.PBRTextureAtlasSprite normalSprite = this.createPBRSprite(
            sprite, resourceManager, atlas, atlasWidth, atlasHeight, mipLevel, PBRType.NORMAL
         );
         AtlasPBRLoader.PBRTextureAtlasSprite specularSprite = this.createPBRSprite(
            sprite, resourceManager, atlas, atlasWidth, atlasHeight, mipLevel, PBRType.SPECULAR
         );
         if (normalSprite != null) {
            if (normalAtlas == null) {
               normalAtlas = new PBRAtlasTexture(atlas, PBRType.NORMAL);
            }

            normalAtlas.addSprite(normalSprite);
            PBRSpriteHolder pbrSpriteHolder = sprite.getContents().getOrCreatePBRHolder();
            pbrSpriteHolder.setNormalSprite(normalSprite);
         }

         if (specularSprite != null) {
            if (specularAtlas == null) {
               specularAtlas = new PBRAtlasTexture(atlas, PBRType.SPECULAR);
            }

            specularAtlas.addSprite(specularSprite);
            PBRSpriteHolder pbrSpriteHolder = sprite.getContents().getOrCreatePBRHolder();
            pbrSpriteHolder.setSpecularSprite(specularSprite);
         }
      }

      if (normalAtlas != null && normalAtlas.tryUpload(atlasWidth, atlasHeight, mipLevel)) {
         pbrTextureConsumer.acceptNormalTexture(normalAtlas);
      }

      if (specularAtlas != null && specularAtlas.tryUpload(atlasWidth, atlasHeight, mipLevel)) {
         pbrTextureConsumer.acceptSpecularTexture(specularAtlas);
      }
   }

   @Nullable
   protected AtlasPBRLoader.PBRTextureAtlasSprite createPBRSprite(
      Sprite sprite, ResourceManager resourceManager, SpriteAtlasTexture atlas, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType
   ) {
      Identifier spriteName = sprite.getContents().getId();
      Identifier pbrImageLocation = this.getPBRImageLocation(spriteName, pbrType);
      Optional<Resource> optionalResource = resourceManager.getResource(pbrImageLocation);
      if (optionalResource.isEmpty()) {
         return null;
      }

      Resource resource = optionalResource.get();

      ResourceMetadata animationMetadata;
      try {
         animationMetadata = resource.getMetadata();
      } catch (Exception e) {
         Iris.logger.error("Unable to parse metadata from {}", pbrImageLocation, e);
         return null;
      }

      NativeImage nativeImage;
      try (InputStream stream = resource.getInputStream()) {
         nativeImage = NativeImage.read(stream);
      } catch (IOException e) {
         Iris.logger.error("Using missing texture, unable to load {}", pbrImageLocation, e);
         return null;
      }

      int imageWidth = nativeImage.getWidth();
      int imageHeight = nativeImage.getHeight();
      AnimationResourceMetadata metadataSection = (AnimationResourceMetadata)animationMetadata.decode(AnimationResourceMetadata.SERIALIZER).orElse(null);
      SpriteDimensions frameSize = metadataSection != null ? metadataSection.getSize(imageWidth, imageHeight) : new SpriteDimensions(imageWidth, imageHeight);
      int frameWidth = frameSize.width();
      int frameHeight = frameSize.height();
      if (MathHelper.isMultipleOf(imageWidth, frameWidth) && MathHelper.isMultipleOf(imageHeight, frameHeight)) {
         int targetFrameWidth = sprite.getContents().getWidth();
         int targetFrameHeight = sprite.getContents().getHeight();
         if (frameWidth != targetFrameWidth || frameHeight != targetFrameHeight) {
            try {
               int targetImageWidth = imageWidth / frameWidth * targetFrameWidth;
               int targetImageHeight = imageHeight / frameHeight * targetFrameHeight;
               NativeImage scaledImage;
               if (targetImageWidth % imageWidth == 0 && targetImageHeight % imageHeight == 0) {
                  scaledImage = ImageManipulationUtil.scaleNearestNeighbor(nativeImage, targetImageWidth, targetImageHeight);
               } else {
                  scaledImage = ImageManipulationUtil.scaleBilinear(nativeImage, targetImageWidth, targetImageHeight);
               }

               nativeImage.close();
               nativeImage = scaledImage;
               frameWidth = targetFrameWidth;
               frameHeight = targetFrameHeight;
               if (metadataSection != null) {
                  Optional<Integer> adjustedWidth = metadataSection.width().isPresent() ? Optional.of(frameWidth) : Optional.empty();
                  Optional<Integer> adjustedHeight = metadataSection.height().isPresent() ? Optional.of(frameHeight) : Optional.empty();
                  metadataSection = new AnimationResourceMetadata(
                     metadataSection.frames(), adjustedWidth, adjustedHeight, metadataSection.defaultFrameTime(), metadataSection.interpolate()
                  );
                  animationMetadata = new ResourceMetadata.Builder().add(AnimationResourceMetadata.SERIALIZER, metadataSection).build();
               }
            } catch (Exception e) {
               Iris.logger.error("Something bad happened trying to load PBR texture " + spriteName.getPath() + pbrType.getSuffix() + "!", e);
               throw e;
            }
         }

         Identifier pbrSpriteName = Identifier.of(spriteName.getNamespace(), spriteName.getPath() + pbrType.getSuffix());
         AtlasPBRLoader.PBRSpriteContents pbrSpriteContents = new AtlasPBRLoader.PBRSpriteContents(
            pbrSpriteName, new SpriteDimensions(frameWidth, frameHeight), nativeImage, animationMetadata, pbrType
         );
         pbrSpriteContents.generateMipmaps(mipLevel);
         return new AtlasPBRLoader.PBRTextureAtlasSprite(pbrSpriteName, pbrSpriteContents, atlasWidth, atlasHeight, sprite.getX(), sprite.getY(), sprite);
      } else {
         Iris.logger.error("Image {} size {},{} is not multiple of frame size {},{}", pbrImageLocation, imageWidth, imageHeight, frameWidth, frameHeight);
         nativeImage.close();
         return null;
      }
   }

   protected Identifier getPBRImageLocation(Identifier spriteName, PBRType pbrType) {
      String path = pbrType.appendSuffix(spriteName.getPath());
      return path.startsWith("optifine/cit/")
         ? Identifier.of(spriteName.getNamespace(), path + ".png")
         : Identifier.of(spriteName.getNamespace(), "textures/" + path + ".png");
   }

   protected static class PBRSpriteContents extends SpriteContents implements CustomMipmapGenerator.Provider {
      protected final PBRType pbrType;

      public PBRSpriteContents(Identifier name, SpriteDimensions size, NativeImage image, ResourceMetadata metadata, PBRType pbrType) {
         super(name, size, image, metadata);
         this.pbrType = pbrType;
      }

      @Override
      public CustomMipmapGenerator getMipmapGenerator() {
         TextureFormat format = TextureFormatLoader.getFormat();
         if (format != null) {
            CustomMipmapGenerator generator = format.getMipmapGenerator(this.pbrType);
            if (generator != null) {
               return generator;
            }
         }

         return AtlasPBRLoader.LINEAR_MIPMAP_GENERATOR;
      }
   }

   public static class PBRTextureAtlasSprite extends Sprite {
      protected final Sprite baseSprite;

      protected PBRTextureAtlasSprite(
         Identifier location, AtlasPBRLoader.PBRSpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, Sprite baseSprite
      ) {
         super(location, contents, atlasWidth, atlasHeight, x, y);
         this.baseSprite = baseSprite;
      }

      public Sprite getBaseSprite() {
         return this.baseSprite;
      }
   }
}
