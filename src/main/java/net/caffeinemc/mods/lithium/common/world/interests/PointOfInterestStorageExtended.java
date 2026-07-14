package net.caffeinemc.mods.lithium.common.world.interests;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

public interface PointOfInterestStorageExtended {
   Optional<PointOfInterest> lithium$findNearestForPortalLogic(
      BlockPos origin,
      int radius,
      RegistryEntry<PointOfInterestType> type,
      PointOfInterestStorage.OccupationStatus status,
      Predicate<PointOfInterest> afterSortPredicate,
      WorldBorder worldBorder
   );
}
