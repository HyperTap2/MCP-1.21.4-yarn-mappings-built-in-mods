package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.AxolotlEntity.Variant;

public class AxolotlEntityRenderState extends LivingEntityRenderState {
   public Variant variant = Variant.LUCY;
   public float playingDeadValue;
   public float isMovingValue;
   public float inWaterValue = 1.0F;
   public float onGroundValue;
}
