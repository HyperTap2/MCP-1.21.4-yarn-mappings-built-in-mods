package net.caffeinemc.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ListeningLong2ObjectOpenHashMap<V> extends Long2ObjectOpenHashMap<V> {
   private final Callback<V> addCallback;
   private final Callback<V> removeCallback;

   public ListeningLong2ObjectOpenHashMap(Callback<V> addCallback, Callback<V> removeCallback) {
      this.addCallback = addCallback;
      this.removeCallback = removeCallback;
   }

   @Override
   public V put(long key, V value) {
      V previous = super.put(key, value);
      if (previous != value) {
         if (previous != null) {
            this.removeCallback.apply(key, value);
         }
         this.addCallback.apply(key, value);
      }
      return previous;
   }

   @Override
   public V remove(long key) {
      V previous = super.remove(key);
      if (previous != null) {
         this.removeCallback.apply(key, previous);
      }
      return previous;
   }

   @FunctionalInterface
   public interface Callback<V> {
      void apply(long key, V value);
   }
}
