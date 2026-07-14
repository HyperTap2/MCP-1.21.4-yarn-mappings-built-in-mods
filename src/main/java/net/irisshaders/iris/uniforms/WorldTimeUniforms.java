package net.irisshaders.iris.uniforms;

import java.util.Objects;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public final class WorldTimeUniforms {
   private WorldTimeUniforms() {
   }

   public static void addWorldTimeUniforms(UniformHolder uniforms) {
      uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "worldTime", WorldTimeUniforms::getWorldDayTime)
         .uniform1i(UniformUpdateFrequency.PER_TICK, "worldDay", WorldTimeUniforms::getWorldDay)
         .uniform1i(UniformUpdateFrequency.PER_TICK, "moonPhase", () -> getWorld().getMoonPhase());
   }

   static int getWorldDayTime() {
      long timeOfDay = getWorld().getTimeOfDay();
      if (Iris.getCurrentDimension() != DimensionId.END && Iris.getCurrentDimension() != DimensionId.NETHER) {
         long dayTime = getWorld().getDimension().fixedTime().orElse(timeOfDay % 24000L);
         return (int)dayTime;
      } else {
         return (int)(timeOfDay % 24000L);
      }
   }

   private static int getWorldDay() {
      long timeOfDay = getWorld().getTimeOfDay();
      long day = timeOfDay / 24000L;
      return (int)day;
   }

   private static ClientWorld getWorld() {
      return Objects.requireNonNull(MinecraftClient.getInstance().world);
   }
}
