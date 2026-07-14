package net.caffeinemc.mods.sodium.client.util;

public class UInt32 {
   public static long upcast(int x) {
      return Integer.toUnsignedLong(x);
   }

   public static int downcast(long x) {
      if (x < 0L) {
         throw new IllegalArgumentException("x < 0");
      } else if (x >= 4294967296L) {
         throw new IllegalArgumentException("x >= (1 << 32)");
      } else {
         return (int)x;
      }
   }

   public static int uncheckedDowncast(long x) {
      return (int)x;
   }
}
