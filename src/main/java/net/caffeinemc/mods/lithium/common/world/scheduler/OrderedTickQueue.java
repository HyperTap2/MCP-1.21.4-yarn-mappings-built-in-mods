package net.caffeinemc.mods.lithium.common.world.scheduler;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import net.minecraft.world.tick.OrderedTick;

public class OrderedTickQueue<T> extends AbstractQueue<OrderedTick<T>> {
   private static final int INITIAL_CAPACITY = 16;
   private static final Comparator<OrderedTick<?>> COMPARATOR = Comparator.comparingLong(OrderedTick::subTickOrder);
   private OrderedTick<T>[] arr;
   private int lastIndexExclusive;
   private int firstIndex;
   private long currentMaxSubTickOrder = Long.MIN_VALUE;
   private boolean isSorted;
   private OrderedTick<T> unsortedPeekResult;

   public OrderedTickQueue(int capacity) {
      this.arr = new OrderedTick[capacity];
      this.lastIndexExclusive = 0;
      this.isSorted = true;
      this.unsortedPeekResult = null;
      this.firstIndex = 0;
   }

   public OrderedTickQueue() {
      this(16);
   }

   @Override
   public void clear() {
      Arrays.fill(this.arr, null);
      this.lastIndexExclusive = 0;
      this.firstIndex = 0;
      this.currentMaxSubTickOrder = Long.MIN_VALUE;
      this.isSorted = true;
      this.unsortedPeekResult = null;
   }

   @Override
   public Iterator<OrderedTick<T>> iterator() {
      if (this.isEmpty()) {
         return Collections.emptyIterator();
      }

      this.sort();
      return new Iterator<OrderedTick<T>>() {
         int nextIndex = OrderedTickQueue.this.firstIndex;

         @Override
         public boolean hasNext() {
            return this.nextIndex < OrderedTickQueue.this.lastIndexExclusive;
         }

         public OrderedTick<T> next() {
            return OrderedTickQueue.this.arr[this.nextIndex++];
         }
      };
   }

   public OrderedTick<T> poll() {
      if (this.isEmpty()) {
         return null;
      }

      if (!this.isSorted) {
         this.sort();
      }

      int polledIndex = this.firstIndex++;
      OrderedTick<T>[] ticks = this.arr;
      OrderedTick<T> nextTick = ticks[polledIndex];
      ticks[polledIndex] = null;
      return nextTick;
   }

   public OrderedTick<T> peek() {
      if (!this.isSorted) {
         return this.unsortedPeekResult;
      } else {
         return this.lastIndexExclusive > this.firstIndex ? this.getTickAtIndex(this.firstIndex) : null;
      }
   }

   public boolean offer(OrderedTick<T> tick) {
      if (this.lastIndexExclusive >= this.arr.length) {
         this.arr = copyArray(this.arr, HashCommon.nextPowerOfTwo(this.arr.length + 1));
      }

      if (tick.subTickOrder() <= this.currentMaxSubTickOrder) {
         OrderedTick<T> firstTick = this.isSorted ? (this.size() > 0 ? this.arr[this.firstIndex] : null) : this.unsortedPeekResult;
         this.isSorted = false;
         this.unsortedPeekResult = firstTick != null && tick.subTickOrder() >= firstTick.subTickOrder() ? firstTick : tick;
      } else {
         this.currentMaxSubTickOrder = tick.subTickOrder();
      }

      this.arr[this.lastIndexExclusive++] = tick;
      return true;
   }

   @Override
   public int size() {
      return this.lastIndexExclusive - this.firstIndex;
   }

   private void handleCompaction(int size) {
      if (this.arr.length > 16 && size < this.arr.length / 2) {
         this.arr = copyArray(this.arr, size);
      } else {
         Arrays.fill(this.arr, size, this.arr.length, null);
      }

      this.firstIndex = 0;
      this.lastIndexExclusive = size;
      if (size != 0 && this.isSorted) {
         OrderedTick<T> tick = this.arr[size - 1];
         this.currentMaxSubTickOrder = tick == null ? Long.MIN_VALUE : tick.subTickOrder();
      } else {
         this.currentMaxSubTickOrder = Long.MIN_VALUE;
      }
   }

   public void sort() {
      if (!this.isSorted) {
         this.removeNullsAndConsumed();
         Arrays.sort(this.arr, this.firstIndex, this.lastIndexExclusive, COMPARATOR);
         this.isSorted = true;
         this.unsortedPeekResult = null;
      }
   }

   public void removeNullsAndConsumed() {
      int src = this.firstIndex;
      int dst = 0;

      while (src < this.lastIndexExclusive) {
         OrderedTick<T> orderedTick = this.arr[src];
         if (orderedTick != null) {
            this.arr[dst] = orderedTick;
            dst++;
         }

         src++;
      }

      this.handleCompaction(dst);
   }

   public OrderedTick<T> getTickAtIndex(int index) {
      if (!this.isSorted) {
         throw new IllegalStateException("Unexpected access on unsorted queue!");
      } else {
         return this.arr[index];
      }
   }

   public void setTickAtIndex(int index, OrderedTick<T> tick) {
      if (!this.isSorted) {
         throw new IllegalStateException("Unexpected access on unsorted queue!");
      }

      this.arr[index] = tick;
   }

   private static <T> OrderedTick<T>[] copyArray(OrderedTick<T>[] src, int size) {
      OrderedTick<T>[] copy = new OrderedTick[Math.max(16, size)];
      if (size != 0) {
         System.arraycopy(src, 0, copy, 0, Math.min(src.length, size));
      }

      return copy;
   }

   @Override
   public boolean isEmpty() {
      return this.lastIndexExclusive <= this.firstIndex;
   }
}

