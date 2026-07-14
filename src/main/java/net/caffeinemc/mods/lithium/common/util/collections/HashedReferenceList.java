package net.caffeinemc.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.AbstractList;
import java.util.Collection;

public class HashedReferenceList<T> extends AbstractList<T> {
   private final ReferenceArrayList<T> elements;
   private final Reference2IntOpenHashMap<T> counts = new Reference2IntOpenHashMap<>();

   public HashedReferenceList(Collection<? extends T> elements) {
      this.elements = new ReferenceArrayList<>(elements);
      this.counts.defaultReturnValue(0);
      for (T element : elements) {
         this.counts.addTo(element, 1);
      }
   }

   @Override
   public T get(int index) {
      return this.elements.get(index);
   }

   @Override
   public int size() {
      return this.elements.size();
   }

   @Override
   public boolean contains(Object element) {
      return this.counts.containsKey(element);
   }

   @Override
   public T set(int index, T element) {
      T previous = this.elements.set(index, element);
      if (previous != element) {
         this.decrement(previous);
         this.counts.addTo(element, 1);
      }
      return previous;
   }

   @Override
   public void add(int index, T element) {
      this.elements.add(index, element);
      this.counts.addTo(element, 1);
      this.modCount++;
   }

   @Override
   public T remove(int index) {
      T removed = this.elements.remove(index);
      this.decrement(removed);
      this.modCount++;
      return removed;
   }

   private void decrement(T element) {
      if (this.counts.addTo(element, -1) <= 1) {
         this.counts.removeInt(element);
      }
   }
}
