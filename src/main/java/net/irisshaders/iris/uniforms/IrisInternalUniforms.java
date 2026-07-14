package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.minecraft.client.render.Fog;
import org.joml.Vector4f;

public class IrisInternalUniforms {
   private static final Vector4f ONE = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);

   private IrisInternalUniforms() {
   }

   public static void addFogUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
      uniforms.uniform4f("iris_FogColor", () -> {
         Fog fog = RenderSystem.getShaderFog();
         return fog == Fog.DUMMY ? ONE : new Vector4f(fog.red(), fog.green(), fog.blue(), fog.alpha());
      }, t -> {});
      uniforms.uniform1f("iris_FogStart", () -> RenderSystem.getShaderFog().start(), t -> {})
         .uniform1f("iris_FogEnd", () -> RenderSystem.getShaderFog().end(), t -> {});
      uniforms.uniform1f("iris_FogDensity", () -> Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity()), notifier -> {});
      uniforms.uniform1f("iris_currentAlphaTest", CapturedRenderingState.INSTANCE::getCurrentAlphaTest, notifier -> {});
      uniforms.uniform1f("alphaTestRef", CapturedRenderingState.INSTANCE::getCurrentAlphaTest, notifier -> {});
   }
}
