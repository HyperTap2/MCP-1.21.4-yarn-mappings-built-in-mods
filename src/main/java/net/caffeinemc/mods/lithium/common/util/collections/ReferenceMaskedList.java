package net.caffeinemc.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.AbstractList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class ReferenceMaskedList<E> extends AbstractList<E> {
   private final ReferenceArrayList<E> allElements = new ReferenceArrayList<>();
   private final BitSet visibleMask = new BitSet();
   private final Reference2IntOpenHashMap<E> elementToIndex = new Reference2IntOpenHashMap<>();
   private final boolean defaultVisibility;
   private int removedCount;

   public ReferenceMaskedList() {
      this(true);
   }

   public ReferenceMaskedList(boolean defaultVisibility) {
      this.defaultVisibility = defaultVisibility;
      this.elementToIndex.defaultReturnValue(-1);
   }

   public int totalSize() {
      return this.allElements.size() - this.removedCount;
   }

   public void setVisible(E element, boolean visible) {
      int index = this.elementToIndex.getInt(element);
      if (index != -1) {
         this.visibleMask.set(index, visible);
      }
   }

   @Override
   public Iterator<E> iterator() {
      return new Iterator<>() {
         private int nextIndex;
         private int cachedNext = -1;

         @Override
         public boolean hasNext() {
            this.cachedNext = ReferenceMaskedList.this.visibleMask.nextSetBit(this.nextIndex);
            return this.cachedNext != -1;
         }

         @Override
         public E next() {
            int index = this.cachedNext;
            this.cachedNext = -1;
            this.nextIndex = index + 1;
            return ReferenceMaskedList.this.allElements.get(index);
         }
      };
   }

   @Override
   public Spliterator<E> spliterator() {
      return new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.DISTINCT) {
         private int nextIndex;

         @Override
         public boolean tryAdvance(Consumer<? super E> action) {
            int index = ReferenceMaskedList.this.visibleMask.nextSetBit(this.nextIndex);
            if (index == -1) {
               return false;
            }
            this.nextIndex = index + 1;
            action.accept(ReferenceMaskedList.this.allElements.get(index));
            return true;
         }
      };
   }

   @Override
   public boolean add(E element) {
      int oldIndex = this.elementToIndex.put(element, this.allElements.size());
      if (oldIndex != -1) {
         throw new IllegalStateException("ReferenceMaskedList must not contain duplicate references");
      }
      this.visibleMask.set(this.allElements.size(), this.defaultVisibility);
      return this.allElements.add(element);
   }

   @Override
   public boolean remove(Object element) {
      int index = this.elementToIndex.removeInt(element);
      if (index == -1) {
         return false;
      }
      this.visibleMask.clear(index);
      this.allElements.set(index, null);
      this.removedCount++;
      if (this.removedCount * 2 > this.allElements.size()) {
         this.compact();
      }
      return true;
   }

   private void compact() {
      ReferenceArrayList<E> oldElements = this.allElements.clone();
      BitSet oldMask = (BitSet)this.visibleMask.clone();
      this.allElements.clear();
      this.visibleMask.clear();
      this.elementToIndex.clear();
      for (int i = 0; i < oldElements.size(); i++) {
         E element = oldElements.get(i);
         if (element != null) {
            int newIndex = this.allElements.size();
            this.allElements.add(element);
            this.visibleMask.set(newIndex, oldMask.get(i));
            this.elementToIndex.put(element, newIndex);
         }
      }
      this.removedCount = 0;
   }

   @Override
   public E get(int index) {
      if (index < 0 || index >= this.size()) {
         throw new IndexOutOfBoundsException(index);
      }
      int visibleIndex = this.visibleMask.nextSetBit(0);
      while (index-- > 0) {
         visibleIndex = this.visibleMask.nextSetBit(visibleIndex + 1);
      }
      return this.allElements.get(visibleIndex);
   }

   @Override
   public int size() {
      return this.visibleMask.cardinality();
   }
}
