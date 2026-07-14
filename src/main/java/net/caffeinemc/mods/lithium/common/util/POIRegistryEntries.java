package net.caffeinemc.mods.lithium.common.util;

import net.minecraft.block.BedBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;

public final class POIRegistryEntries {
   public static final RegistryEntry<PointOfInterestType> NETHER_PORTAL_ENTRY = PointOfInterestTypes.getTypeForState(
      Blocks.NETHER_PORTAL.getDefaultState()
   ).orElseThrow(() -> new IllegalStateException("Nether portal poi type not found"));
   public static final RegistryEntry<PointOfInterestType> HOME_ENTRY = PointOfInterestTypes.getTypeForState(
      Blocks.RED_BED.getDefaultState().with(BedBlock.PART, BedPart.HEAD)
   ).orElseThrow(() -> new IllegalStateException("Home poi type not found"));

   private POIRegistryEntries() {
   }
}
