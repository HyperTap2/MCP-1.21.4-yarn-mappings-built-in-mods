package net.caffeinemc.mods.lithium.common.world.block_pattern_matching;

import java.util.function.Predicate;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public final class BlockSearch {
   private BlockSearch() {
   }

   public static boolean hasAtLeast(WorldView world, BlockBox box, Block requiredBlock, int requiredCount) {
      Predicate<BlockState> predicate = state -> state.isOf(requiredBlock);
      for (int chunkX = ChunkSectionPos.getSectionCoord(box.getMinX()); chunkX <= ChunkSectionPos.getSectionCoord(box.getMaxX()); chunkX++) {
         for (int chunkZ = ChunkSectionPos.getSectionCoord(box.getMinZ()); chunkZ <= ChunkSectionPos.getSectionCoord(box.getMaxZ()); chunkZ++) {
            Chunk chunk = world.getChunk(chunkX, chunkZ);
            int minSectionIndex = Pos.SectionYIndex.fromBlockCoord(world, box.getMinY());
            int maxSectionIndex = Pos.SectionYIndex.fromBlockCoord(world, box.getMaxY());
            for (int sectionIndex = minSectionIndex; sectionIndex <= maxSectionIndex; sectionIndex++) {
               if (sectionIndex >= 0 && sectionIndex < chunk.getSectionArray().length) {
                  ChunkSection section = chunk.getSection(sectionIndex);
                  if (section.hasAny(predicate)) {
                     int sectionY = Pos.SectionYCoord.fromSectionIndex(world, sectionIndex);
                     requiredCount -= countBlocksInBoxInSection(
                        section,
                        Math.max(box.getMinX(), chunkX << 4),
                        Math.max(box.getMinY(), sectionY << 4),
                        Math.max(box.getMinZ(), chunkZ << 4),
                        Math.min(box.getMaxX(), (chunkX << 4) + 15),
                        Math.min(box.getMaxY(), (sectionY << 4) + 15),
                        Math.min(box.getMaxZ(), (chunkZ << 4) + 15),
                        requiredBlock,
                        requiredCount
                     );
                     if (requiredCount <= 0) {
                        return true;
                     }
                  }
               } else if (requiredBlock == Blocks.VOID_AIR) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public static int countBlocksInBoxInSection(
      ChunkSection section, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Block requiredBlock, int findMax
   ) {
      int found = 0;
      for (int y = minY; y <= maxY; y++) {
         for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
               if (section.getBlockState(x & 15, y & 15, z & 15).isOf(requiredBlock) && ++found >= findMax) {
                  return found;
               }
            }
         }
      }
      return found;
   }
}
