package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;

public abstract class FluidRenderer {
   public abstract void render(
      LevelSlice var1, BlockState var2, FluidState var3, BlockPos var4, BlockPos var5, TranslucentGeometryCollector var6, ChunkBuildBuffers var7
   );
}
