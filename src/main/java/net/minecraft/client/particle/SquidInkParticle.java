package net.minecraft.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;

public class SquidInkParticle extends AnimatedParticle {
   SquidInkParticle(
      ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, int color, SpriteProvider spriteProvider
   ) {
      super(world, x, y, z, spriteProvider, 0.0F);
      this.velocityMultiplier = 0.92F;
      this.scale = 0.5F;
      this.setAlpha(1.0F);
      this.setColor(ColorHelper.getRed(color), ColorHelper.getGreen(color), ColorHelper.getBlue(color));
      this.maxAge = (int)(this.scale * 12.0F / (Math.random() * 0.8F + 0.2F));
      this.setSpriteForAge(spriteProvider);
      this.collidesWithWorld = false;
      this.velocityX = velocityX;
      this.velocityY = velocityY;
      this.velocityZ = velocityZ;
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.dead) {
         this.setSpriteForAge(this.spriteProvider);
         if (this.age > this.maxAge / 2) {
            this.setAlpha(1.0F - ((float)this.age - this.maxAge / 2) / this.maxAge);
         }

         if (this.world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z)).isAir()) {
            this.velocityY -= 0.0074F;
         }
      }
   }

   public static class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         return new SquidInkParticle(clientWorld, d, e, f, g, h, i, ColorHelper.getArgb(255, 255, 255, 255), this.spriteProvider);
      }
   }

   public static class GlowSquidInkFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public GlowSquidInkFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
         return new SquidInkParticle(clientWorld, d, e, f, g, h, i, ColorHelper.getArgb(255, 204, 31, 102), this.spriteProvider);
      }
   }
}
