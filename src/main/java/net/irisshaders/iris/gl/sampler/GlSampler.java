package net.irisshaders.iris.gl.sampler;

import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;

public class GlSampler extends GlResource {
   public static final GlSampler MIPPED_LINEAR_HW = new GlSampler(true, true, true, true);
   public static final GlSampler LINEAR_HW = new GlSampler(true, false, true, true);
   public static final GlSampler MIPPED_NEAREST_HW = new GlSampler(false, true, true, true);
   public static final GlSampler NEAREST_HW = new GlSampler(false, false, true, true);
   public static final GlSampler MIPPED_LINEAR = new GlSampler(true, true, false, false);
   public static final GlSampler LINEAR = new GlSampler(true, false, false, false);
   public static final GlSampler MIPPED_NEAREST = new GlSampler(false, true, false, false);
   public static final GlSampler NEAREST = new GlSampler(false, false, false, false);

   public GlSampler(boolean linear, boolean mipmapped, boolean shadow, boolean hardwareShadow) {
      super(IrisRenderSystem.genSampler());
      IrisRenderSystem.samplerParameteri(this.getId(), 10241, linear ? 9729 : 9728);
      IrisRenderSystem.samplerParameteri(this.getId(), 10240, linear ? 9729 : 9728);
      IrisRenderSystem.samplerParameteri(this.getId(), 10242, 33071);
      IrisRenderSystem.samplerParameteri(this.getId(), 10243, 33071);
      if (mipmapped) {
         IrisRenderSystem.samplerParameteri(this.getId(), 10241, linear ? 9987 : 9984);
      }

      if (hardwareShadow) {
         IrisRenderSystem.samplerParameteri(this.getId(), 34892, 34894);
      }
   }

   @Override
   protected void destroyInternal() {
      IrisRenderSystem.destroySampler(this.getGlId());
   }

   public int getId() {
      return this.getGlId();
   }
}
