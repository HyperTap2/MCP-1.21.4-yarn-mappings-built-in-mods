package net.caffeinemc.mods.lithium.common.shapes.pairs;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.shape.PairList;

public final class LithiumDoublePairList implements PairList {
   private final double[] merged;
   private final int[] firstIndices;
   private final int[] secondIndices;
   private final DoubleArrayList pairs;

   public LithiumDoublePairList(DoubleList first, DoubleList second, boolean includeFirst, boolean includeSecond) {
      int size = first.size() + second.size();
      this.merged = new double[size];
      this.firstIndices = new int[size];
      this.secondIndices = new int[size];
      this.pairs = DoubleArrayList.wrap(this.merged);
      this.merge(getArray(first), getArray(second), first.size(), second.size(), includeFirst, includeSecond);
   }

   private void merge(double[] first, double[] second, int firstSize, int secondSize, boolean includeFirst, boolean includeSecond) {
      int firstIndex = 0;
      int secondIndex = 0;
      int indexCount = 0;
      int mergedCount = 0;
      double previous = 0.0;

      while (firstIndex < firstSize || secondIndex < secondSize) {
         boolean firstInBounds = firstIndex < firstSize;
         boolean secondInBounds = secondIndex < secondSize;
         boolean takeFirst = firstInBounds && (!secondInBounds || first[firstIndex] < second[secondIndex] + 1.0E-7);
         double value = takeFirst ? first[firstIndex++] : second[secondIndex++];
         if ((firstIndex != 0 && firstInBounds || takeFirst || includeSecond)
            && (secondIndex != 0 && secondInBounds || !takeFirst || includeFirst)) {
            if (mergedCount == 0 || previous < value - 1.0E-7) {
               this.firstIndices[indexCount] = firstIndex - 1;
               this.secondIndices[indexCount] = secondIndex - 1;
               this.merged[mergedCount++] = value;
               indexCount++;
               previous = value;
            } else {
               this.firstIndices[indexCount - 1] = firstIndex - 1;
               this.secondIndices[indexCount - 1] = secondIndex - 1;
            }
         }
      }

      if (mergedCount == 0) {
         this.merged[mergedCount++] = Math.min(first[firstSize - 1], second[secondSize - 1]);
      }
      this.pairs.size(mergedCount);
   }

   @Override
   public boolean forEachPair(PairList.Consumer consumer) {
      for (int i = 0; i < this.pairs.size() - 1; i++) {
         if (!consumer.merge(this.firstIndices[i], this.secondIndices[i], i)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public int size() {
      return this.pairs.size();
   }

   @Override
   public DoubleList getPairs() {
      return this.pairs;
   }

   private static double[] getArray(DoubleList list) {
      if (list instanceof DoubleArrayList arrayList) {
         return arrayList.elements();
      }
      double[] values = new double[list.size()];
      for (int i = 0; i < values.length; i++) {
         values[i] = list.getDouble(i);
      }
      return values;
   }
}
