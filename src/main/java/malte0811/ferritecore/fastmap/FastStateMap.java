package malte0811.ferritecore.fastmap;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

public final class FastStateMap<S> {
   private final List<Key<?>> keys;
   private final IdentityHashMap<Property<?>, Key<?>> keysByProperty;
   private final Object[] states;

   public FastStateMap(Iterable<Property<?>> properties, Map<Map<Property<?>, Comparable<?>>, S> stateMap) {
      this.keys = new ArrayList<>();
      this.keysByProperty = new IdentityHashMap<>();
      int factor = 1;
      for (Property<?> property : properties) {
         Key<?> key = new Key<>(property, factor);
         this.keys.add(key);
         this.keysByProperty.put(property, key);
         factor *= key.valueCount();
      }
      this.states = new Object[factor];
      for (Map.Entry<Map<Property<?>, Comparable<?>>, S> entry : stateMap.entrySet()) {
         this.states[this.indexOf(entry.getKey())] = entry.getValue();
      }
   }

   public int indexOf(Map<Property<?>, Comparable<?>> values) {
      int index = 0;
      for (Key<?> key : this.keys) {
         int partial = key.partialIndex(values.get(key.property));
         if (partial < 0) {
            throw new IllegalArgumentException("Invalid state value for " + key.property);
         }
         index += partial;
      }
      return index;
   }

   @Nullable
   public S with(int oldIndex, Property<?> property, Comparable<?> value) {
      Key<?> key = this.keysByProperty.get(property);
      if (key == null) {
         return null;
      }
      int valueIndex = key.valueIndex(value);
      if (valueIndex < 0) {
         return null;
      }
      int current = oldIndex / key.factor % key.valueCount();
      int newIndex = oldIndex + (valueIndex - current) * key.factor;
      return (S)this.states[newIndex];
   }

   @Nullable
   public Comparable<?> get(int stateIndex, Object property) {
      Key<?> key = this.keysByProperty.get(property);
      return key == null ? null : key.valueAt(stateIndex / key.factor % key.valueCount());
   }

   public List<Property<?>> properties() {
      List<Property<?>> properties = new ArrayList<>(this.keys.size());
      for (Key<?> key : this.keys) {
         properties.add(key.property);
      }
      return properties;
   }

   private static final class Key<T extends Comparable<T>> {
      private final Property<T> property;
      private final int factor;
      private final List<T> values;

      private Key(Property<T> property, int factor) {
         this.property = property;
         this.factor = factor;
         this.values = property.getValues();
      }

      private int valueCount() {
         return this.values.size();
      }

      private int partialIndex(Comparable<?> value) {
         int index = this.valueIndex(value);
         return index < 0 ? -1 : index * this.factor;
      }

      private int valueIndex(Comparable<?> value) {
         return this.property.ordinal((T)value);
      }

      private T valueAt(int index) {
         return this.values.get(index);
      }
   }
}
