package net.minecraft.client.gl;

import net.minecraft.client.util.ClosableFactory;

public record SimpleFramebufferFactory(int width, int height, boolean useDepth) implements ClosableFactory<Framebuffer> {
   public Framebuffer create() {
      return new SimpleFramebuffer(this.width, this.height, this.useDepth);
   }

   public void close(Framebuffer framebuffer) {
      framebuffer.delete();
   }
}
