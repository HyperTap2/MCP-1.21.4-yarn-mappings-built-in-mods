package net.caffeinemc.mods.lithium.common.hopper;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeListener;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_comparator_tracking.ComparatorTracker;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

public final class LithiumDoubleInventory extends DoubleInventory
   implements LithiumInventory, InventoryChangeTracker, InventoryChangeListener, ComparatorTracker {
   private final LithiumInventory first;
   private final LithiumInventory second;
   private final LithiumDoubleStackList stacks;
   private ReferenceArraySet<InventoryChangeListener> contentListeners;
   private ReferenceArraySet<InventoryChangeListener> majorListeners;

   public static Inventory replace(DoubleInventory inventory) {
      Inventory vanillaFirst = inventory.lithium$getFirst();
      Inventory vanillaSecond = inventory.lithium$getSecond();
      if (vanillaFirst == vanillaSecond || !(vanillaFirst instanceof LithiumInventory first) || !(vanillaSecond instanceof LithiumInventory second)) {
         return inventory;
      }
      LithiumDoubleInventory replacement = new LithiumDoubleInventory(first, second);
      return replacement.stacks.doubleInventory;
   }

   private LithiumDoubleInventory(LithiumInventory first, LithiumInventory second) {
      super(first, second);
      this.first = first;
      this.second = second;
      this.stacks = LithiumDoubleStackList.getOrCreate(
         this, InventoryHelper.getLithiumStackList(first), InventoryHelper.getLithiumStackList(second), this.getMaxCountPerStack()
      );
   }

   @Override
   public DefaultedList<ItemStack> getInventoryLithium() {
      return this.stacks;
   }

   @Override
   public void setInventoryLithium(DefaultedList<ItemStack> inventory) {
      throw new UnsupportedOperationException("A double inventory is backed by its two halves");
   }

   @Override
   public void lithium$emitContentModified() {
      if (this.contentListeners != null) {
         for (InventoryChangeListener listener : this.contentListeners) {
            listener.lithium$handleInventoryContentModified(this);
         }
         this.contentListeners.clear();
      }
   }

   @Override
   public void lithium$emitStackListReplaced() {
      this.lithium$notifyMajorListeners(true);
   }

   @Override
   public void lithium$emitRemoved() {
      this.lithium$notifyMajorListeners(false);
   }

   private void lithium$notifyMajorListeners(boolean replaced) {
      if (this.majorListeners != null) {
         for (InventoryChangeListener listener : this.majorListeners) {
            if (replaced) listener.handleStackListReplaced(this);
            else listener.lithium$handleInventoryRemoved(this);
         }
         this.majorListeners.clear();
      }
      this.stacks.removeInventoryModificationCallback(this);
   }

   @Override
   public void lithium$emitFirstComparatorAdded() {
      if (this.contentListeners != null) {
         this.contentListeners.removeIf(listener -> listener.lithium$handleComparatorAdded(this));
      }
   }

   @Override
   public void lithium$forwardContentChangeOnce(InventoryChangeListener listener, LithiumStackList stackList, InventoryChangeTracker tracker) {
      if (this.contentListeners == null) this.contentListeners = new ReferenceArraySet<>(1);
      stackList.setInventoryModificationCallback(tracker);
      this.contentListeners.add(listener);
   }

   @Override
   public void lithium$forwardMajorInventoryChanges(InventoryChangeListener listener) {
      if (this.majorListeners == null) {
         this.majorListeners = new ReferenceArraySet<>(1);
         ((InventoryChangeTracker)this.first).listenForMajorInventoryChanges(this);
         ((InventoryChangeTracker)this.second).listenForMajorInventoryChanges(this);
      }
      this.majorListeners.add(listener);
   }

   @Override
   public void lithium$stopForwardingMajorInventoryChanges(InventoryChangeListener listener) {
      if (this.majorListeners != null && this.majorListeners.remove(listener) && this.majorListeners.isEmpty()) {
         ((InventoryChangeTracker)this.first).stopListenForMajorInventoryChanges(this);
         ((InventoryChangeTracker)this.second).stopListenForMajorInventoryChanges(this);
      }
   }

   @Override
   public void lithium$handleInventoryContentModified(Inventory inventory) {
      this.lithium$emitContentModified();
   }

   @Override
   public void lithium$handleInventoryRemoved(Inventory inventory) {
      this.lithium$emitRemoved();
   }

   @Override
   public boolean lithium$handleComparatorAdded(Inventory inventory) {
      this.lithium$emitFirstComparatorAdded();
      return this.contentListeners == null || this.contentListeners.isEmpty();
   }

   @Override
   public void lithium$onComparatorAdded(Direction direction, int offset) {
      throw new UnsupportedOperationException("Notify the matching double-inventory half");
   }

   @Override
   public boolean lithium$hasAnyComparatorNearby() {
      return ((ComparatorTracker)this.first).lithium$hasAnyComparatorNearby()
         || ((ComparatorTracker)this.second).lithium$hasAnyComparatorNearby();
   }
}
