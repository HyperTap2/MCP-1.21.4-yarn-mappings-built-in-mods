package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class CamelEntityRenderState extends LivingEntityRenderState {
   public boolean saddled;
   public boolean hasPassengers;
   public float jumpCooldown;
   public final AnimationState sittingTransitionAnimationState = new AnimationState();
   public final AnimationState sittingAnimationState = new AnimationState();
   public final AnimationState standingTransitionAnimationState = new AnimationState();
   public final AnimationState idlingAnimationState = new AnimationState();
   public final AnimationState dashingAnimationState = new AnimationState();
}
