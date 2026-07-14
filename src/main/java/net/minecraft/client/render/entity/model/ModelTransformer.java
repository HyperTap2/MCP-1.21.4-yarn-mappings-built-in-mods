package net.minecraft.client.render.entity.model;

import net.minecraft.client.model.ModelData;

@FunctionalInterface
public interface ModelTransformer {
   static ModelTransformer scaling(float f) {
      float g = 24.016F * (1.0F - f);
      return modelData -> modelData.transform(modelTransform -> modelTransform.scaled(f).addPivot(0.0F, g, 0.0F));
   }

   ModelData apply(ModelData modelData);
}
