package net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking;

import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;

public interface InventoryChangeEmitter {
   void lithium$emitStackListReplaced();
   void lithium$emitRemoved();
   void lithium$emitContentModified();
   void lithium$emitFirstComparatorAdded();
   void lithium$forwardContentChangeOnce(InventoryChangeListener listener, LithiumStackList stackList, InventoryChangeTracker tracker);
   void lithium$forwardMajorInventoryChanges(InventoryChangeListener listener);
   void lithium$stopForwardingMajorInventoryChanges(InventoryChangeListener listener);

   default void emitCallbackReplaced() {
      this.lithium$emitRemoved();
   }
}
