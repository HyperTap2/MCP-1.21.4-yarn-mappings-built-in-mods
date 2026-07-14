package net.minecraft.entity;

import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

public interface Saddleable {
   boolean canBeSaddled();

   void saddle(ItemStack stack, @Nullable SoundCategory soundCategory);

   default SoundEvent getSaddleSound() {
      return SoundEvents.ENTITY_HORSE_SADDLE;
   }

   boolean isSaddled();
}
