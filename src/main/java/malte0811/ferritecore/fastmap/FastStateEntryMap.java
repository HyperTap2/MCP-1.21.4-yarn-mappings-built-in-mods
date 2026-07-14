package malte0811.ferritecore.fastmap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.state.property.Property;

public final class FastStateEntryMap extends AbstractMap<Property<?>, Comparable<?>> {
   private final FastStateMap<?> stateMap;
   private final int stateIndex;
   private final Set<Entry<Property<?>, Comparable<?>>> entries = new AbstractSet<>() {
      @Override
      public Iterator<Entry<Property<?>, Comparable<?>>> iterator() {
         ArrayList<Entry<Property<?>, Comparable<?>>> entries = new ArrayList<>();
         for (Property<?> property : FastStateEntryMap.this.stateMap.properties()) {
            entries.add(Map.entry(property, java.util.Objects.requireNonNull(FastStateEntryMap.this.stateMap.get(FastStateEntryMap.this.stateIndex, property))));
         }
         return entries.iterator();
      }

      @Override
      public int size() {
         return FastStateEntryMap.this.stateMap.properties().size();
      }
   };

   public FastStateEntryMap(FastStateMap<?> stateMap, int stateIndex) {
      this.stateMap = stateMap;
      this.stateIndex = stateIndex;
   }

   @Override
   public Comparable<?> get(Object key) {
      return this.stateMap.get(this.stateIndex, key);
   }

   @Override
   public boolean containsKey(Object key) {
      return key instanceof Property<?> && this.stateMap.properties().contains(key);
   }

   @Override
   public Set<Entry<Property<?>, Comparable<?>>> entrySet() {
      return this.entries;
   }
}
