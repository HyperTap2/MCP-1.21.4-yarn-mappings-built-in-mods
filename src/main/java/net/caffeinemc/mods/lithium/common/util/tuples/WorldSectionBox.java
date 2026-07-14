package net.caffeinemc.mods.lithium.common.util.tuples;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public record WorldSectionBox(World world, int chunkX1, int chunkY1, int chunkZ1, int chunkX2, int chunkY2, int chunkZ2) {
   public static WorldSectionBox entityAccessBox(World world, Box box) {
      int minX = ChunkSectionPos.getSectionCoord(box.minX - 2.0);
      int minY = ChunkSectionPos.getSectionCoord(box.minY - 4.0);
      int minZ = ChunkSectionPos.getSectionCoord(box.minZ - 2.0);
      int maxX = ChunkSectionPos.getSectionCoord(box.maxX + 2.0) + 1;
      int maxY = ChunkSectionPos.getSectionCoord(box.maxY) + 1;
      int maxZ = ChunkSectionPos.getSectionCoord(box.maxZ + 2.0) + 1;
      return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   public static WorldSectionBox relevantExpandedBlocksBox(World world, Box box) {
      int minX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minX) - 1);
      int minY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minY) - 1);
      int minZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minZ) - 1);
      int maxX = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxX) + 1) + 1;
      int maxY = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxY) + 1) + 1;
      int maxZ = ChunkSectionPos.getSectionCoord(MathHelper.floor(box.maxZ) + 1) + 1;
      return new WorldSectionBox(world, minX, minY, minZ, maxX, maxY, maxZ);
   }

   public boolean matchesRelevantBlocksBox(Box box) {
      return ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minX) - 1) == this.chunkX1
         && ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minY) - 1) == this.chunkY1
         && ChunkSectionPos.getSectionCoord(MathHelper.floor(box.minZ) - 1) == this.chunkZ1
         && ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxX) + 1) + 1 == this.chunkX2
         && ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxY) + 1) + 1 == this.chunkY2
         && ChunkSectionPos.getSectionCoord(MathHelper.ceil(box.maxZ) + 1) + 1 == this.chunkZ2;
   }

   public int numSections() {
      return (this.chunkX2 - this.chunkX1) * (this.chunkY2 - this.chunkY1) * (this.chunkZ2 - this.chunkZ1);
   }
}
