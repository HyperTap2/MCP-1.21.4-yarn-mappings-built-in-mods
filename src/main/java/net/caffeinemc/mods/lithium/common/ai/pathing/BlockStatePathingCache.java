package net.caffeinemc.mods.lithium.common.ai.pathing;

import net.minecraft.entity.ai.pathing.PathNodeType;

public interface BlockStatePathingCache {
   PathNodeType lithium$getPathNodeType();

   PathNodeType lithium$getNeighborPathNodeType();

   void lithium$initializePathNodeTypeCache();
}
