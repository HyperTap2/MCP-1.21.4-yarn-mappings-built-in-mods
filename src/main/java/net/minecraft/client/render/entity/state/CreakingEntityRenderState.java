package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class CreakingEntityRenderState extends LivingEntityRenderState {
   public final AnimationState invulnerableAnimationState = new AnimationState();
   public final AnimationState attackAnimationState = new AnimationState();
   public final AnimationState crumblingAnimationState = new AnimationState();
   public boolean glowingEyes;
   public boolean unrooted;
}
