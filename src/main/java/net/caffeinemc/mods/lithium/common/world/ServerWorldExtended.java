package net.caffeinemc.mods.lithium.common.world;

import net.minecraft.entity.mob.MobEntity;

public interface ServerWorldExtended {
   void lithium$setNavigationActive(MobEntity mob);

   void lithium$setNavigationInactive(MobEntity mob);
}
