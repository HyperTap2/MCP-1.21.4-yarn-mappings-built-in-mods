package net.minecraft.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class SpitParticle extends ExplosionSmokeParticle {
   SpitParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, SpriteProvider spriteProvider) {
      super(clientWorld, d, e, f, g, h, i, spriteProvider);
      this.gravityStrength = 0.5F;
   }

   public static class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         return new SpitParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
      }
   }
}
