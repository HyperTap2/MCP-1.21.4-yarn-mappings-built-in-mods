package net.caffeinemc.mods.lithium.api.inventory;

import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface LithiumInventory extends Inventory {
   DefaultedList<ItemStack> getInventoryLithium();

   void setInventoryLithium(DefaultedList<ItemStack> inventory);

   default void generateLootLithium() {
      if (this instanceof LootableContainerBlockEntity container) {
         container.generateLoot(null);
      }
      if (this instanceof VehicleInventory vehicleInventory) {
         vehicleInventory.generateInventoryLoot(null);
      }
   }
}
