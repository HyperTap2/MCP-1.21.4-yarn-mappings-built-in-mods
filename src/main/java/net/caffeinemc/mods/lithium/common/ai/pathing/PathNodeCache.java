package net.caffeinemc.mods.lithium.common.ai.pathing;

import net.caffeinemc.mods.lithium.common.block.BlockCountingSection;
import net.caffeinemc.mods.lithium.common.block.BlockStateFlags;
import net.caffeinemc.mods.lithium.common.initialization.BlockInfoInitializer;
import net.caffeinemc.mods.lithium.common.world.ChunkView;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public final class PathNodeCache {
   private PathNodeCache() {
   }

   public static PathNodeType getPathNodeType(BlockState state) {
      BlockStatePathingCache cache = (BlockStatePathingCache)state;
      PathNodeType type = cache.lithium$getPathNodeType();
      if (type == null) {
         BlockInfoInitializer.initializeBlockInfo();
         type = cache.lithium$getPathNodeType();
      }
      return type;
   }

   public static PathNodeType getNeighborPathNodeType(AbstractBlock.AbstractBlockState state) {
      BlockStatePathingCache cache = (BlockStatePathingCache)state;
      PathNodeType type = cache.lithium$getNeighborPathNodeType();
      if (type == null) {
         BlockInfoInitializer.initializeBlockInfo();
         type = cache.lithium$getNeighborPathNodeType();
      }
      return type;
   }

   public static PathNodeType getNeighborPathNodeType(PathNodeType type) {
      return switch (type) {
         case DAMAGE_OTHER -> PathNodeType.DANGER_OTHER;
         case DAMAGE_FIRE, LAVA -> PathNodeType.DANGER_FIRE;
         case WATER -> PathNodeType.WATER_BORDER;
         case DAMAGE_CAUTIOUS -> PathNodeType.DAMAGE_CAUTIOUS;
         default -> PathNodeType.OPEN;
      };
   }

   private static boolean isSectionSafeAsNeighbor(ChunkSection section) {
      if (section.isEmpty()) {
         return true;
      }
      return !((BlockCountingSection)section).lithium$mayContainAny(BlockStateFlags.PATH_NOT_OPEN);
   }

   public static PathNodeType getNodeTypeFromNeighbors(PathContext context, int x, int y, int z, PathNodeType fallback) {
      BlockView world = context.getWorld();
      ChunkSection section = null;
      if (world instanceof ChunkView chunkView && neighborsWithinSameSection(x, y, z) && !world.isOutOfHeightLimit(y)) {
         Chunk chunk = chunkView.lithium$getLoadedChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
         if (chunk != null) {
            section = chunk.getSection(world.getSectionIndex(y));
         }
         if (section == null || isSectionSafeAsNeighbor(section)) {
            return fallback;
         }
      }

      BlockPos.Mutable pos = context.lithium$getLastNodePos();
      for (int adjacentX = x - 1; adjacentX <= x + 1; adjacentX++) {
         for (int adjacentY = y - 1; adjacentY <= y + 1; adjacentY++) {
            for (int adjacentZ = z - 1; adjacentZ <= z + 1; adjacentZ++) {
               if (adjacentX == x && adjacentZ == z) {
                  continue;
               }

               BlockState state = section != null
                  ? section.getBlockState(adjacentX & 15, adjacentY & 15, adjacentZ & 15)
                  : world.getBlockState(pos.set(adjacentX, adjacentY, adjacentZ));
               if (!state.isAir()) {
                  PathNodeType neighborType = getNeighborPathNodeType(state);
                  if (neighborType == null) {
                     neighborType = getNeighborPathNodeType(context.getNodeType(adjacentX, adjacentY, adjacentZ));
                  }
                  if (neighborType != PathNodeType.OPEN) {
                     return neighborType;
                  }
               }
            }
         }
      }
      return fallback;
   }

   private static boolean neighborsWithinSameSection(int x, int y, int z) {
      int localX = x & 15;
      int localY = y & 15;
      int localZ = z & 15;
      return localX != 0 && localX != 15 && localY != 0 && localY != 15 && localZ != 0 && localZ != 15;
   }
}
