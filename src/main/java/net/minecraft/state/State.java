package net.minecraft.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import malte0811.ferritecore.fastmap.FastStateEntryMap;
import malte0811.ferritecore.fastmap.FastStateMap;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

public abstract class State<O, S> {
   public static final String NAME = "Name";
   public static final String PROPERTIES = "Properties";
   private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_MAP_PRINTER = new Function<Entry<Property<?>, Comparable<?>>, String>() {
      public String apply(@Nullable Entry<Property<?>, Comparable<?>> entry) {
         if (entry == null) {
            return "<NULL>";
         }

         Property<?> property = entry.getKey();
         return property.getName() + "=" + this.nameValue(property, entry.getValue());
      }

      private <T extends Comparable<T>> String nameValue(Property<T> property, Comparable<?> value) {
         return property.name((T)value);
      }
   };
   protected final O owner;
   private Map<Property<?>, Comparable<?>> propertyMap;
   private FastStateMap<S> ferritecore$stateMap;
   private int ferritecore$stateIndex;
   protected final MapCodec<S> codec;

   protected State(O owner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> propertyMap, MapCodec<S> codec) {
      this.owner = owner;
      this.propertyMap = propertyMap;
      this.codec = codec;
   }

   public <T extends Comparable<T>> S cycle(Property<T> property) {
      return this.with(property, getNext(property.getValues(), this.get(property)));
   }

   protected static <T> T getNext(List<T> values, T value) {
      int i = values.indexOf(value) + 1;
      return i == values.size() ? values.getFirst() : values.get(i);
   }

   @Override
   public String toString() {
      StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append(this.owner);
      if (!this.getEntries().isEmpty()) {
         stringBuilder.append('[');
         stringBuilder.append(this.getEntries().entrySet().stream().map(PROPERTY_MAP_PRINTER).collect(Collectors.joining(",")));
         stringBuilder.append(']');
      }

      return stringBuilder.toString();
   }

   public Collection<Property<?>> getProperties() {
      return Collections.unmodifiableCollection(this.propertyMap.keySet());
   }

   public <T extends Comparable<T>> boolean contains(Property<T> property) {
      return this.propertyMap.containsKey(property);
   }

   public <T extends Comparable<T>> T get(Property<T> property) {
      Comparable<?> comparable = (Comparable<?>)this.propertyMap.get(property);
      if (comparable == null) {
         throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.owner);
      } else {
         return property.getType().cast(comparable);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOrEmpty(Property<T> property) {
      return Optional.ofNullable(this.getNullable(property));
   }

   public <T extends Comparable<T>> T get(Property<T> property, T fallback) {
      return Objects.requireNonNullElse(this.getNullable(property), fallback);
   }

   @Nullable
   public <T extends Comparable<T>> T getNullable(Property<T> property) {
      Comparable<?> comparable = (Comparable<?>)this.propertyMap.get(property);
      return comparable == null ? null : property.getType().cast(comparable);
   }

   public <T extends Comparable<T>, V extends T> S with(Property<T> property, V value) {
      Comparable<?> comparable = (Comparable<?>)this.propertyMap.get(property);
      if (comparable == null) {
         throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.owner);
      } else {
         return this.with(property, value, comparable);
      }
   }

   public <T extends Comparable<T>, V extends T> S withIfExists(Property<T> property, V value) {
      Comparable<?> comparable = (Comparable<?>)this.propertyMap.get(property);
      return (S)(comparable == null ? this : this.with(property, value, comparable));
   }

   private <T extends Comparable<T>, V extends T> S with(Property<T> property, V newValue, Comparable<?> oldValue) {
      if (oldValue.equals(newValue)) {
         return (S)this;
      } else {
         S state = this.ferritecore$stateMap.with(this.ferritecore$stateIndex, property, newValue);
         if (state == null) {
            throw new IllegalArgumentException("Cannot set property " + property + " to " + newValue + " on " + this.owner + ", it is not an allowed value");
         } else {
            return state;
         }
      }
   }

   public void createWithMap(Map<Map<Property<?>, Comparable<?>>, S> states) {
      if (this.ferritecore$stateMap != null) {
         throw new IllegalStateException();
      }
      this.ferritecore$stateMap = new FastStateMap<>(this.propertyMap.keySet(), states);
      this.ferritecore$stateIndex = this.ferritecore$stateMap.indexOf(this.propertyMap);
      this.propertyMap = new FastStateEntryMap(this.ferritecore$stateMap, this.ferritecore$stateIndex);
   }

   public Map<Property<?>, Comparable<?>> getEntries() {
      return this.propertyMap;
   }

   protected static <O, S extends State<O, S>> Codec<S> createCodec(Codec<O> codec, Function<O, S> ownerToStateFunction) {
      return codec.dispatch(
         "Name",
         state -> state.owner,
         owner -> {
            S state = ownerToStateFunction.apply((O)owner);
            return state.getEntries().isEmpty()
               ? MapCodec.unit(state)
               : state.codec.codec().lenientOptionalFieldOf("Properties").xmap(statex -> statex.orElse(state), Optional::of);
         }
      );
   }
}
