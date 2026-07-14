package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class BreezeEntityRenderState extends LivingEntityRenderState {
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState shootingAnimationState = new AnimationState();
   public final AnimationState slidingAnimationState = new AnimationState();
   public final AnimationState slidingBackAnimationState = new AnimationState();
   public final AnimationState inhalingAnimationState = new AnimationState();
   public final AnimationState longJumpingAnimationState = new AnimationState();
}
