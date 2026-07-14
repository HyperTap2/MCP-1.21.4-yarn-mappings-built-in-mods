package net.caffeinemc.mods.sodium.client.util.color;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.util.math.MathHelper;

public class BoxBlur {
   public static void blur(int[] src, int[] tmp, int width, int height, int radius) {
      if (!isHomogenous(src)) {
         blurImpl(src, tmp, radius, width - radius, width, 0, height, height, radius);
         blurImpl(tmp, src, radius, width - radius, width, radius, height - radius, height, radius);
      }
   }

   private static void blurImpl(int[] src, int[] dst, int x0, int x1, int width, int y0, int y1, int height, int radius) {
      int windowSize = radius * 2 + 1;
      int multiplier = getAveragingMultiplier(windowSize);

      for (int y = y0; y < y1; y++) {
         int accR = 0;
         int accG = 0;
         int accB = 0;
         int windowPivotIndex = BoxBlur.ColorBuffer.getIndex(x0, y, width);
         int windowTailIndex = windowPivotIndex - radius;
         int windowHeadIndex = windowPivotIndex + radius;

         for (int x = -radius; x <= radius; x++) {
            int color = src[windowPivotIndex + x];
            accR += ColorARGB.unpackRed(color);
            accG += ColorARGB.unpackGreen(color);
            accB += ColorARGB.unpackBlue(color);
         }

         int x = x0;

         while (true) {
            dst[BoxBlur.ColorBuffer.getIndex(y, x, width)] = averageRGB(accR, accG, accB, multiplier);
            if (++x >= x1) {
               break;
            }

            int color = src[windowTailIndex++];
            accR -= ColorARGB.unpackRed(color);
            accG -= ColorARGB.unpackGreen(color);
            accB -= ColorARGB.unpackBlue(color);
            color = src[++windowHeadIndex];
            accR += ColorARGB.unpackRed(color);
            accG += ColorARGB.unpackGreen(color);
            accB += ColorARGB.unpackBlue(color);
         }
      }
   }

   private static int getAveragingMultiplier(int size) {
      return MathHelper.ceil(1.6777216E7 / size);
   }

   public static int averageRGB(int red, int green, int blue, int multiplier) {
      int value = -16777216;
      value |= blue * multiplier >>> 24 << 0;
      value |= green * multiplier >>> 24 << 8;
      return value | red * multiplier >>> 24 << 16;
   }

   private static boolean isHomogenous(int[] array) {
      int first = array[0];

      for (int i = 1; i < array.length; i++) {
         if (array[i] != first) {
            return false;
         }
      }

      return true;
   }

   public static class ColorBuffer {
      public final int[] data;
      protected final int width;
      protected final int height;

      public ColorBuffer(int width, int height) {
         this.data = new int[width * height];
         this.width = width;
         this.height = height;
      }

      public void set(int x, int y, int color) {
         this.data[getIndex(x, y, this.width)] = color;
      }

      public int get(int x, int y) {
         return this.data[getIndex(x, y, this.width)];
      }

      public static int getIndex(int x, int y, int width) {
         return x + y * width;
      }
   }
}
