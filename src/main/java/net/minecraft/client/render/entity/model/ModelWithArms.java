package net.minecraft.client.render.entity.model;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public interface ModelWithArms {
   void setArmAngle(Arm arm, MatrixStack matrices);
}
