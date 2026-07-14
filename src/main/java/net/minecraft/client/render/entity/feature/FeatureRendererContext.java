package net.minecraft.client.render.entity.feature;

import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;

public interface FeatureRendererContext<S extends EntityRenderState, M extends EntityModel<? super S>> {
   M getModel();
}
