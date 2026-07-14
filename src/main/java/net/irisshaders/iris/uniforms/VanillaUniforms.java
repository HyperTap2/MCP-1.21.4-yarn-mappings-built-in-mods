package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.platform.GlStateManager.Viewport;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import org.joml.Vector2f;

public class VanillaUniforms {
   public static void addVanillaUniforms(DynamicUniformHolder uniforms) {
      Vector2f cachedScreenSize = new Vector2f();
      uniforms.uniform1f("iris_LineWidth", RenderSystem::getShaderLineWidth, listener -> {});
      uniforms.uniform2f("iris_ScreenSize", () -> cachedScreenSize.set(Viewport.getWidth(), Viewport.getHeight()), listener -> {});
   }
}
