package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.Nullable;

public record DamagedProperty() implements BooleanProperty {
   public static final MapCodec<DamagedProperty> CODEC = MapCodec.unit(new DamagedProperty());

   @Override
   public boolean getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode) {
      return stack.isDamaged();
   }

   @Override
   public MapCodec<DamagedProperty> getCodec() {
      return CODEC;
   }
}
