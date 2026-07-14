package net.minecraft.client.render.entity.state;

import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.entity.passive.ParrotEntity.Variant;

public class ParrotEntityRenderState extends LivingEntityRenderState {
   public Variant variant = Variant.RED_BLUE;
   public float flapAngle;
   public ParrotEntityModel.Pose parrotPose = ParrotEntityModel.Pose.FLYING;
}
