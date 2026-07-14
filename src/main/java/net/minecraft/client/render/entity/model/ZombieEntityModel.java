package net.minecraft.client.render.entity.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.ZombieEntityRenderState;

public class ZombieEntityModel<S extends ZombieEntityRenderState> extends AbstractZombieModel<S> {
   public ZombieEntityModel(ModelPart modelPart) {
      super(modelPart);
   }
}
