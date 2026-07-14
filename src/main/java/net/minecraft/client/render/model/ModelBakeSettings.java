package net.minecraft.client.render.model;

import net.minecraft.util.math.AffineTransformation;

public interface ModelBakeSettings {
   default AffineTransformation getRotation() {
      return AffineTransformation.identity();
   }

   default boolean isUvLocked() {
      return false;
   }
}
