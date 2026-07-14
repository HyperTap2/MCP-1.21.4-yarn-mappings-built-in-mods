package net.caffeinemc.mods.sodium.client.util.collections;

import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.util.MathUtil;

public class BitArray {
   private static final int ADDRESS_BITS_PER_WORD = 6;
   private static final int BITS_PER_WORD = 64;
   private static final int BIT_INDEX_MASK = 63;
   private static final long WORD_MASK = -1L;
   private final long[] words;
   private final int capacity;

   public BitArray(int capacity) {
      this.words = new long[MathUtil.align(capacity, 64) >> 6];
      this.capacity = capacity;
   }

   public boolean get(int index) {
      return (this.words[wordIndex(index)] & 1L << bitIndex(index)) != 0L;
   }

   public void set(int index) {
      this.words[wordIndex(index)] |= 1L << bitIndex(index);
   }

   public void unset(int index) {
      this.words[wordIndex(index)] &= ~(1L << bitIndex(index));
   }

   public void put(int index, boolean value) {
      int wordIndex = wordIndex(index);
      int bitIndex = bitIndex(index);
      long intValue = value ? 1L : 0L;
      this.words[wordIndex] = this.words[wordIndex] & ~(1L << bitIndex) | intValue << bitIndex;
   }

   public void set(int startIdx, int endIdx) {
      int startWordIndex = wordIndex(startIdx);
      int endWordIndex = wordIndex(endIdx - 1);
      long firstWordMask = -1L << startIdx;
      long lastWordMask = -1L >>> -endIdx;
      if (startWordIndex == endWordIndex) {
         this.words[startWordIndex] = this.words[startWordIndex] | firstWordMask & lastWordMask;
      } else {
         this.words[startWordIndex] = this.words[startWordIndex] | firstWordMask;

         for (int i = startWordIndex + 1; i < endWordIndex; i++) {
            this.words[i] = -1L;
         }

         this.words[endWordIndex] = this.words[endWordIndex] | lastWordMask;
      }
   }

   public void unset(int startIdx, int endIdx) {
      int startWordIndex = wordIndex(startIdx);
      int endWordIndex = wordIndex(endIdx - 1);
      long firstWordMask = ~(-1L << startIdx);
      long lastWordMask = ~(-1L >>> -endIdx);
      if (startWordIndex == endWordIndex) {
         this.words[startWordIndex] = this.words[startWordIndex] & firstWordMask & lastWordMask;
      } else {
         this.words[startWordIndex] = this.words[startWordIndex] & firstWordMask;

         for (int i = startWordIndex + 1; i < endWordIndex; i++) {
            this.words[i] = 0L;
         }

         this.words[endWordIndex] = this.words[endWordIndex] & lastWordMask;
      }
   }

   public void fill(boolean value) {
      Arrays.fill(this.words, value ? -1L : 0L);
   }

   public void unsetAll() {
      this.fill(false);
   }

   public void setAll() {
      this.fill(true);
   }

   public int countSetBits() {
      int sum = 0;

      for (long word : this.words) {
         sum += Long.bitCount(word);
      }

      return sum;
   }

   public int capacity() {
      return this.capacity;
   }

   public boolean getAndSet(int index) {
      int wordIndex = wordIndex(index);
      long bit = 1L << bitIndex(index);
      long word = this.words[wordIndex];
      this.words[wordIndex] = word | bit;
      return (word & bit) != 0L;
   }

   public boolean getAndUnset(int index) {
      int wordIndex = wordIndex(index);
      long bit = 1L << bitIndex(index);
      long word = this.words[wordIndex];
      this.words[wordIndex] = word & ~bit;
      return (word & bit) != 0L;
   }

   public int nextSetBit(int fromIndex) {
      int u = wordIndex(fromIndex);
      if (u >= this.words.length) {
         return -1;
      } else {
         long word;
         for (word = this.words[u] & -1L << fromIndex; word == 0L; word = this.words[u]) {
            if (++u == this.words.length) {
               return -1;
            }
         }

         return u * 64 + Long.numberOfTrailingZeros(word);
      }
   }

   private static int wordIndex(int index) {
      return index >> 6;
   }

   private static int bitIndex(int index) {
      return index & 63;
   }
}
