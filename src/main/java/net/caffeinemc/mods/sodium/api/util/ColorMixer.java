package net.caffeinemc.mods.sodium.api.util;

import org.jetbrains.annotations.ApiStatus.Experimental;

public class ColorMixer {
   public static int mix(int start, int end, int weight) {
      long hi = (start & 16711935L) * weight + (end & 16711935L) * (255 - weight);
      long lo = (start & 4278255360L) * weight + (end & 4278255360L) * (255 - weight);
      long result = hi + 16711935L >>> 8 & 16711935L | lo + 4278255360L >>> 8 & 4278255360L;
      return (int)result;
   }

   public static int mix(int start, int end, float weight) {
      return mix(start, end, ColorU8.normalizedFloatToByte(weight));
   }

   @Experimental
   public static int mix2d(int m00, int m01, int m10, int m11, float x, float y) {
      int x1 = ColorU8.normalizedFloatToByte(x);
      int x0 = 255 - x1;
      int y1 = ColorU8.normalizedFloatToByte(y);
      int y0 = 255 - y1;
      long row0a = (m00 & 16711935L) * x0 + (m10 & 16711935L) * x1 + 16711935L >>> 8 & 16711935L;
      long row0b = (m00 & 4278255360L) * x0 + (m10 & 4278255360L) * x1 + 4278255360L >>> 8 & 4278255360L;
      long row1a = (m01 & 16711935L) * x0 + (m11 & 16711935L) * x1 + 16711935L >>> 8 & 16711935L;
      long row1b = (m01 & 4278255360L) * x0 + (m11 & 4278255360L) * x1 + 4278255360L >>> 8 & 4278255360L;
      long result = row0a * y0 + row1a * y1 + 16711935L >>> 8 & 16711935L | row0b * y0 + row1b * y1 + 4278255360L >>> 8 & 4278255360L;
      return (int)result;
   }

   public static int mulComponentWise(int color0, int color1) {
      int comp0 = (color0 >>> 0 & 0xFF) * (color1 >>> 0 & 0xFF) + 255 >>> 8;
      int comp1 = (color0 >>> 8 & 0xFF) * (color1 >>> 8 & 0xFF) + 255 >>> 8;
      int comp2 = (color0 >>> 16 & 0xFF) * (color1 >>> 16 & 0xFF) + 255 >>> 8;
      int comp3 = (color0 >>> 24 & 0xFF) * (color1 >>> 24 & 0xFF) + 255 >>> 8;
      return comp0 << 0 | comp1 << 8 | comp2 << 16 | comp3 << 24;
   }

   public static int mul(int color, int factor) {
      long hi = (color & 16711935L) * factor;
      long lo = (color & 4278255360L) * factor;
      long result = hi + 16711935L >>> 8 & 16711935L | lo + 4278255360L >>> 8 & 4278255360L;
      return (int)result;
   }

   public static int mul(int color, float factor) {
      return mul(color, ColorU8.normalizedFloatToByte(factor));
   }
}
