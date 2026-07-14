package net.minecraft.client.render.entity.state;

import net.minecraft.entity.AnimationState;

public class BatEntityRenderState extends LivingEntityRenderState {
   public boolean roosting;
   public final AnimationState flyingAnimationState = new AnimationState();
   public final AnimationState roostingAnimationState = new AnimationState();
}
