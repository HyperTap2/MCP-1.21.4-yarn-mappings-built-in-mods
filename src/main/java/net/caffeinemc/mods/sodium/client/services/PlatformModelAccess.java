package net.caffeinemc.mods.sodium.client.services;

import java.util.List;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface PlatformModelAccess {
   PlatformModelAccess INSTANCE = Services.load(PlatformModelAccess.class);

   static PlatformModelAccess getInstance() {
      return INSTANCE;
   }

   Iterable<RenderLayer> getModelRenderTypes(BlockRenderView var1, BakedModel var2, BlockState var3, BlockPos var4, Random var5, SodiumModelData var6);

   List<BakedQuad> getQuads(
      BlockRenderView var1, BlockPos var2, BakedModel var3, BlockState var4, Direction var5, Random var6, RenderLayer var7, SodiumModelData var8
   );

   SodiumModelDataContainer getModelDataContainer(World var1, ChunkSectionPos var2);

   SodiumModelData getModelData(LevelSlice var1, BakedModel var2, BlockState var3, BlockPos var4, SodiumModelData var5);

   @Internal
   SodiumModelData getEmptyModelData();
}
