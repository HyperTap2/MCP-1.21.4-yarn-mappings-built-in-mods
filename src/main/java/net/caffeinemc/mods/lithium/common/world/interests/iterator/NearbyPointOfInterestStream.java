package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.util.tuples.SortedPointOfInterest;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

public class NearbyPointOfInterestStream extends AbstractSpliterator<PointOfInterest> {
   private final RegionBasedStorageSectionExtended<PointOfInterestSet> storage;
   private final Predicate<RegistryEntry<PointOfInterestType>> typeSelector;
   private final PointOfInterestStorage.OccupationStatus occupationStatus;
   private final LongArrayList chunksSortedByMinDistance;
   private final ArrayList<SortedPointOfInterest> points = new ArrayList<>();
   @Nullable
   private final Predicate<PointOfInterest> afterSortingPredicate;
   private final Consumer<PointOfInterest> collector;
   private final BlockPos origin;
   private final Comparator<? super SortedPointOfInterest> pointComparator;
   private int chunkIndex;
   private double currentChunkMinDistanceSq;
   private int pointIndex;

   public NearbyPointOfInterestStream(
      Predicate<RegistryEntry<PointOfInterestType>> typeSelector,
      PointOfInterestStorage.OccupationStatus status,
      boolean useSquareDistanceLimit,
      boolean preferNegativeY,
      @Nullable Predicate<PointOfInterest> afterSortingPredicate,
      BlockPos origin,
      int radius,
      RegionBasedStorageSectionExtended<PointOfInterestSet> storage
   ) {
      super(Long.MAX_VALUE, 16);
      this.storage = storage;
      this.occupationStatus = status;
      this.typeSelector = typeSelector;
      this.origin = origin;
      if (useSquareDistanceLimit) {
         this.collector = point -> {
            if (Distances.isWithinSquareRadius(this.origin, radius, point.getPos())) {
               this.points.add(new SortedPointOfInterest(point, this.origin));
            }
         };
      } else {
         double radiusSq = radius * radius;
         this.collector = point -> {
            if (Distances.isWithinCircleRadius(this.origin, radiusSq, point.getPos())) {
               this.points.add(new SortedPointOfInterest(point, this.origin));
            }
         };
      }
      double distanceLimitSq = useSquareDistanceLimit ? radius * radius * 2.0 : radius * radius;
      this.chunksSortedByMinDistance = initChunkPositions(origin, radius, distanceLimitSq);
      this.afterSortingPredicate = afterSortingPredicate;
      this.pointComparator = preferNegativeY
         ? Comparator.comparingDouble(SortedPointOfInterest::distanceSq)
            .thenComparingInt(SortedPointOfInterest::getY)
            .thenComparingInt(point -> ChunkSectionPos.getSectionCoord(point.getX()))
            .thenComparingInt(point -> ChunkSectionPos.getSectionCoord(point.getZ()))
         : Comparator.comparingDouble(SortedPointOfInterest::distanceSq)
            .thenComparingInt(point -> ChunkSectionPos.getSectionCoord(point.getX()))
            .thenComparingInt(point -> ChunkSectionPos.getSectionCoord(point.getZ()))
            .thenComparingInt(point -> ChunkSectionPos.getSectionCoord(point.getY()));
   }

   private static LongArrayList initChunkPositions(BlockPos origin, int radius, double distanceLimitSq) {
      int minChunkX = origin.getX() - radius - 1 >> 4;
      int minChunkZ = origin.getZ() - radius - 1 >> 4;
      int maxChunkX = origin.getX() + radius + 1 >> 4;
      int maxChunkZ = origin.getZ() + radius + 1 >> 4;
      LongArrayList positions = new LongArrayList();
      for (int x = minChunkX; x <= maxChunkX; x++) {
         for (int z = minChunkZ; z <= maxChunkZ; z++) {
            if (distanceLimitSq >= Distances.getMinChunkToBlockDistanceL2Sq(origin, x, z)) {
               positions.add(ChunkPos.toLong(x, z));
            }
         }
      }
      positions.sort(
         (first, second) -> Double.compare(
            Distances.getMinChunkToBlockDistanceL2Sq(origin, ChunkPos.getPackedX(first), ChunkPos.getPackedZ(first)),
            Distances.getMinChunkToBlockDistanceL2Sq(origin, ChunkPos.getPackedX(second), ChunkPos.getPackedZ(second))
         )
      );
      return positions;
   }

   @Override
   public boolean tryAdvance(Consumer<? super PointOfInterest> action) {
      if (this.pointIndex < this.points.size() && this.tryAdvancePoint(action)) {
         return true;
      }
      while (this.chunkIndex < this.chunksSortedByMinDistance.size()) {
         long packed = this.chunksSortedByMinDistance.getLong(this.chunkIndex);
         int chunkX = ChunkPos.getPackedX(packed);
         int chunkZ = ChunkPos.getPackedZ(packed);
         this.currentChunkMinDistanceSq = Distances.getMinChunkToBlockDistanceL2Sq(this.origin, chunkX, chunkZ);
         this.chunkIndex++;
         if (this.chunkIndex == this.chunksSortedByMinDistance.size()) {
            this.currentChunkMinDistanceSq = Double.POSITIVE_INFINITY;
         }
         int previousSize = this.points.size();
         for (PointOfInterestSet set : this.storage.lithium$getInChunkColumn(chunkX, chunkZ)) {
            ((PointOfInterestSetExtended)set).lithium$collectMatchingPoints(this.typeSelector, this.occupationStatus, this.collector);
         }
         if (this.points.size() != previousSize) {
            this.points.subList(this.pointIndex, this.points.size()).sort(this.pointComparator);
            if (this.tryAdvancePoint(action)) {
               return true;
            }
         }
      }
      return this.tryAdvancePoint(action);
   }

   private boolean tryAdvancePoint(Consumer<? super PointOfInterest> action) {
      while (this.pointIndex < this.points.size()) {
         SortedPointOfInterest next = this.points.get(this.pointIndex);
         if (next.distanceSq() >= this.currentChunkMinDistanceSq) {
            return false;
         }
         this.pointIndex++;
         if (this.afterSortingPredicate == null || this.afterSortingPredicate.test(next.poi())) {
            action.accept(next.poi());
            return true;
         }
      }
      return false;
   }
}
