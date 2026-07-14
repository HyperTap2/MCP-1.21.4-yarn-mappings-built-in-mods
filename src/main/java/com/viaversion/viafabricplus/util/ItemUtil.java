package com.viaversion.viafabricplus.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class ItemUtil {
   public static NbtCompound getTagOrNull(ItemStack stack) {
      NbtComponent tag = (NbtComponent)stack.get(DataComponentTypes.CUSTOM_DATA);
      return tag != null ? tag.copyNbt() : null;
   }
}
