package net.caffeinemc.mods.lithium.common.hopper;

public final class HopperCachingState {
   private HopperCachingState() {
   }

   public enum BlockInventory {
      UNKNOWN,
      BLOCK_STATE,
      BLOCK_ENTITY,
      REMOVAL_TRACKING_BLOCK_ENTITY,
      NO_BLOCK_INVENTORY
   }
}
