package net.caffeinemc.mods.lithium.common.world.chunk;

import java.util.Collection;
import net.caffeinemc.mods.lithium.common.entity.EntityClassGroup;

public interface ClassGroupFilterableList<T> {
   Collection<T> lithium$getAllOfGroupType(EntityClassGroup group);
}
