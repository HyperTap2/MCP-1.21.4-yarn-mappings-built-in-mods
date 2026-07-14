package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestSet;

public class SphereChunkOrderedPoiSetSpliterator extends AbstractSpliterator<Stream<PointOfInterestSet>> {
   private final int limit;
   private final int minChunkZ;
   private final BlockPos origin;
   private final double radiusSq;
   private final RegionBasedStorageSectionExtended<PointOfInterestSet> storage;
   private final int maxChunkZ;
   private int chunkX;
   private int chunkZ;
   private int iterated;

   public SphereChunkOrderedPoiSetSpliterator(int radius, BlockPos origin, RegionBasedStorageSectionExtended<PointOfInterestSet> storage) {
      super(
         (long)((origin.getX() + radius + 1 >> 4) - (origin.getX() - radius - 1 >> 4) + 1)
            * ((origin.getZ() + radius + 1 >> 4) - (origin.getZ() - radius - 1 >> 4) + 1),
         16
      );
      this.origin = origin;
      this.radiusSq = radius * radius;
      this.storage = storage;
      int minChunkX = origin.getX() - radius - 1 >> 4;
      int maxChunkX = origin.getX() + radius + 1 >> 4;
      this.minChunkZ = origin.getZ() - radius - 1 >> 4;
      this.maxChunkZ = origin.getZ() + radius + 1 >> 4;
      this.limit = (maxChunkX - minChunkX + 1) * (this.maxChunkZ - this.minChunkZ + 1);
      this.chunkX = minChunkX;
      this.chunkZ = this.minChunkZ;
   }

   @Override
   public boolean tryAdvance(Consumer<? super Stream<PointOfInterestSet>> action) {
      while (this.iterated < this.limit) {
         this.iterated++;
         boolean accepted = false;
         if (Distances.getMinChunkToBlockDistanceL2Sq(this.origin, this.chunkX, this.chunkZ) <= this.radiusSq) {
            action.accept(this.storage.lithium$getWithinChunkColumn(this.chunkX, this.chunkZ));
            accepted = true;
         }
         this.chunkZ++;
         if (this.chunkZ > this.maxChunkZ) {
            this.chunkX++;
            this.chunkZ = this.minChunkZ;
         }
         if (accepted) {
            return true;
         }
      }
      return false;
   }
}
