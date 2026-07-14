package net.minecraft.world.chunk;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;
import net.caffeinemc.mods.lithium.common.world.ChunkView;

public class ChunkCache implements CollisionView, ChunkView {
   protected final int minX;
   protected final int minZ;
   protected final Chunk[][] chunks;
   private final Chunk[] lithium$chunksFlat;
   private final int lithium$xLength;
   private final int lithium$zLength;
   private final int lithium$bottomY;
   private final int lithium$topY;
   protected boolean empty;
   protected final World world;
   private final Supplier<RegistryEntry<Biome>> plainsEntryGetter;

   public ChunkCache(World world, BlockPos minPos, BlockPos maxPos) {
      this.world = world;
      this.plainsEntryGetter = Suppliers.memoize(() -> world.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS));
      this.minX = ChunkSectionPos.getSectionCoord(minPos.getX());
      this.minZ = ChunkSectionPos.getSectionCoord(minPos.getZ());
      int i = ChunkSectionPos.getSectionCoord(maxPos.getX());
      int j = ChunkSectionPos.getSectionCoord(maxPos.getZ());
      this.chunks = new Chunk[i - this.minX + 1][j - this.minZ + 1];
      ChunkManager chunkManager = world.getChunkManager();
      this.empty = true;

      for (int k = this.minX; k <= i; k++) {
         for (int l = this.minZ; l <= j; l++) {
            this.chunks[k - this.minX][l - this.minZ] = chunkManager.getWorldChunk(k, l);
         }
      }

      this.lithium$xLength = this.chunks.length;
      this.lithium$zLength = this.chunks[0].length;
      this.lithium$chunksFlat = new Chunk[this.lithium$xLength * this.lithium$zLength];
      for (int x = 0; x < this.lithium$xLength; x++) {
         System.arraycopy(this.chunks[x], 0, this.lithium$chunksFlat, x * this.lithium$zLength, this.lithium$zLength);
      }
      this.lithium$bottomY = this.getBottomY();
      this.lithium$topY = this.getTopYInclusive();

      for (int k = ChunkSectionPos.getSectionCoord(minPos.getX()); k <= ChunkSectionPos.getSectionCoord(maxPos.getX()); k++) {
         for (int l = ChunkSectionPos.getSectionCoord(minPos.getZ()); l <= ChunkSectionPos.getSectionCoord(maxPos.getZ()); l++) {
            Chunk chunk = this.chunks[k - this.minX][l - this.minZ];
            if (chunk != null && !chunk.areSectionsEmptyBetween(minPos.getY(), maxPos.getY())) {
               this.empty = false;
               return;
            }
         }
      }
   }

   private Chunk getChunk(BlockPos pos) {
      return this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
   }

   private Chunk getChunk(int chunkX, int chunkZ) {
      int i = chunkX - this.minX;
      int j = chunkZ - this.minZ;
      if (i >= 0 && i < this.chunks.length && j >= 0 && j < this.chunks[i].length) {
         Chunk chunk = this.chunks[i][j];
         return chunk != null ? chunk : new EmptyChunk(this.world, new ChunkPos(chunkX, chunkZ), this.plainsEntryGetter.get());
      } else {
         return new EmptyChunk(this.world, new ChunkPos(chunkX, chunkZ), this.plainsEntryGetter.get());
      }
   }

   @Nullable
   @Override
   public Chunk lithium$getLoadedChunk(int chunkX, int chunkZ) {
      int x = chunkX - this.minX;
      int z = chunkZ - this.minZ;
      return x >= 0 && x < this.chunks.length && z >= 0 && z < this.chunks[x].length ? this.chunks[x][z] : null;
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.world.getWorldBorder();
   }

   @Override
   public BlockView getChunkAsView(int chunkX, int chunkZ) {
      return this.getChunk(chunkX, chunkZ);
   }

   @Override
   public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box) {
      return List.of();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      Chunk chunk = this.getChunk(pos);
      return chunk.getBlockEntity(pos);
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      int y = pos.getY();
      if (y >= this.lithium$bottomY && y <= this.lithium$topY) {
         int chunkX = ChunkSectionPos.getSectionCoord(pos.getX()) - this.minX;
         int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ()) - this.minZ;
         if (chunkX >= 0 && chunkX < this.lithium$xLength && chunkZ >= 0 && chunkZ < this.lithium$zLength) {
            Chunk chunk = this.lithium$chunksFlat[chunkX * this.lithium$zLength + chunkZ];
            if (chunk != null) {
               ChunkSection section = chunk.getSection(this.getSectionIndex(y));
               if (section != null) {
                  return section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
               }
            }
         }
      }
      return Blocks.AIR.getDefaultState();
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   @Override
   public int getBottomY() {
      return this.world.getBottomY();
   }

   @Override
   public int getHeight() {
      return this.world.getHeight();
   }
}
