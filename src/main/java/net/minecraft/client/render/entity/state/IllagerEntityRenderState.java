package net.minecraft.client.render.entity.state;

import net.minecraft.entity.mob.IllagerEntity.State;
import net.minecraft.util.Arm;

public class IllagerEntityRenderState extends ArmedEntityRenderState {
   public boolean hasVehicle;
   public boolean attacking;
   public Arm illagerMainArm = Arm.RIGHT;
   public State illagerState = State.NEUTRAL;
   public int crossbowPullTime;
   public int itemUseTime;
   public float handSwingProgress;
}
