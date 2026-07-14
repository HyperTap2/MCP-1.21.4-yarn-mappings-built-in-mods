package net.minecraft.client.render.item.property.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface NumericProperty {
   float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity holder, int seed);

   MapCodec<? extends NumericProperty> getCodec();
}
