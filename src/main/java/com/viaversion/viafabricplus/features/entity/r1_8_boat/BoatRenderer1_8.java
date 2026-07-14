package com.viaversion.viafabricplus.features.entity.r1_8_boat;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.AbstractBoatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.BoatEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class BoatRenderer1_8 extends AbstractBoatEntityRenderer {
   private static final Identifier TEXTURE = Identifier.of("viafabricplus", "textures/boat1_8.png");
   private final BoatModel1_8 model;

   public BoatRenderer1_8(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.5F;
      this.model = new BoatModel1_8(ctx.getPart(BoatModel1_8.MODEL_LAYER));
   }

   protected EntityModel<BoatEntityRenderState> getModel() {
      return this.model;
   }

   protected RenderLayer getRenderLayer() {
      return this.model.getLayer(TEXTURE);
   }

   public void render(BoatEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
      matrices.push();
      matrices.translate(0.0, 0.25, 0.0);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
      if (state.damageWobbleTicks > 0.0F) {
         matrices.multiply(
            RotationAxis.POSITIVE_X
               .rotationDegrees(MathHelper.sin(state.damageWobbleTicks) * state.damageWobbleTicks * state.damageWobbleStrength / 10.0F * state.damageWobbleSide)
         );
      }

      matrices.scale(-1.0F, -1.0F, 1.0F);
      this.model.setAngles(state);
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.model.getLayer(TEXTURE));
      this.model.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
      matrices.pop();
   }
}
