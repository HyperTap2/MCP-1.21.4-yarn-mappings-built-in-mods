package net.minecraft.client.gl;

import com.mojang.blaze3d.systems.RenderSystem;

public class SimpleFramebuffer extends Framebuffer {
   public SimpleFramebuffer(int width, int height, boolean useDepth) {
      super(useDepth);
      RenderSystem.assertOnRenderThreadOrInit();
      this.resize(width, height);
   }
}
