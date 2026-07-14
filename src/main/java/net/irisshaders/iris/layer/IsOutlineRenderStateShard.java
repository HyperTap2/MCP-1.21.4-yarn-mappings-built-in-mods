package net.irisshaders.iris.layer;

import net.minecraft.client.render.RenderPhase;

public class IsOutlineRenderStateShard extends RenderPhase {
   public static final IsOutlineRenderStateShard INSTANCE = new IsOutlineRenderStateShard();

   private IsOutlineRenderStateShard() {
      super("iris:is_outline", GbufferPrograms::beginOutline, GbufferPrograms::endOutline);
   }
}
