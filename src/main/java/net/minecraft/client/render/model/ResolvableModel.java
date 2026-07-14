package net.minecraft.client.render.model;

import net.minecraft.util.Identifier;

public interface ResolvableModel {
   void resolve(ResolvableModel.Resolver resolver);

   interface Resolver {
      UnbakedModel resolve(Identifier id);
   }
}
