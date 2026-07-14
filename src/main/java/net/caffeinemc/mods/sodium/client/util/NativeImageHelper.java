package net.caffeinemc.mods.sodium.client.util;

import java.util.Locale;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImage.Format;

public class NativeImageHelper {
   public static long getPointerRGBA(NativeImage nativeImage) {
      if (nativeImage.getFormat() != Format.RGBA) {
         throw new IllegalArgumentException(
            String.format(Locale.ROOT, "Tried to get pointer to RGBA pixel data on NativeImage of wrong format; have %s", nativeImage.getFormat())
         );
      } else {
         return nativeImage.pointer;
      }
   }
}
