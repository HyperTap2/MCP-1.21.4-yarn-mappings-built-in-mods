package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.minecraft.client.MinecraftClient;

public final class ViewportUniforms {
   private ViewportUniforms() {
   }

   public static void addViewportUniforms(UniformHolder uniforms) {
      uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "viewHeight", () -> MinecraftClient.getInstance().getFramebuffer().textureHeight)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "viewWidth", () -> MinecraftClient.getInstance().getFramebuffer().textureWidth)
         .uniform1f(UniformUpdateFrequency.PER_FRAME, "aspectRatio", ViewportUniforms::getAspectRatio);
   }

   private static float getAspectRatio() {
      return (float)MinecraftClient.getInstance().getFramebuffer().textureWidth / MinecraftClient.getInstance().getFramebuffer().textureHeight;
   }
}
