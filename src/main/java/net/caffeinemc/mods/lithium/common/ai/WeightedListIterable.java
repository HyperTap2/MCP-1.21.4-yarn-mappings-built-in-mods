package net.caffeinemc.mods.lithium.common.ai;

import java.util.Iterator;
import net.minecraft.util.collection.WeightedList;

public interface WeightedListIterable<U> extends Iterable<U> {
   static <T> Iterable<? extends T> cast(WeightedList<T> list) {
      return (WeightedListIterable<T>)list;
   }

   final class ListIterator<U> implements Iterator<U> {
      private final Iterator<? extends WeightedList.Entry<? extends U>> inner;

      public ListIterator(Iterator<? extends WeightedList.Entry<? extends U>> inner) {
         this.inner = inner;
      }

      @Override
      public boolean hasNext() {
         return this.inner.hasNext();
      }

      @Override
      public U next() {
         return this.inner.next().getElement();
      }
   }
}
