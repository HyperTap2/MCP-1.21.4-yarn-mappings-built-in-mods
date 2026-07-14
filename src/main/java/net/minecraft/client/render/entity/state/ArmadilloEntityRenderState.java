package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class ArmadilloEntityRenderState extends LivingEntityRenderState {
   public boolean rolledUp;
   public final AnimationState unrollingAnimationState = new AnimationState();
   public final AnimationState rollingAnimationState = new AnimationState();
   public final AnimationState scaredAnimationState = new AnimationState();
}
