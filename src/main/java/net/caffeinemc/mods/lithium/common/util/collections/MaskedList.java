package net.caffeinemc.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.AbstractList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class MaskedList<E> extends AbstractList<E> {
   private final ObjectArrayList<E> allElements = new ObjectArrayList<>();
   private final BitSet visibleMask = new BitSet();
   private final Object2IntOpenHashMap<E> elementToIndex = new Object2IntOpenHashMap<>();
   private final boolean defaultVisibility;

   public MaskedList(ObjectArrayList<E> elements, boolean defaultVisibility) {
      this.defaultVisibility = defaultVisibility;
      this.elementToIndex.defaultReturnValue(-1);
      this.addAll(elements);
   }

   public void addOrSet(E element, boolean visible) {
      int index = this.elementToIndex.getInt(element);
      if (index < 0) {
         this.add(element);
         index = this.allElements.size() - 1;
      }
      this.visibleMask.set(index, visible);
   }

   public void setVisible(E element, boolean visible) {
      int index = this.elementToIndex.getInt(element);
      if (index >= 0) {
         this.visibleMask.set(index, visible);
      }
   }

   public int totalSize() {
      return this.allElements.size();
   }

   @Override
   public boolean add(E element) {
      int index = this.allElements.size();
      int oldIndex = this.elementToIndex.putIfAbsent(element, index);
      if (oldIndex >= 0) {
         throw new IllegalStateException("MaskedList cannot contain duplicate elements");
      }
      this.visibleMask.set(index, this.defaultVisibility);
      return this.allElements.add(element);
   }

   @Override
   public boolean remove(Object element) {
      int index = this.elementToIndex.removeInt(element);
      if (index < 0) {
         return false;
      }
      this.visibleMask.clear(index);
      this.allElements.set(index, null);
      this.modCount++;
      return true;
   }

   @Override
   public Iterator<E> iterator() {
      return new Iterator<>() {
         private int nextIndex = MaskedList.this.visibleMask.nextSetBit(0);

         @Override
         public boolean hasNext() {
            return this.nextIndex >= 0;
         }

         @Override
         public E next() {
            if (this.nextIndex < 0) {
               throw new NoSuchElementException();
            }
            E element = MaskedList.this.allElements.get(this.nextIndex);
            this.nextIndex = MaskedList.this.visibleMask.nextSetBit(this.nextIndex + 1);
            return element;
         }
      };
   }

   @Override
   public Spliterator<E> spliterator() {
      return new Spliterators.AbstractSpliterator<>(this.size(), Spliterator.ORDERED | Spliterator.NONNULL) {
         private int nextIndex;

         @Override
         public boolean tryAdvance(Consumer<? super E> action) {
            int index = MaskedList.this.visibleMask.nextSetBit(this.nextIndex);
            if (index < 0) {
               return false;
            }
            this.nextIndex = index + 1;
            action.accept(MaskedList.this.allElements.get(index));
            return true;
         }
      };
   }

   @Override
   public E get(int index) {
      if (index < 0 || index >= this.size()) {
         throw new IndexOutOfBoundsException(index);
      }
      int visibleIndex = this.visibleMask.nextSetBit(0);
      for (int i = 0; i < index; i++) {
         visibleIndex = this.visibleMask.nextSetBit(visibleIndex + 1);
      }
      return this.allElements.get(visibleIndex);
   }

   @Override
   public int size() {
      return this.visibleMask.cardinality();
   }
}
