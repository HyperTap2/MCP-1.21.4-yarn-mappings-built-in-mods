package net.irisshaders.iris.layer;

import net.minecraft.client.render.RenderPhase;

public final class BlockEntityRenderStateShard extends RenderPhase {
   public static final BlockEntityRenderStateShard INSTANCE = new BlockEntityRenderStateShard();

   private BlockEntityRenderStateShard() {
      super("iris:is_block_entity", GbufferPrograms::beginBlockEntities, GbufferPrograms::endBlockEntities);
   }
}
