package net.caffeinemc.mods.lithium.common.hopper;

import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public final class InventoryHelper {
   private InventoryHelper() {
   }

   public static LithiumStackList getLithiumStackList(LithiumInventory inventory) {
      DefaultedList<ItemStack> stacks = inventory.getInventoryLithium();
      if (stacks instanceof LithiumStackList lithiumStackList) {
         return lithiumStackList;
      }
      inventory.generateLootLithium();
      LithiumStackList upgraded = new LithiumStackList(inventory.getInventoryLithium(), inventory.getMaxCountPerStack());
      inventory.setInventoryLithium(upgraded);
      return upgraded;
   }

   public static LithiumStackList getLithiumStackListOrNull(LithiumInventory inventory) {
      return inventory.getInventoryLithium() instanceof LithiumStackList stacks ? stacks : null;
   }
}
