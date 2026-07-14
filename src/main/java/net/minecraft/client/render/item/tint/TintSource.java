package net.minecraft.client.render.item.tint;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface TintSource {
   int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user);

   MapCodec<? extends TintSource> getCodec();
}
