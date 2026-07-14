package net.caffeinemc.mods.sodium.client.render.frapi.helper;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;

public abstract class ColorHelper {
   public static int maxBrightness(int b0, int b1) {
      return Math.max(b0 & 65535, b1 & 65535) | Math.max(b0 & -65536, b1 & -65536);
   }

   public static int toVanillaColor(int color) {
      return ColorABGR.toNativeByteOrder(ColorARGB.toABGR(color));
   }

   public static int fromVanillaColor(int color) {
      return ColorARGB.fromABGR(ColorABGR.fromNativeByteOrder(color));
   }
}
