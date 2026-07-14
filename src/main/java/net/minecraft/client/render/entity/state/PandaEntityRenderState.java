package net.minecraft.client.render.entity.state;

import net.minecraft.entity.passive.PandaEntity.Gene;

public class PandaEntityRenderState extends ItemHolderEntityRenderState {
   public Gene gene = Gene.NORMAL;
   public boolean askingForBamboo;
   public boolean sneezing;
   public int sneezeProgress;
   public boolean eating;
   public boolean scaredByThunderstorm;
   public boolean sitting;
   public float sittingAnimationProgress;
   public float lieOnBackAnimationProgress;
   public float rollOverAnimationProgress;
   public float playingTicks;
}
