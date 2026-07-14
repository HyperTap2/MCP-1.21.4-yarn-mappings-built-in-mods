package net.caffeinemc.mods.lithium.common.shapes.lists;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

public class OffsetFractionalDoubleList extends AbstractDoubleList {
   private final int sectionCount;
   private final double offset;
   private final double scale;

   public OffsetFractionalDoubleList(int sectionCount, double offset) {
      this.sectionCount = sectionCount;
      this.offset = offset;
      this.scale = 1.0 / sectionCount;
   }

   @Override
   public double getDouble(int position) {
      return this.offset + position * this.scale;
   }

   @Override
   public int size() {
      return this.sectionCount + 1;
   }
}
