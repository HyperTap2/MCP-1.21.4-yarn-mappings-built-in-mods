package net.minecraft.client.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.jetbrains.annotations.Nullable;

public interface ParticleFactory<T extends ParticleEffect> {
   @Nullable
   Particle createParticle(T parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

   interface BlockLeakParticleFactory<T extends ParticleEffect> {
      @Nullable
      SpriteBillboardParticle createParticle(
         T parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ
      );
   }
}
