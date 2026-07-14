package net.irisshaders.iris.layer;

import net.minecraft.client.render.RenderPhase;

public final class EntityRenderStateShard extends RenderPhase {
   public static final EntityRenderStateShard INSTANCE = new EntityRenderStateShard();

   private EntityRenderStateShard() {
      super("iris:is_entity", GbufferPrograms::beginEntities, GbufferPrograms::endEntities);
   }
}
