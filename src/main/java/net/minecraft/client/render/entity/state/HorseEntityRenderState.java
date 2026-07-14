package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.HorseColor;
import net.minecraft.entity.passive.HorseMarking;
import net.minecraft.item.ItemStack;

public class HorseEntityRenderState extends LivingHorseEntityRenderState {
   public HorseColor color = HorseColor.WHITE;
   public HorseMarking marking = HorseMarking.NONE;
   public ItemStack armor = ItemStack.EMPTY;
}
