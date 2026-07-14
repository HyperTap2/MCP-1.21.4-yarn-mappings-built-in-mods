package net.minecraft.world.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.caffeinemc.mods.lithium.common.util.Distances;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestStorageExtended;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.NearbyPointOfInterestStream;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.SphereChunkOrderedPoiSetSpliterator;
import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.SectionDistanceLevelPropagator;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.storage.ChunkPosKeyedStorage;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import net.minecraft.world.storage.StorageKey;

public class PointOfInterestStorage extends SerializingRegionBasedStorage<PointOfInterestSet, PointOfInterestSet.Serialized>
   implements PointOfInterestStorageExtended {
   public static final int field_30265 = 6;
   public static final int field_30266 = 1;
   private final PointOfInterestStorage.PointOfInterestDistanceTracker pointOfInterestDistanceTracker;
   private final LongSet preloadedChunks = new LongOpenHashSet();

   public PointOfInterestStorage(
      StorageKey storageKey,
      Path directory,
      DataFixer dataFixer,
      boolean dsync,
      DynamicRegistryManager registryManager,
      ChunkErrorHandler errorHandler,
      HeightLimitView world
   ) {
      super(
         new ChunkPosKeyedStorage(storageKey, directory, dataFixer, dsync, DataFixTypes.POI_CHUNK),
         PointOfInterestSet.Serialized.CODEC,
         PointOfInterestSet::toSerialized,
         PointOfInterestSet.Serialized::toPointOfInterestSet,
         PointOfInterestSet::new,
         registryManager,
         errorHandler,
         world
      );
      this.pointOfInterestDistanceTracker = new PointOfInterestStorage.PointOfInterestDistanceTracker();
   }

   public void add(BlockPos pos, RegistryEntry<PointOfInterestType> type) {
      this.getOrCreate(ChunkSectionPos.toLong(pos)).add(pos, type);
   }

   public void remove(BlockPos pos) {
      this.get(ChunkSectionPos.toLong(pos)).ifPresent(poiSet -> poiSet.remove(pos));
   }

   public long count(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.lithium$withinSphereChunkSectionSorted(typePredicate, pos, radius, occupationStatus).size();
   }

   public boolean hasTypeAt(RegistryKey<PointOfInterestType> type, BlockPos pos) {
      return this.test(pos, entry -> entry.matchesKey(type));
   }

   public Stream<PointOfInterest> getInSquare(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      int i = Math.floorDiv(radius, 16) + 1;
      return ChunkPos.stream(new ChunkPos(pos), i).flatMap(chunkPos -> this.getInChunk(typePredicate, chunkPos, occupationStatus)).filter(poi -> {
         BlockPos blockPos2 = poi.getPos();
         return Math.abs(blockPos2.getX() - pos.getX()) <= radius && Math.abs(blockPos2.getZ() - pos.getZ()) <= radius;
      });
   }

   public Stream<PointOfInterest> getInCircle(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.lithium$withinSphereChunkSectionSortedStream(typePredicate, pos, radius, occupationStatus);
   }

   @Debug
   public Stream<PointOfInterest> getInChunk(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, ChunkPos chunkPos, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return ((RegionBasedStorageSectionExtended<PointOfInterestSet>)this)
         .lithium$getWithinChunkColumn(chunkPos.x, chunkPos.z)
         .flatMap(set -> set.get(typePredicate, occupationStatus));
   }

   public Stream<BlockPos> getPositions(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getInCircle(typePredicate, pos, radius, occupationStatus).map(PointOfInterest::getPos).filter(posPredicate);
   }

   public Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getTypesAndPositions(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getInCircle(typePredicate, pos, radius, occupationStatus)
         .filter(poi -> posPredicate.test(poi.getPos()))
         .map(poi -> Pair.of(poi.getType(), poi.getPos()));
   }

   public Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getSortedTypesAndPositions(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getTypesAndPositions(typePredicate, posPredicate, pos, radius, occupationStatus)
         .sorted(Comparator.comparingDouble(pair -> ((BlockPos)pair.getSecond()).getSquaredDistance(pos)));
   }

   public Optional<BlockPos> getPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getPositions(typePredicate, posPredicate, pos, radius, occupationStatus).findFirst();
   }

   public Optional<BlockPos> getNearestPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getNearestPosition(typePredicate, null, pos, radius, occupationStatus);
   }

   public Optional<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getNearestTypeAndPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      return this.getInCircle(typePredicate, pos, radius, occupationStatus)
         .min(Comparator.comparingDouble(poi -> poi.getPos().getSquaredDistance(pos)))
         .map(poi -> Pair.of(poi.getType(), poi.getPos()));
   }

   public Optional<BlockPos> getNearestPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus
   ) {
      Predicate<PointOfInterest> afterSort = posPredicate == null ? null : poi -> posPredicate.test(poi.getPos());
      return this.lithium$streamOutwards(pos, radius, occupationStatus, true, false, typePredicate, afterSort)
         .map(PointOfInterest::getPos)
         .findFirst();
   }

   public Optional<BlockPos> getPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      BiPredicate<RegistryEntry<PointOfInterestType>, BlockPos> posPredicate,
      BlockPos pos,
      int radius
   ) {
      return this.getInCircle(typePredicate, pos, radius, PointOfInterestStorage.OccupationStatus.HAS_SPACE)
         .filter(poi -> posPredicate.test(poi.getType(), poi.getPos()))
         .findFirst()
         .map(poi -> {
            poi.reserveTicket();
            return poi.getPos();
         });
   }

   public Optional<BlockPos> getPosition(
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> positionPredicate,
      PointOfInterestStorage.OccupationStatus occupationStatus,
      BlockPos pos,
      int radius,
      Random random
   ) {
      ArrayList<PointOfInterest> points = this.lithium$withinSphereChunkSectionSorted(typePredicate, pos, radius, occupationStatus);
      for (int i = points.size() - 1; i >= 0; i--) {
         PointOfInterest current = points.set(random.nextInt(i + 1), points.get(i));
         points.set(i, current);
         if (positionPredicate.test(current.getPos())) {
            return Optional.of(current.getPos());
         }
      }
      return Optional.empty();
   }

   @Override
   public Optional<PointOfInterest> lithium$findNearestForPortalLogic(
      BlockPos origin,
      int radius,
      RegistryEntry<PointOfInterestType> type,
      PointOfInterestStorage.OccupationStatus status,
      Predicate<PointOfInterest> afterSortPredicate,
      WorldBorder worldBorder
   ) {
      boolean borderIsFarAway = worldBorder == null || worldBorder.getDistanceInsideBorder(origin.getX(), origin.getZ()) > radius + 3;
      Predicate<PointOfInterest> filtered = borderIsFarAway
         ? afterSortPredicate
         : poi -> worldBorder.contains(poi.getPos()) && afterSortPredicate.test(poi);
      return this.lithium$streamOutwards(origin, radius, status, true, true, new SinglePointOfInterestTypeFilter(type), filtered).findFirst();
   }

   private Stream<PointOfInterest> lithium$withinSphereChunkSectionSortedStream(
      Predicate<RegistryEntry<PointOfInterestType>> predicate,
      BlockPos origin,
      int radius,
      PointOfInterestStorage.OccupationStatus status
   ) {
      double radiusSq = radius * radius;
      RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>)this;
      Stream<Stream<PointOfInterestSet>> sections = StreamSupport.stream(new SphereChunkOrderedPoiSetSpliterator(radius, origin, storage), false);
      return sections.flatMap(
         sectionStream -> sectionStream.flatMap(
            set -> set.get(predicate, status).filter(point -> Distances.isWithinCircleRadius(origin, radiusSq, point.getPos()))
         )
      );
   }

   private ArrayList<PointOfInterest> lithium$withinSphereChunkSectionSorted(
      Predicate<RegistryEntry<PointOfInterestType>> predicate,
      BlockPos origin,
      int radius,
      PointOfInterestStorage.OccupationStatus status
   ) {
      double radiusSq = radius * radius;
      int minChunkX = origin.getX() - radius - 1 >> 4;
      int minChunkZ = origin.getZ() - radius - 1 >> 4;
      int maxChunkX = origin.getX() + radius + 1 >> 4;
      int maxChunkZ = origin.getZ() + radius + 1 >> 4;
      RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>)this;
      ArrayList<PointOfInterest> points = new ArrayList<>();
      Consumer<PointOfInterest> collector = point -> {
         if (Distances.isWithinCircleRadius(origin, radiusSq, point.getPos())) {
            points.add(point);
         }
      };
      for (int x = minChunkX; x <= maxChunkX; x++) {
         for (int z = minChunkZ; z <= maxChunkZ; z++) {
            for (PointOfInterestSet set : storage.lithium$getInChunkColumn(x, z)) {
               ((PointOfInterestSetExtended)set).lithium$collectMatchingPoints(predicate, status, collector);
            }
         }
      }
      return points;
   }

   private Stream<PointOfInterest> lithium$streamOutwards(
      BlockPos origin,
      int radius,
      PointOfInterestStorage.OccupationStatus status,
      boolean useSquareDistanceLimit,
      boolean preferNegativeY,
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<PointOfInterest> afterSortingPredicate
   ) {
      RegionBasedStorageSectionExtended<PointOfInterestSet> storage = (RegionBasedStorageSectionExtended<PointOfInterestSet>)this;
      return StreamSupport.stream(
         new NearbyPointOfInterestStream(
            typePredicate, status, useSquareDistanceLimit, preferNegativeY, afterSortingPredicate, origin, radius, storage
         ),
         false
      );
   }

   public boolean releaseTicket(BlockPos pos) {
      return this.get(ChunkSectionPos.toLong(pos))
         .map(poiSet -> poiSet.releaseTicket(pos))
         .orElseThrow(() -> Util.getFatalOrPause(new IllegalStateException("POI never registered at " + pos)));
   }

   public boolean test(BlockPos pos, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
      return this.get(ChunkSectionPos.toLong(pos)).map(poiSet -> poiSet.test(pos, predicate)).orElse(false);
   }

   public Optional<RegistryEntry<PointOfInterestType>> getType(BlockPos pos) {
      return this.get(ChunkSectionPos.toLong(pos)).flatMap(poiSet -> poiSet.getType(pos));
   }

   @Deprecated
   @Debug
   public int getFreeTickets(BlockPos pos) {
      return this.get(ChunkSectionPos.toLong(pos)).map(poiSet -> poiSet.getFreeTickets(pos)).orElse(0);
   }

   public int getDistanceFromNearestOccupied(ChunkSectionPos pos) {
      this.pointOfInterestDistanceTracker.update();
      return this.pointOfInterestDistanceTracker.getLevel(pos.asLong());
   }

   boolean isOccupied(long pos) {
      Optional<PointOfInterestSet> optional = this.getIfLoaded(pos);
      return optional == null
         ? false
         : optional.<Boolean>map(
               poiSet -> poiSet.get(entry -> entry.isIn(PointOfInterestTypeTags.VILLAGE), PointOfInterestStorage.OccupationStatus.IS_OCCUPIED)
                  .findAny()
                  .isPresent()
            )
            .orElse(false);
   }

   @Override
   public void tick(BooleanSupplier shouldKeepTicking) {
      super.tick(shouldKeepTicking);
      this.pointOfInterestDistanceTracker.update();
   }

   @Override
   protected void onUpdate(long pos) {
      super.onUpdate(pos);
      this.pointOfInterestDistanceTracker.update(pos, this.pointOfInterestDistanceTracker.getInitialLevel(pos), false);
   }

   @Override
   protected void onLoad(long pos) {
      this.pointOfInterestDistanceTracker.update(pos, this.pointOfInterestDistanceTracker.getInitialLevel(pos), false);
   }

   public void initForPalette(ChunkSectionPos sectionPos, ChunkSection chunkSection) {
      Util.ifPresentOrElse(this.get(sectionPos.asLong()), poiSet -> poiSet.updatePointsOfInterest(populator -> {
         if (shouldScan(chunkSection)) {
            this.scanAndPopulate(chunkSection, sectionPos, populator);
         }
      }), () -> {
         if (shouldScan(chunkSection)) {
            PointOfInterestSet pointOfInterestSet = this.getOrCreate(sectionPos.asLong());
            this.scanAndPopulate(chunkSection, sectionPos, pointOfInterestSet::add);
         }
      });
   }

   private static boolean shouldScan(ChunkSection chunkSection) {
      return chunkSection.hasAny(PointOfInterestTypes::isPointOfInterest);
   }

   private void scanAndPopulate(ChunkSection chunkSection, ChunkSectionPos sectionPos, BiConsumer<BlockPos, RegistryEntry<PointOfInterestType>> populator) {
      sectionPos.streamBlocks()
         .forEach(
            pos -> {
               BlockState blockState = chunkSection.getBlockState(
                  ChunkSectionPos.getLocalCoord(pos.getX()), ChunkSectionPos.getLocalCoord(pos.getY()), ChunkSectionPos.getLocalCoord(pos.getZ())
               );
               PointOfInterestTypes.getTypeForState(blockState).ifPresent(poiType -> populator.accept(pos, (RegistryEntry<PointOfInterestType>)poiType));
            }
         );
   }

   public void preloadChunks(WorldView world, BlockPos pos, int radius) {
      if (this.lithium$preloadRadius != radius) {
         this.lithium$preloadedCenterChunks.clear();
         this.lithium$preloadRadius = radius;
      }
      long centerChunk = ChunkPos.toLong(pos);
      if (this.lithium$preloadedCenterChunks.contains(centerChunk)) {
         return;
      }
      int centerX = ChunkSectionPos.getSectionCoord(pos.getX());
      int centerZ = ChunkSectionPos.getSectionCoord(pos.getZ());
      int chunkRadius = Math.floorDiv(radius, 16);
      int maxSection = this.world.getTopSectionCoord() - 1;
      int minSection = this.world.getBottomSectionCoord();
      for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
         for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
            this.lithium$preloadChunkIfAnySectionContainsPoi(world, x, z, minSection, maxSection);
         }
      }
      this.lithium$preloadedCenterChunks.add(centerChunk);
   }

   private final LongSet lithium$preloadedCenterChunks = new LongOpenHashSet();
   private int lithium$preloadRadius;

   private void lithium$preloadChunkIfAnySectionContainsPoi(WorldView worldView, int x, int z, int minSection, int maxSection) {
      long packed = ChunkPos.toLong(x, z);
      if (this.preloadedChunks.contains(packed)) {
         return;
      }
      for (int y = minSection; y <= maxSection; y++) {
         Optional<PointOfInterestSet> section = this.get(ChunkSectionPos.asLong(x, y, z));
         if (section.isPresent() && section.get().isValid()) {
            if (this.preloadedChunks.add(packed)) {
               worldView.getChunk(x, z, ChunkStatus.EMPTY);
            }
            break;
         }
      }
   }

   public enum OccupationStatus {
      HAS_SPACE(PointOfInterest::hasSpace),
      IS_OCCUPIED(PointOfInterest::isOccupied),
      ANY(poi -> true);

      private final Predicate<? super PointOfInterest> predicate;

      OccupationStatus(final Predicate<? super PointOfInterest> predicate) {
         this.predicate = predicate;
      }

      public Predicate<? super PointOfInterest> getPredicate() {
         return this.predicate;
      }
   }

   final class PointOfInterestDistanceTracker extends SectionDistanceLevelPropagator {
      private final Long2ByteMap distances = new Long2ByteOpenHashMap();

      protected PointOfInterestDistanceTracker() {
         super(7, 16, 256);
         this.distances.defaultReturnValue((byte)7);
      }

      @Override
      protected int getInitialLevel(long id) {
         return PointOfInterestStorage.this.isOccupied(id) ? 0 : 7;
      }

      @Override
      protected int getLevel(long id) {
         return this.distances.get(id);
      }

      @Override
      protected void setLevel(long id, int level) {
         if (level > 6) {
            this.distances.remove(id);
         } else {
            this.distances.put(id, (byte)level);
         }
      }

      public void update() {
         super.applyPendingUpdates(Integer.MAX_VALUE);
      }
   }
}
