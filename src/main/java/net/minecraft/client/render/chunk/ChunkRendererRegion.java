package net.minecraft.client.render.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.Nullable;

public class ChunkRendererRegion implements BlockRenderView {
   public static final int field_52160 = 1;
   public static final int field_52161 = 3;
   private final int chunkXOffset;
   private final int chunkZOffset;
   protected final RenderedChunk[] chunks;
   protected final World world;

   ChunkRendererRegion(World world, int chunkX, int chunkZ, RenderedChunk[] chunks) {
      this.world = world;
      this.chunkXOffset = chunkX;
      this.chunkZOffset = chunkZ;
      this.chunks = chunks;
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockState(pos);
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockState(pos).getFluidState();
   }

   public float getBrightness(Direction direction, boolean shaded) {
      return this.world.getBrightness(direction, shaded);
   }

   public LightingProvider getLightingProvider() {
      return this.world.getLightingProvider();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.getRenderedChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ())).getBlockEntity(pos);
   }

   private RenderedChunk getRenderedChunk(int x, int z) {
      return this.chunks[getIndex(this.chunkXOffset, this.chunkZOffset, x, z)];
   }

   public int getColor(BlockPos pos, ColorResolver colorResolver) {
      return this.world.getColor(pos, colorResolver);
   }

   public int getBottomY() {
      return this.world.getBottomY();
   }

   public int getHeight() {
      return this.world.getHeight();
   }

   public static int getIndex(int xOffset, int zOffset, int x, int z) {
      return x - xOffset + (z - zOffset) * 3;
   }
}
