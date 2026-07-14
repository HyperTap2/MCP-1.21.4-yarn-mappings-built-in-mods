package net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking;

import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;

public interface InventoryChangeTracker extends InventoryChangeEmitter {
   default void listenForContentChangesOnce(LithiumStackList stackList, InventoryChangeListener listener) {
      this.lithium$forwardContentChangeOnce(listener, stackList, this);
   }

   default void listenForMajorInventoryChanges(InventoryChangeListener listener) {
      this.lithium$forwardMajorInventoryChanges(listener);
   }

   default void stopListenForMajorInventoryChanges(InventoryChangeListener listener) {
      this.lithium$stopForwardingMajorInventoryChanges(listener);
   }
}
