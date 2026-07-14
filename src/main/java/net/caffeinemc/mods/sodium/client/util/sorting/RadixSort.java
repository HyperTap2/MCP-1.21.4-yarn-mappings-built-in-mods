package net.caffeinemc.mods.sodium.client.util.sorting;

public class RadixSort extends AbstractSort {
   public static final int RADIX_SORT_THRESHOLD = 64;
   private static final int DIGIT_BITS = 8;
   private static final int RADIX_KEY_BITS = 32;
   private static final int BUCKET_COUNT = 256;
   private static final int DIGIT_COUNT = 4;
   private static final int DIGIT_MASK = 255;

   public static int[] sort(int[] keys) {
      return keys.length <= 1 ? new int[keys.length] : radixSort(keys, createHistogram(keys));
   }

   private static int[][] createHistogram(int[] keys) {
      int[][] histogram = new int[4][256];

      for (int key : keys) {
         for (int digit = 0; digit < 4; digit++) {
            histogram[digit][extractDigit(key, digit)]++;
         }
      }

      return histogram;
   }

   private static void prefixSum(int[][] offsets) {
      for (int digit = 0; digit < 4; digit++) {
         int[] buckets = offsets[digit];
         int sum = 0;

         for (int bucket_idx = 0; bucket_idx < 256; bucket_idx++) {
            int offset = sum;
            sum += buckets[bucket_idx];
            buckets[bucket_idx] = offset;
         }
      }
   }

   private static int[] radixSort(int[] keys, int[][] offsets) {
      prefixSum(offsets);
      int length = keys.length;
      int[] cur = createIndexBuffer(length);
      int[] next = new int[length];

      for (int digit = 0; digit < 4; digit++) {
         int[] buckets = offsets[digit];

         for (int pos = 0; pos < length; pos++) {
            int index = cur[pos];
            int bucket_idx = extractDigit(keys[index], digit);
            next[buckets[bucket_idx]] = index;
            buckets[bucket_idx]++;
         }

         int[] temp = next;
         next = cur;
         cur = temp;
      }

      return cur;
   }

   private static int extractDigit(int key, int digit) {
      return key >>> digit * 8 & 0xFF;
   }

   public static boolean useRadixSort(int length) {
      return length >= 64;
   }
}
