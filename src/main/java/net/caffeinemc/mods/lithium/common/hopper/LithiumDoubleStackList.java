package net.caffeinemc.mods.lithium.common.hopper;

import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public final class LithiumDoubleStackList extends LithiumStackList {
   private final LithiumStackList first;
   private final LithiumStackList second;
   final LithiumDoubleInventory doubleInventory;
   private long signalStrengthChangeCount = Long.MIN_VALUE;

   private LithiumDoubleStackList(
      LithiumDoubleInventory doubleInventory, LithiumStackList first, LithiumStackList second, int maxCountPerStack
   ) {
      super(DefaultedList.of(), maxCountPerStack);
      this.doubleInventory = doubleInventory;
      this.first = first;
      this.second = second;
   }

   static LithiumDoubleStackList getOrCreate(
      LithiumDoubleInventory doubleInventory, LithiumStackList first, LithiumStackList second, int maxCountPerStack
   ) {
      LithiumDoubleStackList parent = first.parent;
      if (parent == null || parent != second.parent || parent.first != first || parent.second != second) {
         if (parent != null) {
            parent.doubleInventory.lithium$emitRemoved();
         }
         parent = new LithiumDoubleStackList(doubleInventory, first, second, maxCountPerStack);
         first.parent = parent;
         second.parent = parent;
      }
      return parent;
   }

   void changedFromHalf() {
      this.cachedSignalStrength = -1;
   }

   @Override
   public long getModCount() {
      return this.first.getModCount() + this.second.getModCount();
   }

   @Override
   public void changedALot() {
      throw new UnsupportedOperationException("Change a double inventory through one of its halves");
   }

   @Override
   public void changed() {
      throw new UnsupportedOperationException("Change a double inventory through one of its halves");
   }

   @Override
   public ItemStack get(int index) {
      return index >= this.first.size() ? this.second.get(index - this.first.size()) : this.first.get(index);
   }

   @Override
   public ItemStack set(int index, ItemStack element) {
      return index >= this.first.size() ? this.second.set(index - this.first.size(), element) : this.first.set(index, element);
   }

   @Override
   public int size() {
      return this.first.size() + this.second.size();
   }

   @Override
   public void clear() {
      this.first.clear();
      this.second.clear();
   }

   @Override
   public int getSignalStrength(net.minecraft.inventory.Inventory inventory) {
      if (this.first.hasSignalStrengthOverride() || this.second.hasSignalStrengthOverride()) {
         return 0;
      }
      long modCount = this.getModCount();
      if (this.cachedSignalStrength == -1 || this.signalStrengthChangeCount != modCount) {
         this.cachedSignalStrength = this.calculateSignalStrength(Integer.MAX_VALUE);
         this.signalStrengthChangeCount = modCount;
      }
      return this.cachedSignalStrength;
   }

   @Override
   public void setReducedSignalStrengthOverride() {
      this.first.setReducedSignalStrengthOverride();
      this.second.setReducedSignalStrengthOverride();
   }

   @Override
   public void clearSignalStrengthOverride() {
      this.first.clearSignalStrengthOverride();
      this.second.clearSignalStrengthOverride();
   }

   @Override
   public void runComparatorUpdatePatternOnFailedExtract(LithiumStackList masterStackList, net.minecraft.inventory.Inventory inventory) {
      if (inventory instanceof DoubleInventory doubleInventory) {
         this.first.runComparatorUpdatePatternOnFailedExtract(this, doubleInventory.lithium$getFirst());
         this.second.runComparatorUpdatePatternOnFailedExtract(this, doubleInventory.lithium$getSecond());
      }
   }

   @Override
   public void setInventoryModificationCallback(InventoryChangeTracker callback) {
      this.first.setInventoryModificationCallback(callback);
      this.second.setInventoryModificationCallback(callback);
   }

   @Override
   public void removeInventoryModificationCallback(InventoryChangeTracker callback) {
      this.first.removeInventoryModificationCallback(callback);
      this.second.removeInventoryModificationCallback(callback);
   }
}
