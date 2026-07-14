package net.caffeinemc.mods.lithium.common.world.interests;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

public interface PointOfInterestSetExtended {
   void lithium$collectMatchingPoints(
      Predicate<RegistryEntry<PointOfInterestType>> type,
      PointOfInterestStorage.OccupationStatus status,
      Consumer<PointOfInterest> consumer
   );
}
