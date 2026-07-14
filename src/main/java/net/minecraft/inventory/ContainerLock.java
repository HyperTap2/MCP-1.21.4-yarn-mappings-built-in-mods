package net.minecraft.inventory;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryWrapper;

public record ContainerLock(ItemPredicate predicate) {
   public static final ContainerLock EMPTY = new ContainerLock(ItemPredicate.Builder.create().build());
   public static final Codec<ContainerLock> CODEC = ItemPredicate.CODEC.xmap(ContainerLock::new, ContainerLock::predicate);
   public static final String LOCK_KEY = "lock";

   public boolean canOpen(ItemStack stack) {
      return this.predicate.test(stack);
   }

   public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      if (this != EMPTY) {
         DataResult<NbtElement> dataResult = CODEC.encode(this, registries.getOps(NbtOps.INSTANCE), new NbtCompound());
         dataResult.result().ifPresent(lock -> nbt.put("lock", lock));
      }
   }

   public static ContainerLock fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
      if (nbt.contains("lock", 10)) {
         DataResult<Pair<ContainerLock, NbtElement>> dataResult = CODEC.decode(registries.getOps(NbtOps.INSTANCE), nbt.get("lock"));
         if (dataResult.isSuccess()) {
            return (ContainerLock)((Pair)dataResult.getOrThrow()).getFirst();
         }
      }

      return EMPTY;
   }
}
