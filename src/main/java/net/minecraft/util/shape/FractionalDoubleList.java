package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class FractionalDoubleList extends AbstractDoubleList {
   private final int sectionCount;
   private final double scale;

   public FractionalDoubleList(int sectionCount) {
      if (sectionCount <= 0) {
         throw new IllegalArgumentException("Need at least 1 part");
      }

      this.sectionCount = sectionCount;
      this.scale = 1.0 / sectionCount;
   }

   public double getDouble(int position) {
      return position * this.scale;
   }

   public int size() {
      return this.sectionCount + 1;
   }
}
