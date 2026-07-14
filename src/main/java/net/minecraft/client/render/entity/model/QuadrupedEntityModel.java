package net.minecraft.client.render.entity.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.math.MathHelper;

public class QuadrupedEntityModel<T extends LivingEntityRenderState> extends EntityModel<T> {
   protected final ModelPart head;
   protected final ModelPart body;
   protected final ModelPart rightHindLeg;
   protected final ModelPart leftHindLeg;
   protected final ModelPart rightFrontLeg;
   protected final ModelPart leftFrontLeg;

   protected QuadrupedEntityModel(ModelPart root) {
      super(root);
      this.head = root.getChild("head");
      this.body = root.getChild("body");
      this.rightHindLeg = root.getChild("right_hind_leg");
      this.leftHindLeg = root.getChild("left_hind_leg");
      this.rightFrontLeg = root.getChild("right_front_leg");
      this.leftFrontLeg = root.getChild("left_front_leg");
   }

   public static ModelData getModelData(int stanceWidth, Dilation dilation) {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      modelPartData.addChild(
         "head",
         ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F, dilation),
         ModelTransform.pivot(0.0F, 18 - stanceWidth, -6.0F)
      );
      modelPartData.addChild(
         "body",
         ModelPartBuilder.create().uv(28, 8).cuboid(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, dilation),
         ModelTransform.of(0.0F, 17 - stanceWidth, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, stanceWidth, 4.0F, dilation);
      modelPartData.addChild("right_hind_leg", modelPartBuilder, ModelTransform.pivot(-3.0F, 24 - stanceWidth, 7.0F));
      modelPartData.addChild("left_hind_leg", modelPartBuilder, ModelTransform.pivot(3.0F, 24 - stanceWidth, 7.0F));
      modelPartData.addChild("right_front_leg", modelPartBuilder, ModelTransform.pivot(-3.0F, 24 - stanceWidth, -5.0F));
      modelPartData.addChild("left_front_leg", modelPartBuilder, ModelTransform.pivot(3.0F, 24 - stanceWidth, -5.0F));
      return modelData;
   }

   public void setAngles(T livingEntityRenderState) {
      super.setAngles(livingEntityRenderState);
      this.head.pitch = livingEntityRenderState.pitch * (float) (Math.PI / 180.0);
      this.head.yaw = livingEntityRenderState.yawDegrees * (float) (Math.PI / 180.0);
      float f = livingEntityRenderState.limbFrequency;
      float g = livingEntityRenderState.limbAmplitudeMultiplier;
      this.rightHindLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
      this.leftHindLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
      this.rightFrontLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
      this.leftFrontLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
   }
}
