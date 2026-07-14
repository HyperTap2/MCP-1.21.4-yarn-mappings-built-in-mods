package net.irisshaders.iris.targets.backed;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;

public class NativeImageBackedSingleColorTexture extends NativeImageBackedTexture {
   public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
      super(create(ColorARGB.pack(red, green, blue, alpha)));
   }

   public NativeImageBackedSingleColorTexture(int rgba) {
      this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
   }

   private static NativeImage create(int color) {
      NativeImage image = new NativeImage(Format.RGBA, 1, 1, false);
      image.setColorArgb(0, 0, color);
      return image;
   }
}
