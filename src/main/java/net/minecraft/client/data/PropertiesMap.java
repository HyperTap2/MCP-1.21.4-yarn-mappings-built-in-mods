package net.minecraft.client.data;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.state.property.Property.Value;

public final class PropertiesMap {
   private static final PropertiesMap EMPTY = new PropertiesMap(ImmutableList.of());
   private static final Comparator<Value<?>> COMPARATOR = Comparator.comparing(value -> value.property().getName());
   private final List<Value<?>> values;

   public PropertiesMap withValue(Value<?> value) {
      return new PropertiesMap(ImmutableList.<Value<?>>builder().addAll(this.values).add(value).build());
   }

   public PropertiesMap copyOf(PropertiesMap propertiesMap) {
      return new PropertiesMap(ImmutableList.<Value<?>>builder().addAll(this.values).addAll(propertiesMap.values).build());
   }

   private PropertiesMap(List<Value<?>> values) {
      this.values = values;
   }

   public static PropertiesMap empty() {
      return EMPTY;
   }

   public static PropertiesMap withValues(Value<?>... values) {
      return new PropertiesMap(ImmutableList.copyOf(values));
   }

   @Override
   public boolean equals(Object o) {
      return this == o || o instanceof PropertiesMap && this.values.equals(((PropertiesMap)o).values);
   }

   @Override
   public int hashCode() {
      return this.values.hashCode();
   }

   public String asString() {
      return this.values.stream().sorted(COMPARATOR).<CharSequence>map(Value::toString).collect(Collectors.joining(","));
   }

   @Override
   public String toString() {
      return this.asString();
   }
}
