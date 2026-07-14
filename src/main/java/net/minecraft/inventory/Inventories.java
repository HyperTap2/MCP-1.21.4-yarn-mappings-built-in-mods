package net.minecraft.inventory;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

public class Inventories {
   public static final String ITEMS_NBT_KEY = "Items";

   public static ItemStack splitStack(List<ItemStack> stacks, int slot, int amount) {
      return slot >= 0 && slot < stacks.size() && !stacks.get(slot).isEmpty() && amount > 0 ? stacks.get(slot).split(amount) : ItemStack.EMPTY;
   }

   public static ItemStack removeStack(List<ItemStack> stacks, int slot) {
      return slot >= 0 && slot < stacks.size() ? stacks.set(slot, ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static NbtCompound writeNbt(NbtCompound nbt, DefaultedList<ItemStack> stacks, RegistryWrapper.WrapperLookup registries) {
      return writeNbt(nbt, stacks, true, registries);
   }

   public static NbtCompound writeNbt(NbtCompound nbt, DefaultedList<ItemStack> stacks, boolean setIfEmpty, RegistryWrapper.WrapperLookup registries) {
      NbtList nbtList = new NbtList();

      for (int i = 0; i < stacks.size(); i++) {
         ItemStack itemStack = stacks.get(i);
         if (!itemStack.isEmpty()) {
            NbtCompound nbtCompound = new NbtCompound();
            nbtCompound.putByte("Slot", (byte)i);
            nbtList.add(itemStack.toNbt(registries, nbtCompound));
         }
      }

      if (!nbtList.isEmpty() || setIfEmpty) {
         nbt.put("Items", nbtList);
      }

      return nbt;
   }

   public static void readNbt(NbtCompound nbt, DefaultedList<ItemStack> stacks, RegistryWrapper.WrapperLookup registries) {
      NbtList nbtList = nbt.getList("Items", 10);

      for (int i = 0; i < nbtList.size(); i++) {
         NbtCompound nbtCompound = nbtList.getCompound(i);
         int j = nbtCompound.getByte("Slot") & 255;
         if (j >= 0 && j < stacks.size()) {
            stacks.set(j, ItemStack.fromNbt(registries, nbtCompound).orElse(ItemStack.EMPTY));
         }
      }
   }

   public static int remove(Inventory inventory, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
      int i = 0;

      for (int j = 0; j < inventory.size(); j++) {
         ItemStack itemStack = inventory.getStack(j);
         int k = remove(itemStack, shouldRemove, maxCount - i, dryRun);
         if (k > 0 && !dryRun && itemStack.isEmpty()) {
            inventory.setStack(j, ItemStack.EMPTY);
         }

         i += k;
      }

      return i;
   }

   public static int remove(ItemStack stack, Predicate<ItemStack> shouldRemove, int maxCount, boolean dryRun) {
      if (stack.isEmpty() || !shouldRemove.test(stack)) {
         return 0;
      }

      if (dryRun) {
         return stack.getCount();
      }

      int i = maxCount < 0 ? stack.getCount() : Math.min(maxCount, stack.getCount());
      stack.decrement(i);
      return i;
   }
}
