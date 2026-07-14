package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.caffeinemc.mods.lithium.common.entity.TypeFilterableListInternalAccess;
import net.caffeinemc.mods.lithium.common.entity.EntityClassGroup;
import net.caffeinemc.mods.lithium.common.world.chunk.ClassGroupFilterableList;
import net.minecraft.entity.Entity;

public class TypeFilterableList<T> extends AbstractCollection<T> implements TypeFilterableListInternalAccess<T>, ClassGroupFilterableList<T> {
   private final Map<Class<?>, List<T>> elementsByType = new Reference2ReferenceOpenHashMap<>();
   private final Class<T> elementType;
   private final List<T> allElements = Lists.newArrayList();
   private final Reference2ReferenceArrayMap<EntityClassGroup, ReferenceLinkedOpenHashSet<T>> elementsByGroup = new Reference2ReferenceArrayMap<>();

   public TypeFilterableList(Class<T> elementType) {
      this.elementType = elementType;
      this.elementsByType.put(elementType, this.allElements);
   }

   @Override
   public boolean add(T e) {
      boolean bl = false;

      for (Entry<Class<?>, List<T>> entry : this.elementsByType.entrySet()) {
         if (entry.getKey().isInstance(e)) {
            bl |= entry.getValue().add(e);
         }
      }
      if (e instanceof Entity entity) {
         for (Entry<EntityClassGroup, ReferenceLinkedOpenHashSet<T>> entry : this.elementsByGroup.entrySet()) {
            if (entry.getKey().contains(entity)) {
               entry.getValue().add(e);
            }
         }
      }

      return bl;
   }

   @Override
   public boolean remove(Object o) {
      boolean bl = false;

      for (Entry<Class<?>, List<T>> entry : this.elementsByType.entrySet()) {
         if (entry.getKey().isInstance(o)) {
            List<T> list = entry.getValue();
            bl |= list.remove(o);
         }
      }
      for (ReferenceLinkedOpenHashSet<T> elements : this.elementsByGroup.values()) {
         elements.remove(o);
      }

      return bl;
   }

   @Override
   public boolean contains(Object o) {
      return this.getAllOfType(o.getClass()).contains(o);
   }

   public <S> Collection<S> getAllOfType(Class<S> type) {
      Collection<T> collection = this.elementsByType.get(type);
      if (collection == null) {
         collection = this.createAllOfType(type);
      }
      return (Collection<S>)Collections.unmodifiableCollection(collection);
   }

   private <S> Collection<T> createAllOfType(Class<S> type) {
      List<T> list = new ArrayList<>();
      for (T element : this.allElements) {
         if (type.isInstance(element)) {
            list.add(element);
         }
      }
      this.elementsByType.put(type, list);
      return list;
   }

   @Override
   public Collection<T> lithium$getAllOfGroupType(EntityClassGroup group) {
      ReferenceLinkedOpenHashSet<T> cached = this.elementsByGroup.get(group);
      if (cached != null) {
         return Collections.unmodifiableCollection(cached);
      }
      cached = new ReferenceLinkedOpenHashSet<>();
      for (T element : this.allElements) {
         if (element instanceof Entity entity && group.contains(entity)) {
            cached.add(element);
         }
      }
      this.elementsByGroup.put(group, cached);
      return Collections.unmodifiableCollection(cached);
   }

   @Override
   public <S extends T> List<S> lithium$getOrCreateAllOfTypeRaw(Class<S> type) {
      List<T> list = this.elementsByType.get(type);
      if (list == null) {
         this.getAllOfType(type);
         list = this.elementsByType.get(type);
      }
      return (List<S>)list;
   }

   @Override
   public <S extends T> List<S> lithium$replaceCollectionAndGet(Class<S> type, Function<ArrayList<S>, List<S>> listFactory) {
      List<T> oldList = this.elementsByType.get(type);
      List<S> newList = listFactory.apply((ArrayList<S>)(ArrayList<?>)oldList);
      this.elementsByType.put(type, (List<T>)(List<?>)newList);
      return newList;
   }

   @Override
   public <S extends T> List<S> lithium$replaceCollectionAndGet(Class<S> type, ArrayList<S> list) {
      this.elementsByType.put(type, (List<T>)(List<?>)list);
      return list;
   }

   @Override
   public Iterator<T> iterator() {
      return (Iterator<T>)(this.allElements.isEmpty() ? Collections.emptyIterator() : Iterators.unmodifiableIterator(this.allElements.iterator()));
   }

   public List<T> copy() {
      return ImmutableList.copyOf(this.allElements);
   }

   public List<T> lithium$getAllElements() {
      return this.allElements;
   }

   @Override
   public int size() {
      return this.allElements.size();
   }
}
