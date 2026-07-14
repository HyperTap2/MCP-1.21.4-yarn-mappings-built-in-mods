package net.caffeinemc.mods.lithium.common.hopper;

import net.caffeinemc.mods.lithium.api.inventory.LithiumTransferConditionInventory;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public final class HopperHelper {
   private HopperHelper() {
   }

   public static Inventory replaceDoubleInventory(Inventory inventory) {
      return inventory instanceof DoubleInventory doubleInventory ? LithiumDoubleInventory.replace(doubleInventory) : inventory;
   }

   public static boolean tryMoveSingleItem(Inventory to, ItemStack stack, @Nullable Direction fromDirection) {
      ItemStack checker;
      if (((LithiumTransferConditionInventory)to).lithium$itemInsertionTestRequiresStackSize1()) {
         checker = stack.copyWithCount(1);
      } else {
         checker = stack;
      }
      SidedInventory sided = to instanceof SidedInventory inventory ? inventory : null;
      if (sided != null && fromDirection != null) {
         for (int slot : sided.getAvailableSlots(fromDirection)) {
            if (tryMoveSingleItem(to, sided, stack, checker, slot, fromDirection)) {
               return true;
            }
         }
      } else {
         for (int slot = 0; slot < to.size(); slot++) {
            if (tryMoveSingleItem(to, sided, stack, checker, slot, fromDirection)) {
               return true;
            }
         }
      }
      return false;
   }

   private static boolean tryMoveSingleItem(
      Inventory to, @Nullable SidedInventory sided, ItemStack source, ItemStack checker, int slot, @Nullable Direction direction
   ) {
      ItemStack target = to.getStack(slot);
      if (!to.isValid(slot, checker) || sided != null && !sided.canInsert(slot, checker, direction)) {
         return false;
      }
      if (target.isEmpty()) {
         to.setStack(slot, source.split(1));
         return true;
      }
      int count = target.getCount();
      if (count < target.getMaxCount() && count < to.getMaxCountPerStack() && ItemStack.areItemsAndComponentsEqual(target, source)) {
         source.decrement(1);
         target.increment(1);
         return true;
      }
      return false;
   }

   public static ComparatorUpdatePattern determineComparatorUpdatePattern(Inventory from, LithiumStackList stacks) {
      if (from instanceof HopperBlockEntity || !(from instanceof LootableContainerBlockEntity)) {
         return ComparatorUpdatePattern.NO_UPDATE;
      }
      float weight = 0.0F;
      int occupied = 0;
      for (int i = 0; i < from.size(); i++) {
         ItemStack stack = from.getStack(i);
         if (!stack.isEmpty()) {
            weight += (float)stack.getCount() / Math.min(from.getMaxCountPerStack(), stack.getMaxCount());
            occupied++;
         }
      }
      int original = MathHelper.floor(weight / from.size() * 14.0F) + (occupied > 0 ? 1 : 0);
      ComparatorUpdatePattern pattern = ComparatorUpdatePattern.NO_UPDATE;
      int[] slots = from instanceof SidedInventory sided ? sided.getAvailableSlots(Direction.DOWN) : null;
      int length = slots == null ? from.size() : slots.length;
      for (int i = 0; i < length; i++) {
         int slot = slots == null ? i : slots[i];
         ItemStack stack = stacks.get(slot);
         if (!stack.isEmpty() && (!(from instanceof SidedInventory sided) || sided.canExtract(slot, stack, Direction.DOWN))) {
            int newOccupied = occupied - (stack.getCount() == 1 ? 1 : 0);
            float newWeight = weight - 1.0F / Math.min(from.getMaxCountPerStack(), stack.getMaxCount());
            int reduced = MathHelper.floor(newWeight / from.size() * 14.0F) + (newOccupied > 0 ? 1 : 0);
            pattern = reduced == original ? pattern.thenUpdate() : pattern.thenDecrementUpdateIncrementUpdate();
            if (!pattern.isChainable()) {
               break;
            }
         }
      }
      return pattern;
   }
}
