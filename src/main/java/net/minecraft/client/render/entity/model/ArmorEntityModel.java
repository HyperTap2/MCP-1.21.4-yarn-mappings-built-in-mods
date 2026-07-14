package net.minecraft.client.render.entity.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;

public class ArmorEntityModel<S extends BipedEntityRenderState> extends BipedEntityModel<S> {
   public ArmorEntityModel(ModelPart modelPart) {
      super(modelPart);
   }

   public static ModelData getModelData(Dilation dilation) {
      ModelData modelData = BipedEntityModel.getModelData(dilation, 0.0F);
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "right_leg",
         ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
         ModelTransform.pivot(-1.9F, 12.0F, 0.0F)
      );
      modelPartData.addChild(
         "left_leg",
         ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, dilation.add(-0.1F)),
         ModelTransform.pivot(1.9F, 12.0F, 0.0F)
      );
      return modelData;
   }
}
