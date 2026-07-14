package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.minecraft.client.render.FogShape;
import org.joml.Vector3f;

public class FogUniforms {
   private FogUniforms() {
   }

   public static void addFogUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
      if (fogMode == FogMode.OFF) {
         uniforms.uniform1i(UniformUpdateFrequency.ONCE, "fogMode", () -> 0);
         uniforms.uniform1i(UniformUpdateFrequency.ONCE, "fogShape", () -> -1);
      } else if (fogMode == FogMode.PER_VERTEX || fogMode == FogMode.PER_FRAGMENT) {
         uniforms.uniform1i("fogMode", () -> {
            float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();
            return fogDensity < 0.0F ? 9729 : 2049;
         }, listener -> {});
         uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "fogShape", () -> RenderSystem.getShaderFog().shape() == FogShape.CYLINDER ? 1 : 0);
      }

      uniforms.uniform1f("fogDensity", () -> Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity()), notifier -> {});
      uniforms.uniform1f("fogStart", () -> RenderSystem.getShaderFog().start(), listener -> StateUpdateNotifiers.fogStartNotifier.setListener(listener));
      uniforms.uniform1f("fogEnd", () -> RenderSystem.getShaderFog().end(), listener -> StateUpdateNotifiers.fogEndNotifier.setListener(listener));
      uniforms.uniform3f(
         UniformUpdateFrequency.PER_FRAME,
         "fogColor",
         () -> new Vector3f(
            (float)CapturedRenderingState.INSTANCE.getFogColor().x,
            (float)CapturedRenderingState.INSTANCE.getFogColor().y,
            (float)CapturedRenderingState.INSTANCE.getFogColor().z
         )
      );
   }
}
