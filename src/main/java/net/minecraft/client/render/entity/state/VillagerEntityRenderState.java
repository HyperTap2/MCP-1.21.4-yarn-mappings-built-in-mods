package net.minecraft.client.render.entity.state;

import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

public class VillagerEntityRenderState extends ItemHolderEntityRenderState implements VillagerDataRenderState {
   public boolean headRolling;
   public VillagerData villagerData = new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1);

   @Override
   public VillagerData getVillagerData() {
      return this.villagerData;
   }
}
