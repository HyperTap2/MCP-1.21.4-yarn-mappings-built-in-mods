package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.FoxEntity.Type;

public class FoxEntityRenderState extends ItemHolderEntityRenderState {
   public float headRoll;
   public float bodyRotationHeightOffset;
   public boolean inSneakingPose;
   public boolean sleeping;
   public boolean sitting;
   public boolean walking;
   public boolean chasing;
   public Type type = Type.RED;
}
