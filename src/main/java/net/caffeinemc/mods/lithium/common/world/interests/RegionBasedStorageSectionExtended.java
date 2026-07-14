package net.caffeinemc.mods.lithium.common.world.interests;

import java.util.stream.Stream;

public interface RegionBasedStorageSectionExtended<R> {
   Stream<R> lithium$getWithinChunkColumn(int chunkX, int chunkZ);

   Iterable<R> lithium$getInChunkColumn(int chunkX, int chunkZ);
}
