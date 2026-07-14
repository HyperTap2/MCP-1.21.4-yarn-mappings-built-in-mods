package net.minecraft.world.poi;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.registry.entry.RegistryEntry;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.slf4j.Logger;

public class PointOfInterestSet implements PointOfInterestSetExtended {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Short2ObjectMap<PointOfInterest> pointsOfInterestByPos = new Short2ObjectOpenHashMap();
   private final Map<RegistryEntry<PointOfInterestType>, Set<PointOfInterest>> pointsOfInterestByType = new Reference2ReferenceOpenHashMap<>();
   private final Runnable updateListener;
   private boolean valid;

   public PointOfInterestSet(Runnable updateListener) {
      this(updateListener, true, ImmutableList.of());
   }

   PointOfInterestSet(Runnable updateListener, boolean valid, List<PointOfInterest> pois) {
      this.updateListener = updateListener;
      this.valid = valid;
      pois.forEach(this::add);
   }

   public PointOfInterestSet.Serialized toSerialized() {
      return new PointOfInterestSet.Serialized(this.valid, this.pointsOfInterestByPos.values().stream().map(PointOfInterest::toSerialized).toList());
   }

   public Stream<PointOfInterest> get(Predicate<RegistryEntry<PointOfInterestType>> predicate, PointOfInterestStorage.OccupationStatus occupationStatus) {
      return this.pointsOfInterestByType
         .entrySet()
         .stream()
         .filter(entry -> predicate.test(entry.getKey()))
         .flatMap(entry -> entry.getValue().stream())
         .filter(occupationStatus.getPredicate());
   }

   private static <K, V> Iterable<? extends Map.Entry<K, V>> lithium$getFastEntries(Map<K, V> map) {
      return map instanceof Reference2ReferenceMap
         ? Reference2ReferenceMaps.fastIterable((Reference2ReferenceMap<K, V>)map)
         : map.entrySet();
   }

   @Override
   public void lithium$collectMatchingPoints(
      Predicate<RegistryEntry<PointOfInterestType>> type,
      PointOfInterestStorage.OccupationStatus status,
      Consumer<PointOfInterest> consumer
   ) {
      if (type instanceof SinglePointOfInterestTypeFilter filter) {
         this.lithium$collectSingleType(filter.getType(), status, consumer);
         return;
      }
      for (Map.Entry<RegistryEntry<PointOfInterestType>, Set<PointOfInterest>> entry : lithium$getFastEntries(this.pointsOfInterestByType)) {
         if (type.test(entry.getKey()) && !entry.getValue().isEmpty()) {
            for (PointOfInterest poi : entry.getValue()) {
               if (status.getPredicate().test(poi)) {
                  consumer.accept(poi);
               }
            }
         }
      }
   }

   private void lithium$collectSingleType(
      RegistryEntry<PointOfInterestType> type, PointOfInterestStorage.OccupationStatus status, Consumer<PointOfInterest> consumer
   ) {
      Set<PointOfInterest> points = this.pointsOfInterestByType.get(type);
      if (points != null && !points.isEmpty()) {
         for (PointOfInterest poi : points) {
            if (status.getPredicate().test(poi)) {
               consumer.accept(poi);
            }
         }
      }
   }

   public void add(BlockPos pos, RegistryEntry<PointOfInterestType> type) {
      if (this.add(new PointOfInterest(pos, type, this.updateListener))) {
         LOGGER.debug("Added POI of type {} @ {}", type.getIdAsString(), pos);
         this.updateListener.run();
      }
   }

   private boolean add(PointOfInterest poi) {
      BlockPos blockPos = poi.getPos();
      RegistryEntry<PointOfInterestType> registryEntry = poi.getType();
      short s = ChunkSectionPos.packLocal(blockPos);
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.get(s);
      if (pointOfInterest != null) {
         if (registryEntry.equals(pointOfInterest.getType())) {
            return false;
         }

         Util.logErrorOrPause("POI data mismatch: already registered at " + blockPos);
      }

      this.pointsOfInterestByPos.put(s, poi);
      this.pointsOfInterestByType.computeIfAbsent(registryEntry, type -> new ObjectOpenHashSet<>()).add(poi);
      return true;
   }

   public void remove(BlockPos pos) {
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.remove(ChunkSectionPos.packLocal(pos));
      if (pointOfInterest == null) {
         LOGGER.error("POI data mismatch: never registered at {}", pos);
      } else {
         this.pointsOfInterestByType.get(pointOfInterest.getType()).remove(pointOfInterest);
         LOGGER.debug("Removed POI of type {} @ {}", LogUtils.defer(pointOfInterest::getType), LogUtils.defer(pointOfInterest::getPos));
         this.updateListener.run();
      }
   }

   @Deprecated
   @Debug
   public int getFreeTickets(BlockPos pos) {
      return this.get(pos).map(PointOfInterest::getFreeTickets).orElse(0);
   }

   public boolean releaseTicket(BlockPos pos) {
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos));
      if (pointOfInterest == null) {
         throw (IllegalStateException)Util.getFatalOrPause(new IllegalStateException("POI never registered at " + pos));
      }

      boolean bl = pointOfInterest.releaseTicket();
      this.updateListener.run();
      return bl;
   }

   public boolean test(BlockPos pos, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
      return this.getType(pos).filter(predicate).isPresent();
   }

   public Optional<RegistryEntry<PointOfInterestType>> getType(BlockPos pos) {
      return this.get(pos).map(PointOfInterest::getType);
   }

   private Optional<PointOfInterest> get(BlockPos pos) {
      return Optional.ofNullable((PointOfInterest)this.pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos)));
   }

   public void updatePointsOfInterest(Consumer<BiConsumer<BlockPos, RegistryEntry<PointOfInterestType>>> updater) {
      if (!this.valid) {
         Short2ObjectMap<PointOfInterest> short2ObjectMap = new Short2ObjectOpenHashMap(this.pointsOfInterestByPos);
         this.clear();
         updater.accept(
            (pos, poiEntry) -> {
               short s = ChunkSectionPos.packLocal(pos);
               PointOfInterest pointOfInterest = (PointOfInterest)short2ObjectMap.computeIfAbsent(
                  s, sx -> new PointOfInterest(pos, poiEntry, this.updateListener)
               );
               this.add(pointOfInterest);
            }
         );
         this.valid = true;
         this.updateListener.run();
      }
   }

   private void clear() {
      this.pointsOfInterestByPos.clear();
      this.pointsOfInterestByType.clear();
   }

   boolean isValid() {
      return this.valid;
   }

   public record Serialized(boolean isValid, List<PointOfInterest.Serialized> records) {
      public static final Codec<PointOfInterestSet.Serialized> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.BOOL.lenientOptionalFieldOf("Valid", false).forGetter(PointOfInterestSet.Serialized::isValid),
               PointOfInterest.Serialized.CODEC.listOf().fieldOf("Records").forGetter(PointOfInterestSet.Serialized::records)
            )
            .apply(instance, PointOfInterestSet.Serialized::new)
      );

      public PointOfInterestSet toPointOfInterestSet(Runnable updateListener) {
         return new PointOfInterestSet(
            updateListener, this.isValid, this.records.stream().map(serialized -> serialized.toPointOfInterest(updateListener)).toList()
         );
      }
   }
}
