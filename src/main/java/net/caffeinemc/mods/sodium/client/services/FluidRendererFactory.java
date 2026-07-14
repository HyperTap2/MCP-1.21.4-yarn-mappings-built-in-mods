package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;

public interface FluidRendererFactory {
   FluidRendererFactory INSTANCE = Services.load(FluidRendererFactory.class);

   static FluidRendererFactory getInstance() {
      return INSTANCE;
   }

   FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry var1, LightPipelineProvider var2);

   BlendedColorProvider<FluidState> getWaterColorProvider();

   BlendedColorProvider<BlockState> getWaterBlockColorProvider();
}
