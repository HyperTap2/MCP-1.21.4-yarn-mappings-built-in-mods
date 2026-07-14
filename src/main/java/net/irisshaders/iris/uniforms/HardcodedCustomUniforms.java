package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;
import org.joml.Math;

public class HardcodedCustomUniforms {
   private static final MinecraftClient client = MinecraftClient.getInstance();
   private static RegistryEntry<Biome> storedBiome;

   public static void addHardcodedCustomUniforms(UniformHolder holder, FrameUpdateNotifier updateNotifier) {
      updateNotifier.addListener(() -> {
         if (MinecraftClient.getInstance().world != null) {
            storedBiome = MinecraftClient.getInstance().world.getBiome(MinecraftClient.getInstance().getCameraEntity().getBlockPos());
         } else {
            storedBiome = null;
         }
      });
      CameraUniforms.CameraPositionTracker tracker = new CameraUniforms.CameraPositionTracker(updateNotifier);
      SmoothedFloat eyeInCave = new SmoothedFloat(6.0F, 12.0F, HardcodedCustomUniforms::getEyeInCave, updateNotifier);
      SmoothedFloat rainStrengthS = rainStrengthS(updateNotifier, 15.0F, 15.0F);
      SmoothedFloat rainStrengthShining = rainStrengthS(updateNotifier, 10.0F, 11.0F);
      SmoothedFloat rainStrengthS2 = rainStrengthS(updateNotifier, 70.0F, 1.0F);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "timeAngle", HardcodedCustomUniforms::getTimeAngle);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "timeBrightness", HardcodedCustomUniforms::getTimeBrightness);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "moonBrightness", HardcodedCustomUniforms::getMoonBrightness);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "shadowFade", HardcodedCustomUniforms::getShadowFade);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "rainStrengthS", rainStrengthS);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "rainStrengthShiningStars", rainStrengthShining);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "rainStrengthS2", rainStrengthS2);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "blindFactor", HardcodedCustomUniforms::getBlindFactor);
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME, "isDry", new SmoothedFloat(20.0F, 10.0F, () -> getRawPrecipitation() == 0.0F ? 1.0F : 0.0F, updateNotifier)
      );
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME, "isRainy", new SmoothedFloat(20.0F, 10.0F, () -> getRawPrecipitation() == 1.0F ? 1.0F : 0.0F, updateNotifier)
      );
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME, "isSnowy", new SmoothedFloat(20.0F, 10.0F, () -> getRawPrecipitation() == 2.0F ? 1.0F : 0.0F, updateNotifier)
      );
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "isEyeInCave", () -> CommonUniforms.isEyeInWater() == 0 ? eyeInCave.getAsFloat() : 0.0F);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "velocity", () -> getVelocity(tracker));
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "starter", getStarter(tracker, updateNotifier));
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME, "frameTimeSmooth", new SmoothedFloat(5.0F, 5.0F, SystemTimeUniforms.TIMER::getLastFrameTime, updateNotifier)
      );
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME, "eyeBrightnessM", new SmoothedFloat(5.0F, 5.0F, HardcodedCustomUniforms::getEyeBrightnessM, updateNotifier)
      );
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "rainFactor", rainStrengthS);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "inSwamp", new SmoothedFloat(5.0F, 5.0F, () -> {
         if (storedBiome == null) {
            return 0.0F;
         } else {
            return storedBiome.isIn(BiomeTags.HAS_CLOSER_WATER_FOG) ? 1.0F : 0.0F;
         }
      }, updateNotifier));
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "BiomeTemp", () -> storedBiome == null ? 0.0F : ((Biome)storedBiome.value()).getTemperature());
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "day", HardcodedCustomUniforms::getDay);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "night", HardcodedCustomUniforms::getNight);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "dawnDusk", HardcodedCustomUniforms::getDawnDusk);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "shdFade", HardcodedCustomUniforms::getShdFade);
      holder.uniform1f(
         UniformUpdateFrequency.PER_FRAME,
         "isPrecipitationRain",
         new SmoothedFloat(6.0F, 6.0F, () -> getRawPrecipitation() == 1.0F && tracker.getCurrentCameraPosition().y < 96.0 ? 1.0F : 0.0F, updateNotifier)
      );
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "touchmybody", new SmoothedFloat(0.0F, 0.1F, HardcodedCustomUniforms::getHurtFactor, updateNotifier));
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "sneakSmooth", new SmoothedFloat(2.0F, 0.9F, HardcodedCustomUniforms::getSneakFactor, updateNotifier));
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "burningSmooth", new SmoothedFloat(1.0F, 2.0F, HardcodedCustomUniforms::getBurnFactor, updateNotifier));
      SmoothedFloat smoothSpeed = new SmoothedFloat(1.0F, 1.5F, () -> getVelocity(tracker) / SystemTimeUniforms.TIMER.getLastFrameTime(), updateNotifier);
      holder.uniform1f(UniformUpdateFrequency.PER_FRAME, "effectStrength", () -> getHyperSpeedStrength(smoothSpeed));
   }

   private static float getHyperSpeedStrength(SmoothedFloat smoothSpeed) {
      return (float)(1.0 - Math.exp(-smoothSpeed.getAsFloat() * 0.003906F));
   }

   private static float getBurnFactor() {
      return MinecraftClient.getInstance().player.isOnFire() ? 1.0F : 0.0F;
   }

   private static float getSneakFactor() {
      return MinecraftClient.getInstance().player.isInSneakingPose() ? 1.0F : 0.0F;
   }

   private static float getHurtFactor() {
      PlayerEntity player = MinecraftClient.getInstance().player;
      return player.hurtTime <= 0 && player.deathTime <= 0 ? 0.0F : 0.4F;
   }

   private static float getEyeInCave() {
      return client.getCameraEntity().getEyeY() < 5.0 ? 1.0F - getEyeSkyBrightness() / 240.0F : 0.0F;
   }

   private static float getEyeBrightnessM() {
      return getEyeSkyBrightness() / 240.0F;
   }

   private static float getEyeSkyBrightness() {
      if (client.cameraEntity != null && client.world != null) {
         Vec3d feet = client.cameraEntity.getPos();
         Vec3d eyes = new Vec3d(feet.x, client.cameraEntity.getEyeY(), feet.z);
         BlockPos eyeBlockPos = BlockPos.ofFloored(eyes);
         int skyLight = client.world.getLightLevel(LightType.SKY, eyeBlockPos);
         return skyLight * 16;
      } else {
         return 0.0F;
      }
   }

   private static float getVelocity(CameraUniforms.CameraPositionTracker tracker) {
      float difX = (float)(tracker.getCurrentCameraPosition().x - tracker.getPreviousCameraPosition().x);
      float difY = (float)(tracker.getCurrentCameraPosition().y - tracker.getPreviousCameraPosition().y);
      float difZ = (float)(tracker.getCurrentCameraPosition().z - tracker.getPreviousCameraPosition().z);
      return Math.sqrt(difX * difX + difY * difY + difZ * difZ);
   }

   private static SmoothedFloat getStarter(CameraUniforms.CameraPositionTracker tracker, FrameUpdateNotifier notifier) {
      return new SmoothedFloat(20.0F, 20.0F, new SmoothedFloat(0.0F, 3.1536E7F, () -> getMoving(tracker), notifier), notifier);
   }

   private static float getMoving(CameraUniforms.CameraPositionTracker tracker) {
      float difX = (float)(tracker.getCurrentCameraPosition().x - tracker.getPreviousCameraPosition().x);
      float difY = (float)(tracker.getCurrentCameraPosition().y - tracker.getPreviousCameraPosition().y);
      float difZ = (float)(tracker.getCurrentCameraPosition().z - tracker.getPreviousCameraPosition().z);
      float difSum = Math.abs(difX) + Math.abs(difY) + Math.abs(difZ);
      return difSum > 0.0F && difSum < 1.0F ? 1.0F : 0.0F;
   }

   private static float getTimeAngle() {
      return getWorldDayTime() / 24000.0F;
   }

   private static int getWorldDayTime() {
      World level = MinecraftClient.getInstance().world;
      long timeOfDay = level.getTimeOfDay();
      long dayTime = level.getDimension().fixedTime().orElse(timeOfDay % 24000L);
      return (int)dayTime;
   }

   private static float getTimeBrightness() {
      return (float)java.lang.Math.max(java.lang.Math.sin(getTimeAngle() * java.lang.Math.PI * 2.0), 0.0);
   }

   private static float getMoonBrightness() {
      return (float)java.lang.Math.max(java.lang.Math.sin(getTimeAngle() * java.lang.Math.PI * -2.0), 0.0);
   }

   private static float getShadowFade() {
      return (float)Math.clamp(0.0, 1.0, 1.0 - (java.lang.Math.abs(java.lang.Math.abs(CelestialUniforms.getSunAngle() - 0.5) - 0.25) - 0.23) * 100.0);
   }

   private static SmoothedFloat rainStrengthS(FrameUpdateNotifier updateNotifier, float halfLifeUp, float halfLifeDown) {
      return new SmoothedFloat(halfLifeUp, halfLifeDown, CommonUniforms::getRainStrength, updateNotifier);
   }

   private static float getRawPrecipitation() {
      if (storedBiome == null) {
         return 0.0F;
      }

      Precipitation precipitation = ((Biome)storedBiome.value())
         .getPrecipitation(MinecraftClient.getInstance().cameraEntity.getBlockPos(), MinecraftClient.getInstance().world.getSeaLevel());

      return switch (precipitation) {
         case RAIN -> 1.0F;
         case SNOW -> 2.0F;
         default -> 0.0F;
      };
   }

   private static float getBlindFactor() {
      float blindFactorSqrt = (float)Math.clamp(0.0, 1.0, CommonUniforms.getBlindness() * 2.0 - 1.0);
      return blindFactorSqrt * blindFactorSqrt;
   }

   private static float frac(float value) {
      return java.lang.Math.abs(value % 1.0F);
   }

   private static float getAdjTime() {
      return Math.abs((WorldTimeUniforms.getWorldDayTime() / 1000.0F + 6.0F) % 24.0F - 12.0F);
   }

   private static float getDay() {
      return Math.clamp(0.0F, 1.0F, 5.4F - getAdjTime());
   }

   private static float getNight() {
      return Math.clamp(0.0F, 1.0F, getAdjTime() - 6.0F);
   }

   private static float getDawnDusk() {
      return 1.0F - getDay() - getNight();
   }

   private static float getShdFade() {
      return (float)Math.clamp(0.0, 1.0, 1.0 - (Math.abs(Math.abs(CelestialUniforms.getSunAngle() - 0.5) - 0.25) - 0.225) * 40.0);
   }
}
