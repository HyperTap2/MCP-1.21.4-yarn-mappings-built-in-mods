package net.caffeinemc.mods.lithium.common.initialization;

import net.caffeinemc.mods.lithium.common.ai.pathing.BlockStatePathingCache;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlagHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public final class BlockInfoInitializer {
   private static boolean initialized;
   private static boolean initializing;

   private BlockInfoInitializer() {
   }

   public static synchronized void initializeBlockInfo() {
      if (initialized || initializing) {
         return;
      }

      initializing = true;
      try {
         for (BlockState blockState : Block.STATE_IDS) {
            ((BlockStatePathingCache)blockState).lithium$initializePathNodeTypeCache();
         }

         for (BlockState blockState : Block.STATE_IDS) {
            ((BlockStateFlagHolder)blockState).lithium$initializeFlags();
         }

         initialized = true;
      } finally {
         initializing = false;
      }
   }
}
