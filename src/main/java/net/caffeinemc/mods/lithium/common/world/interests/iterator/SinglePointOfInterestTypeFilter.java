package net.caffeinemc.mods.lithium.common.world.interests.iterator;

import java.util.function.Predicate;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;

public record SinglePointOfInterestTypeFilter(RegistryEntry<PointOfInterestType> type)
   implements Predicate<RegistryEntry<PointOfInterestType>> {
   @Override
   public boolean test(RegistryEntry<PointOfInterestType> other) {
      return this.type == other;
   }

   public RegistryEntry<PointOfInterestType> getType() {
      return this.type;
   }
}
