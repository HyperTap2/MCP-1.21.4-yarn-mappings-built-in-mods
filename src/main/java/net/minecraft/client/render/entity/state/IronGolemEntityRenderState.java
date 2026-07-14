package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.Cracks.CrackLevel;

public class IronGolemEntityRenderState extends LivingEntityRenderState {
   public float attackTicksLeft;
   public int lookingAtVillagerTicks;
   public CrackLevel crackLevel = CrackLevel.NONE;
}
