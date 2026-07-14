package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface PairList {
   DoubleList getPairs();

   boolean forEachPair(PairList.Consumer predicate);

   int size();

   interface Consumer {
      boolean merge(int x, int y, int index);
   }
}
