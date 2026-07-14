package net.caffeinemc.mods.lithium.common.world;

import net.minecraft.world.entity.EntityLike;

public interface ChunkAwareEntityIterable<T extends EntityLike> {
   Iterable<T> lithium$iterateEntitiesInTrackedSections();
}
