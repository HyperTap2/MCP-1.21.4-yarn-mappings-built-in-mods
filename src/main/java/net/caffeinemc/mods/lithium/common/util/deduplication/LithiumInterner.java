package net.caffeinemc.mods.lithium.common.util.deduplication;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public final class LithiumInterner<T> {
   private final ObjectOpenHashSet<T> canonicalStorage = new ObjectOpenHashSet<>();

   @SuppressWarnings("unchecked")
   public <S extends T> S getCanonical(S value) {
      return (S)this.canonicalStorage.addOrGet(value);
   }

   public void deleteCanonical(T value) {
      this.canonicalStorage.remove(value);
   }
}
