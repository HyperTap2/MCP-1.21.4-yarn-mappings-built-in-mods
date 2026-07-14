package net.minecraft.client.render.model;

import net.minecraft.client.model.ModelNameSupplier;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.Debug;

public interface Baker {
   BakedModel bake(Identifier id, ModelBakeSettings settings);

   SpriteGetter getSpriteGetter();

   @Debug
   ModelNameSupplier getModelNameSupplier();
}
