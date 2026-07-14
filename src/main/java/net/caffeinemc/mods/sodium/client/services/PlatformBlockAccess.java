package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;

public interface PlatformBlockAccess {
   PlatformBlockAccess INSTANCE = Services.load(PlatformBlockAccess.class);

   static PlatformBlockAccess getInstance() {
      return INSTANCE;
   }

   int getLightEmission(BlockState var1, BlockRenderView var2, BlockPos var3);

   boolean shouldSkipRender(BlockView var1, BlockState var2, BlockState var3, BlockPos var4, BlockPos var5, Direction var6);

   boolean shouldShowFluidOverlay(BlockState var1, BlockRenderView var2, BlockPos var3, FluidState var4);

   boolean platformHasBlockData();

   float getNormalVectorShade(ModelQuadView var1, BlockRenderView var2, boolean var3);

   AmbientOcclusionMode usesAmbientOcclusion(
      BakedModel model, BlockState state, SodiumModelData data, RenderLayer renderType, BlockRenderView level, BlockPos pos
   );

   boolean shouldBlockEntityGlow(BlockEntity var1, ClientPlayerEntity var2);

   boolean shouldOccludeFluid(Direction var1, BlockState var2, FluidState var3);
}
