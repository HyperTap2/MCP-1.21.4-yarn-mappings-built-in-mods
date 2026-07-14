package net.minecraft.client.render.entity.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.state.CatEntityRenderState;

public class CatEntityModel extends FelineEntityModel<CatEntityRenderState> {
   public static final ModelTransformer CAT_TRANSFORMER = ModelTransformer.scaling(0.8F);

   public CatEntityModel(ModelPart modelPart) {
      super(modelPart);
   }
}
