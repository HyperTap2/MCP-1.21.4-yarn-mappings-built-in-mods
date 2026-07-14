package net.minecraft.client.render.entity.state;

import net.minecraft.entity.mob.PiglinActivity;

public class PiglinEntityRenderState extends BipedEntityRenderState {
   public boolean brute;
   public boolean shouldZombify;
   public float piglinCrossbowPullTime;
   public PiglinActivity activity = PiglinActivity.DEFAULT;
}
