package com.viaversion.viafabricplus.features.footstep_particle;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class FootStepParticle1_12_2 extends SpriteBillboardParticle {
   public static final Identifier ID = Identifier.of("viafabricplus", "footstep");
   public static final SimpleParticleType TYPE = Registry.register(Registries.PARTICLE_TYPE, ID, new SimpleParticleType(true));
   public static int RAW_ID;

   private FootStepParticle1_12_2(ClientWorld clientWorld, double x, double y, double z) {
      super(clientWorld, x, y, z);
      this.scale = 0.125F;
      this.setMaxAge(200);
   }

   public ParticleTextureSheet getType() {
      return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
      float strength = (this.age + tickDelta) / this.maxAge;
      this.alpha = 2.0F - strength * strength * 2.0F;
      if (this.alpha > 1.0F) {
         this.alpha = 0.2F;
      } else {
         this.alpha *= 0.2F;
      }

      Vec3d cameraPos = camera.getPos();
      float x = (float)(MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cameraPos.getX());
      float y = (float)(MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cameraPos.getY());
      float z = (float)(MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cameraPos.getZ());
      float minU = this.getMinU();
      float maxU = this.getMaxU();
      float minV = this.getMinV();
      float maxV = this.getMaxV();
      int light = this.getBrightness(tickDelta);
      vertexConsumer.vertex(x - this.scale, y, z + this.scale).texture(maxU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light);
      vertexConsumer.vertex(x + this.scale, y, z + this.scale).texture(maxU, minV).color(this.red, this.green, this.blue, this.alpha).light(light);
      vertexConsumer.vertex(x + this.scale, y, z - this.scale).texture(minU, minV).color(this.red, this.green, this.blue, this.alpha).light(light);
      vertexConsumer.vertex(x - this.scale, y, z - this.scale).texture(minU, maxV).color(this.red, this.green, this.blue, this.alpha).light(light);
   }

   public static void init() {
   }

   static {
      RAW_ID = Registries.PARTICLE_TYPE.getRawId(TYPE);
   }

   public static class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ
      ) {
         if (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2)) {
            throw new UnsupportedOperationException("FootStepParticle is not supported on versions newer than 1.12.2");
         }

         FootStepParticle1_12_2 particle = new FootStepParticle1_12_2(world, x, y, z);
         particle.setSprite(this.spriteProvider);
         return particle;
      }
   }
}
