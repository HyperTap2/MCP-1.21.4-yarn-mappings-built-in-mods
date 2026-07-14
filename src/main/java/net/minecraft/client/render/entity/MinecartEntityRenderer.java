package net.minecraft.client.render.entity;

import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.MinecartEntityRenderState;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;

public class MinecartEntityRenderer extends AbstractMinecartEntityRenderer<AbstractMinecartEntity, MinecartEntityRenderState> {
   public MinecartEntityRenderer(EntityRendererFactory.Context context, EntityModelLayer entityModelLayer) {
      super(context, entityModelLayer);
   }

   public MinecartEntityRenderState createRenderState() {
      return new MinecartEntityRenderState();
   }
}
