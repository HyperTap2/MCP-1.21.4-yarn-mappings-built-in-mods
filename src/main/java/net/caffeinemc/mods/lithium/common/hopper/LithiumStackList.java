package net.caffeinemc.mods.lithium.common.hopper;

import net.caffeinemc.mods.lithium.api.inventory.LithiumDefaultedList;
import net.caffeinemc.mods.lithium.common.block.entity.inventory_change_tracking.InventoryChangeTracker;
import net.caffeinemc.mods.lithium.common.util.change_tracking.ChangePublisher;
import net.caffeinemc.mods.lithium.common.util.change_tracking.ChangeSubscriber;
import net.minecraft.inventory.Inventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public class LithiumStackList extends DefaultedList<ItemStack>
   implements LithiumDefaultedList, ChangeSubscriber.CountChangeSubscriber<ItemStack> {
   private final int maxCountPerStack;
   protected int cachedSignalStrength = -1;
   private boolean signalStrengthOverride;
   private ComparatorUpdatePattern cachedComparatorUpdatePattern;
   private long modCount;
   private int occupiedSlots;
   private int fullSlots;
   private InventoryChangeTracker inventoryModificationCallback;
   LithiumDoubleStackList parent;

   public LithiumStackList(DefaultedList<ItemStack> original, int maxCountPerStack) {
      super(original.lithium$getDelegate(), ItemStack.EMPTY);
      this.maxCountPerStack = maxCountPerStack;
      this.recountAndSubscribe();
   }

   public long getModCount() {
      return this.modCount;
   }

   private void recountAndSubscribe() {
      this.occupiedSlots = 0;
      this.fullSlots = 0;
      for (int i = 0; i < this.size(); i++) {
         ItemStack stack = this.get(i);
         if (!stack.isEmpty()) {
            this.occupiedSlots++;
            if (stack.getCount() >= stack.getMaxCount()) {
               this.fullSlots++;
            }
            if (!stack.lithium$isSubscribedWithData(this, i)) {
               stack.lithium$subscribe(this, i);
            }
         }
      }
   }

   public void changedALot() {
      this.changed();
      for (ItemStack stack : this) {
         if (!stack.isEmpty()) {
            stack.lithium$unsubscribe(this);
         }
      }
      this.recountAndSubscribe();
   }

   public void changed() {
      this.cachedSignalStrength = -1;
      this.cachedComparatorUpdatePattern = null;
      this.modCount++;
      InventoryChangeTracker callback = this.inventoryModificationCallback;
      if (callback != null) {
         this.inventoryModificationCallback = null;
         callback.lithium$emitContentModified();
      }
      if (this.parent != null) {
         this.parent.changedFromHalf();
      }
   }

   @Override
   public ItemStack set(int index, ItemStack element) {
      ItemStack previous = super.set(index, element);
      if (previous == element && !element.isEmpty() && !element.lithium$isSubscribedWithData(this, index)) {
         previous = ItemStack.EMPTY;
      }
      if (previous != element) {
         if (!previous.isEmpty()) {
            previous.lithium$unsubscribeWithData(this, index);
         }
         if (!element.isEmpty()) {
            element.lithium$subscribe(this, index);
         }
         this.occupiedSlots += (previous.isEmpty() ? 1 : 0) - (element.isEmpty() ? 1 : 0);
         this.fullSlots += (element.getCount() >= element.getMaxCount() ? 1 : 0)
            - (previous.getCount() >= previous.getMaxCount() ? 1 : 0);
         this.changed();
      }
      return previous;
   }

   @Override
   public void add(int index, ItemStack element) {
      super.add(index, element);
      this.changedALot();
   }

   @Override
   public ItemStack remove(int index) {
      ItemStack previous = super.remove(index);
      if (!previous.isEmpty()) {
         previous.lithium$unsubscribeWithData(this, index);
      }
      this.changedALot();
      return previous;
   }

   @Override
   public void clear() {
      for (int i = 0; i < this.size(); i++) {
         ItemStack stack = this.get(i);
         if (!stack.isEmpty()) {
            stack.lithium$unsubscribeWithData(this, i);
         }
      }
      super.clear();
      this.changedALot();
   }

   public int getSignalStrength(Inventory inventory) {
      if (this.signalStrengthOverride) {
         return 0;
      }
      if (this.cachedSignalStrength == -1) {
         this.cachedSignalStrength = this.calculateSignalStrength(inventory.size());
      }
      return this.cachedSignalStrength;
   }

   public void setReducedSignalStrengthOverride() {
      this.signalStrengthOverride = true;
   }

   public void clearSignalStrengthOverride() {
      this.signalStrengthOverride = false;
   }

   boolean hasSignalStrengthOverride() {
      return this.signalStrengthOverride;
   }

   public void runComparatorUpdatePatternOnFailedExtract(LithiumStackList masterStackList, Inventory inventory) {
      if (inventory instanceof BlockEntity blockEntity) {
         if (this.cachedComparatorUpdatePattern == null) {
            this.cachedComparatorUpdatePattern = HopperHelper.determineComparatorUpdatePattern(inventory, masterStackList);
         }
         this.cachedComparatorUpdatePattern.apply(blockEntity, masterStackList);
      }
   }

   public boolean maybeSendsComparatorUpdatesOnFailedExtract() {
      return this.cachedComparatorUpdatePattern == null || this.cachedComparatorUpdatePattern != ComparatorUpdatePattern.NO_UPDATE;
   }

   protected int calculateSignalStrength(int inventorySize) {
      float fullness = 0.0F;
      int occupied = 0;
      inventorySize = Math.min(inventorySize, this.size());
      for (int i = 0; i < inventorySize; i++) {
         ItemStack stack = this.get(i);
         if (!stack.isEmpty()) {
            fullness += (float)stack.getCount() / Math.min(this.maxCountPerStack, stack.getMaxCount());
            occupied++;
         }
      }
      fullness /= inventorySize;
      return MathHelper.floor(fullness * 14.0F) + (occupied > 0 ? 1 : 0);
   }

   public int getOccupiedSlots() {
      return this.occupiedSlots;
   }

   public int getFullSlots() {
      return this.fullSlots;
   }

   @Override
   public void changedInteractionConditions() {
      this.changed();
   }

   public void setInventoryModificationCallback(InventoryChangeTracker callback) {
      if (this.inventoryModificationCallback != null && this.inventoryModificationCallback != callback) {
         this.inventoryModificationCallback.emitCallbackReplaced();
      }
      this.inventoryModificationCallback = callback;
   }

   public void removeInventoryModificationCallback(InventoryChangeTracker callback) {
      if (this.inventoryModificationCallback == callback) {
         this.inventoryModificationCallback = null;
      }
   }

   @Override
   public void lithium$notify(@Nullable ItemStack publisher, int subscriberData) {
      this.changed();
   }

   @Override
   public void lithium$forceUnsubscribe(ItemStack publisher, int subscriberData) {
      throw new UnsupportedOperationException("Cannot force-unsubscribe a LithiumStackList");
   }

   @Override
   public void lithium$notifyCount(ItemStack stack, int index, int newCount) {
      if (stack != this.get(index)) {
         return;
      }
      int oldCount = stack.getCount();
      if (newCount <= 0) {
         stack.lithium$unsubscribeWithData(this, index);
      }
      int maxCount = stack.getMaxCount();
      this.occupiedSlots -= newCount <= 0 ? 1 : 0;
      this.fullSlots += (newCount >= maxCount ? 1 : 0) - (oldCount >= maxCount ? 1 : 0);
      this.changed();
   }
}
