package net.minecraft.client.render.entity.model;

import net.minecraft.util.Identifier;

public record EntityModelLayer(Identifier id, String name) {
   @Override
   public String toString() {
      return this.id + "#" + this.name;
   }
}
