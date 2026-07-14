package net.minecraft.client.render.item.model.special;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.LoadedEntityModels;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import org.jetbrains.annotations.Nullable;

public interface SpecialModelRenderer<T> {
   void render(
      @Nullable T data,
      ModelTransformationMode modelTransformationMode,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      boolean glint
   );

   @Nullable
   T getData(ItemStack stack);

   interface Unbaked {
      @Nullable
      SpecialModelRenderer<?> bake(LoadedEntityModels entityModels);

      MapCodec<? extends SpecialModelRenderer.Unbaked> getCodec();
   }
}
