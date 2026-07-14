package net.caffeinemc.mods.lithium.common.world.chunk.heightmap;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

public final class CombinedHeightmapUpdate {
   private CombinedHeightmapUpdate() {
   }

   public static void updateHeightmaps(
      Heightmap heightmap0, Heightmap heightmap1, Heightmap heightmap2, Heightmap heightmap3,
      WorldChunk chunk, int x, int y, int z, BlockState state
   ) {
      Heightmap[] heightmaps = {heightmap0, heightmap1, heightmap2, heightmap3};
      Predicate<BlockState>[] predicates = new Predicate[heightmaps.length];
      int remaining = 0;

      for (int index = 0; index < heightmaps.length; index++) {
         Heightmap heightmap = heightmaps[index];
         int height = heightmap.get(x, z);
         if (y + 2 <= height) {
            heightmaps[index] = null;
            continue;
         }

         Predicate<BlockState> predicate = Objects.requireNonNull(heightmap.lithium$getBlockPredicate());
         predicates[index] = predicate;
         if (predicate.test(state)) {
            if (y >= height) {
               heightmap.lithium$set(x, z, y + 1);
            }
            heightmaps[index] = null;
         } else if (height != y + 1) {
            heightmaps[index] = null;
         } else {
            remaining++;
         }
      }

      if (remaining == 0) {
         return;
      }

      BlockPos.Mutable pos = new BlockPos.Mutable();
      int bottomY = chunk.getBottomY();
      for (int searchY = y - 1; searchY >= bottomY && remaining > 0; searchY--) {
         BlockState below = chunk.getBlockState(pos.set(x, searchY, z));
         for (int index = 0; index < heightmaps.length; index++) {
            Heightmap heightmap = heightmaps[index];
            if (heightmap != null && predicates[index].test(below)) {
               heightmap.lithium$set(x, z, searchY + 1);
               heightmaps[index] = null;
               remaining--;
            }
         }
      }

      for (Heightmap heightmap : heightmaps) {
         if (heightmap != null) {
            heightmap.lithium$set(x, z, bottomY);
         }
      }
   }
}
