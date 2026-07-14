package net.minecraft.client.particle;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.world.ClientWorld;
import net.irisshaders.iris.fantastic.IrisParticleRenderTypes;
import net.minecraft.particle.BlockStateParticleEffect;

public class BlockMarkerParticle extends SpriteBillboardParticle {
   private final boolean iris$opaque;

   BlockMarkerParticle(ClientWorld world, double x, double y, double z, BlockState state) {
      super(world, x, y, z);
      RenderLayer layer = RenderLayers.getBlockLayer(state);
      this.iris$opaque = layer == RenderLayer.getSolid() || layer == RenderLayer.getCutout() || layer == RenderLayer.getCutoutMipped();
      this.setSprite(MinecraftClient.getInstance().getBlockRenderManager().getModels().getModelParticleSprite(state));
      this.gravityStrength = 0.0F;
      this.maxAge = 80;
      this.collidesWithWorld = false;
   }

   @Override
   public ParticleTextureSheet getType() {
      return this.iris$opaque ? IrisParticleRenderTypes.TERRAIN_OPAQUE : ParticleTextureSheet.TERRAIN_SHEET;
   }

   @Override
   public float getSize(float tickDelta) {
      return 0.5F;
   }

   public static class Factory implements ParticleFactory<BlockStateParticleEffect> {
      public Particle createParticle(
         BlockStateParticleEffect blockStateParticleEffect, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i
      ) {
         return new BlockMarkerParticle(clientWorld, d, e, f, blockStateParticleEffect.getBlockState());
      }
   }
}
