package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.TropicalFishEntity.Variety;

public class TropicalFishEntityRenderState extends LivingEntityRenderState {
   public Variety variety = Variety.FLOPPER;
   public int baseColor = -1;
   public int patternColor = -1;
}
