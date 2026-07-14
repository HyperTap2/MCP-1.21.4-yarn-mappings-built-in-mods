package com.viaversion.viafabricplus.features.entity.r1_8_boat;

import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.util.Identifier;

public final class BoatModel1_8 extends EntityModel<BoatEntityRenderState> {
   public static final EntityModelLayer MODEL_LAYER = new EntityModelLayer(Identifier.of("viafabricplus", "boat1_8"), "main");

   public BoatModel1_8(ModelPart root) {
      super(root);
   }

   public static TexturedModelData getTexturedModelData() {
      ModelData modelData = new ModelData();
      ModelPartData root = modelData.getRoot();
      float width = 24.0F;
      float wallHeight = 6.0F;
      float baseWidth = 20.0F;
      float pivotY = 4.0F;
      root.addChild(
         "bottom",
         ModelPartBuilder.create().uv(0, 8).cuboid(-12.0F, -8.0F, -3.0F, 24.0F, 16.0F, 4.0F),
         ModelTransform.of(0.0F, 4.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addChild(
         "back",
         ModelPartBuilder.create().uv(0, 0).cuboid(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
         ModelTransform.of(-11.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI * 3.0 / 2.0), 0.0F)
      );
      root.addChild(
         "front",
         ModelPartBuilder.create().uv(0, 0).cuboid(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
         ModelTransform.of(11.0F, 4.0F, 0.0F, 0.0F, (float) (Math.PI / 2), 0.0F)
      );
      root.addChild(
         "right",
         ModelPartBuilder.create().uv(0, 0).cuboid(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F),
         ModelTransform.of(0.0F, 4.0F, -9.0F, 0.0F, (float) Math.PI, 0.0F)
      );
      root.addChild("left", ModelPartBuilder.create().uv(0, 0).cuboid(-10.0F, -7.0F, -1.0F, 20.0F, 6.0F, 2.0F), ModelTransform.pivot(0.0F, 4.0F, 9.0F));
      return TexturedModelData.of(modelData, 64, 32);
   }

   public void setAngles(BoatEntityRenderState state) {
   }
}
