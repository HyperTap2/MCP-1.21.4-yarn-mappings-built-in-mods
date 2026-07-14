package net.caffeinemc.mods.sodium.client.util;

public class MathUtil {
   public static boolean isPowerOfTwo(int n) {
      return (n & n - 1) == 0;
   }

   public static long toMib(long bytes) {
      return bytes / 1048576L;
   }

   public static int align(int num, int alignment) {
      int additive = alignment - 1;
      int mask = ~additive;
      return num + additive & mask;
   }

   public static int floatToComparableInt(float f) {
      int bits = Float.floatToRawIntBits(f);
      return bits ^ bits >> 31 & 2147483647;
   }

   public static float comparableIntToFloat(int i) {
      return Float.intBitsToFloat(i ^ i >> 31 & 2147483647);
   }
}
