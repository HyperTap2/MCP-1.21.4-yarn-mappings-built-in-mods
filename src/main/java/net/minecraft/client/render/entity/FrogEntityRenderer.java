package net.minecraft.client.render.entity;

import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.FrogEntityModel;
import net.minecraft.client.render.entity.state.FrogEntityRenderState;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.FrogVariant;
import net.minecraft.util.Identifier;

public class FrogEntityRenderer extends MobEntityRenderer<FrogEntity, FrogEntityRenderState, FrogEntityModel> {
   public FrogEntityRenderer(EntityRendererFactory.Context context) {
      super(context, new FrogEntityModel(context.getPart(EntityModelLayers.FROG)), 0.3F);
   }

   public Identifier getTexture(FrogEntityRenderState frogEntityRenderState) {
      return frogEntityRenderState.texture;
   }

   public FrogEntityRenderState createRenderState() {
      return new FrogEntityRenderState();
   }

   public void updateRenderState(FrogEntity frogEntity, FrogEntityRenderState frogEntityRenderState, float f) {
      super.updateRenderState(frogEntity, frogEntityRenderState, f);
      frogEntityRenderState.insideWaterOrBubbleColumn = frogEntity.isInsideWaterOrBubbleColumn();
      frogEntityRenderState.longJumpingAnimationState.copyFrom(frogEntity.longJumpingAnimationState);
      frogEntityRenderState.croakingAnimationState.copyFrom(frogEntity.croakingAnimationState);
      frogEntityRenderState.usingTongueAnimationState.copyFrom(frogEntity.usingTongueAnimationState);
      frogEntityRenderState.idlingInWaterAnimationState.copyFrom(frogEntity.idlingInWaterAnimationState);
      frogEntityRenderState.texture = ((FrogVariant)frogEntity.getVariant().value()).texture();
   }
}
