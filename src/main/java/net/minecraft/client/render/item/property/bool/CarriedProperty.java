package net.minecraft.client.render.item.property.bool;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.Nullable;

public record CarriedProperty() implements BooleanProperty {
   public static final MapCodec<CarriedProperty> CODEC = MapCodec.unit(new CarriedProperty());

   @Override
   public boolean getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity user, int seed, ModelTransformationMode modelTransformationMode) {
      return user instanceof ClientPlayerEntity clientPlayerEntity && clientPlayerEntity.currentScreenHandler.getCursorStack() == stack;
   }

   @Override
   public MapCodec<CarriedProperty> getCodec() {
      return CODEC;
   }
}
