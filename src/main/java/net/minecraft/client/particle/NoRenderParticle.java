package net.minecraft.client.particle;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;

public class NoRenderParticle extends Particle {
   protected NoRenderParticle(ClientWorld clientWorld, double d, double e, double f) {
      super(clientWorld, d, e, f);
   }

   protected NoRenderParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
      super(clientWorld, d, e, f, g, h, i);
   }

   @Override
   public final void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
   }

   @Override
   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.NO_RENDER;
   }
}
