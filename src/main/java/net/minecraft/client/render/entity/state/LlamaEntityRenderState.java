package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.LlamaEntity.Variant;
import net.minecraft.item.ItemStack;

public class LlamaEntityRenderState extends LivingEntityRenderState {
   public Variant variant = Variant.CREAMY;
   public boolean hasChest;
   public ItemStack bodyArmor = ItemStack.EMPTY;
   public boolean trader;
}
