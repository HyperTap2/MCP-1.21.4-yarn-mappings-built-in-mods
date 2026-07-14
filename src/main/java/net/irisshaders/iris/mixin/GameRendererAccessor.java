package net.irisshaders.iris.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.Pool;
import net.minecraft.client.util.math.MatrixStack;

public interface GameRendererAccessor {
   boolean getRenderHand();

   boolean getPanoramicMode();

   void invokeBobView(MatrixStack matrices, float tickDelta);

   void invokeBobHurt(MatrixStack matrices, float tickDelta);

   float invokeGetFov(Camera camera, float tickDelta, boolean changingFov);

   boolean shouldRenderBlockOutlineA();

   Pool getResourcePool();
}
