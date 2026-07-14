package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class SnifferEntityRenderState extends LivingEntityRenderState {
   public boolean searching;
   public final AnimationState diggingAnimationState = new AnimationState();
   public final AnimationState sniffingAnimationState = new AnimationState();
   public final AnimationState risingAnimationState = new AnimationState();
   public final AnimationState feelingHappyAnimationState = new AnimationState();
   public final AnimationState scentingAnimationState = new AnimationState();
}
