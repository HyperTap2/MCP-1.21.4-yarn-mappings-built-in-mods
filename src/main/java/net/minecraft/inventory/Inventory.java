package net.minecraft.inventory;

import java.util.Set;
import java.util.function.Predicate;
import net.caffeinemc.mods.lithium.api.inventory.LithiumCooldownReceivingInventory;
import net.caffeinemc.mods.lithium.api.inventory.LithiumTransferConditionInventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface Inventory extends Clearable, LithiumCooldownReceivingInventory, LithiumTransferConditionInventory {
   float DEFAULT_MAX_INTERACTION_RANGE = 4.0F;

   int size();

   boolean isEmpty();

   ItemStack getStack(int slot);

   ItemStack removeStack(int slot, int amount);

   ItemStack removeStack(int slot);

   void setStack(int slot, ItemStack stack);

   default int getMaxCountPerStack() {
      return 99;
   }

   default int getMaxCount(ItemStack stack) {
      return Math.min(this.getMaxCountPerStack(), stack.getMaxCount());
   }

   void markDirty();

   boolean canPlayerUse(PlayerEntity player);

   default void onOpen(PlayerEntity player) {
   }

   default void onClose(PlayerEntity player) {
   }

   default boolean isValid(int slot, ItemStack stack) {
      return true;
   }

   default boolean canTransferTo(Inventory hopperInventory, int slot, ItemStack stack) {
      return true;
   }

   default int count(Item item) {
      int i = 0;

      for (int j = 0; j < this.size(); j++) {
         ItemStack itemStack = this.getStack(j);
         if (itemStack.getItem().equals(item)) {
            i += itemStack.getCount();
         }
      }

      return i;
   }

   default boolean containsAny(Set<Item> items) {
      return this.containsAny(stack -> !stack.isEmpty() && items.contains(stack.getItem()));
   }

   default boolean containsAny(Predicate<ItemStack> predicate) {
      for (int i = 0; i < this.size(); i++) {
         ItemStack itemStack = this.getStack(i);
         if (predicate.test(itemStack)) {
            return true;
         }
      }

      return false;
   }

   static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player) {
      return canPlayerUse(blockEntity, player, 4.0F);
   }

   static boolean canPlayerUse(BlockEntity blockEntity, PlayerEntity player, float range) {
      World world = blockEntity.getWorld();
      BlockPos blockPos = blockEntity.getPos();
      if (world == null) {
         return false;
      } else {
         return world.getBlockEntity(blockPos) != blockEntity ? false : player.canInteractWithBlockAt(blockPos, range);
      }
   }
}
