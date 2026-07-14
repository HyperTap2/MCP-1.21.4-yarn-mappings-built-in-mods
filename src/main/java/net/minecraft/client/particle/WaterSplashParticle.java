package net.minecraft.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;

public class WaterSplashParticle extends RainSplashParticle {
   WaterSplashParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
      super(clientWorld, d, e, f);
      this.gravityStrength = 0.04F;
      if (h == 0.0 && (g != 0.0 || i != 0.0)) {
         this.velocityX = g;
         this.velocityY = 0.1;
         this.velocityZ = i;
      }
   }

   public static class SplashFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public SplashFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         WaterSplashParticle waterSplashParticle = new WaterSplashParticle(clientWorld, d, e, f, g, h, i);
         waterSplashParticle.setSprite(this.spriteProvider);
         return waterSplashParticle;
      }
   }
}
