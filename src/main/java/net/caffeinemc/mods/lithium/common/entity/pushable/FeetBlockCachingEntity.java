package net.caffeinemc.mods.lithium.common.entity.pushable;

import net.minecraft.block.BlockState;
import org.jetbrains.annotations.Nullable;

public interface FeetBlockCachingEntity {
   default void lithium$onFeetBlockCacheDeleted() {
   }

   default void lithium$onFeetBlockCacheSet(BlockState newState) {
   }

   default void lithium$setClimbingMobCachingSectionUpdateBehavior(boolean listening) {
      throw new UnsupportedOperationException();
   }

   @Nullable
   BlockState lithium$getCachedFeetBlockState();
}
