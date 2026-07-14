package net.irisshaders.iris.shadows;

import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;

public class ShadowRenderingState {
   private static ShadowRenderingState.BlockEntityRenderFunction function = ShadowRenderer::renderBlockEntities;

   public static boolean areShadowsCurrentlyBeingRendered() {
      return ShadowRenderer.ACTIVE;
   }

   public static void setBlockEntityRenderFunction(ShadowRenderingState.BlockEntityRenderFunction function) {
      ShadowRenderingState.function = function;
   }

   public static int renderBlockEntities(
      ShadowRenderer shadowRenderer,
      BufferBuilderStorage bufferSource,
      MatrixStack modelView,
      Camera camera,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      boolean hasEntityFrustum,
      boolean lightsOnly
   ) {
      return function.renderBlockEntities(shadowRenderer, bufferSource, modelView, camera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, lightsOnly);
   }

   public static int getRenderDistance() {
      return ShadowRenderer.renderDistance;
   }

   public interface BlockEntityRenderFunction {
      int renderBlockEntities(
         ShadowRenderer var1,
         BufferBuilderStorage var2,
         MatrixStack var3,
         Camera var4,
         double var5,
         double var7,
         double var9,
         float var11,
         boolean var12,
         boolean var13
      );
   }
}
