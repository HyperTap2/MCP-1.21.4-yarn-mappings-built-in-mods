package com.viaversion.viafabricplus.features.item.negative_item_count;

import com.viaversion.viafabricplus.util.ItemUtil;
import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class NegativeItemUtil {
   private static final String VV_IDENTIFIER = "VV|" + Protocol1_10To1_11.class.getSimpleName();

   public static int getCount(ItemStack stack) {
      NbtCompound tag = ItemUtil.getTagOrNull(stack);
      return tag != null && tag.contains(VV_IDENTIFIER) ? tag.getInt(VV_IDENTIFIER) : stack.getCount();
   }
}
