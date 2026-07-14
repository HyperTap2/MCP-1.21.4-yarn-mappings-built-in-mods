package net.caffeinemc.mods.lithium.api.inventory;

public interface LithiumTransferConditionInventory {
   default boolean lithium$itemInsertionTestRequiresStackSize1() {
      return false;
   }
}
