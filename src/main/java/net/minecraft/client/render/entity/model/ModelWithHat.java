package net.minecraft.client.render.entity.model;

import net.minecraft.client.util.math.MatrixStack;

public interface ModelWithHat {
   void setHatVisible(boolean visible);

   void rotateArms(MatrixStack stack);
}
