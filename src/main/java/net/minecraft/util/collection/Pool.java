package net.minecraft.util.collection;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.caffeinemc.mods.lithium.common.util.collections.HashedReferenceList;
import net.caffeinemc.mods.sodium.client.util.WeightedRandomListExtension;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public class Pool<E extends Weighted> implements WeightedRandomListExtension<E> {
   private final int totalWeight;
   private final ImmutableList<E> entries;
   private final List<E> entryHashList;

   Pool(List<? extends E> entries) {
      this.entries = ImmutableList.copyOf(entries);
      this.entryHashList = this.entries.size() > 4
         ? Collections.unmodifiableList(new HashedReferenceList<>(this.entries))
         : this.entries;
      this.totalWeight = Weighting.getWeightSum(entries);
   }

   public static <E extends Weighted> Pool<E> empty() {
      return new Pool<>(ImmutableList.of());
   }

   @SafeVarargs
   public static <E extends Weighted> Pool<E> of(E... entries) {
      return new Pool<>(ImmutableList.copyOf(entries));
   }

   public static <E extends Weighted> Pool<E> of(List<E> entries) {
      return new Pool<>(entries);
   }

   public boolean isEmpty() {
      return this.entries.isEmpty();
   }

   public Optional<E> getOrEmpty(Random random) {
      if (this.totalWeight == 0) {
         return Optional.empty();
      }

      int i = random.nextInt(this.totalWeight);
      return Weighting.getAt(this.entries, i);
   }

   public List<E> getEntries() {
      return this.entryHashList;
   }

   @Override
   public E sodium$getQuick(Random random) {
      if (this.totalWeight == 0) {
         return null;
      }
      int value = Math.abs((int)random.nextLong()) % this.totalWeight;
      for (E entry : this.entries) {
         value -= entry.getWeight().getValue();
         if (value < 0) {
            return entry;
         }
      }
      return null;
   }

   public static <E extends Weighted> Codec<Pool<E>> createCodec(Codec<E> entryCodec) {
      return entryCodec.listOf().xmap(Pool::of, Pool::getEntries);
   }

   @Override
   public boolean equals(@Nullable Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Pool<?> pool = (Pool<?>)o;
         return this.totalWeight == pool.totalWeight && Objects.equals(this.entries, pool.entries);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.totalWeight, this.entries);
   }
}
