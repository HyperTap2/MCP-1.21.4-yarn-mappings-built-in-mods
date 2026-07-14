package net.irisshaders.iris.shadows.frustum.advanced;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.minecraft.util.math.Box;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public class ReversedAdvancedShadowCullingFrustum extends AdvancedShadowCullingFrustum implements Frustum {
   private final BoxCuller distanceCuller;

   public ReversedAdvancedShadowCullingFrustum(
      Matrix4fc modelViewProjection, Matrix4fc shadowProjection, Vector3f shadowLightVectorFromOrigin, BoxCuller voxelCuller, BoxCuller distanceCuller
   ) {
      super(modelViewProjection, shadowProjection, shadowLightVectorFromOrigin, voxelCuller);
      this.distanceCuller = distanceCuller;
   }

   @Override
   public void setPosition(double cameraX, double cameraY, double cameraZ) {
      if (this.distanceCuller != null) {
         this.distanceCuller.setPosition(cameraX, cameraY, cameraZ);
      }

      super.setPosition(cameraX, cameraY, cameraZ);
   }

   @Override
   public boolean isVisible(Box box) {
      if (this.distanceCuller != null && this.distanceCuller.isCulled(box)) {
         return false;
      } else {
         return this.boxCuller != null && !this.boxCuller.isCulled(box)
            ? true
            : this.isVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ) != 0;
      }
   }

   @Override
   public int fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      if (this.distanceCuller != null && this.distanceCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ)) {
         return 0;
      } else {
         return this.boxCuller != null && !this.boxCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ) ? 2 : this.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Override
   public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      if (this.distanceCuller != null && this.distanceCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
         return false;
      } else {
         return this.boxCuller != null && !this.boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)
            ? true
            : this.checkCornerVisibility(minX, minY, minZ, maxX, maxY, maxZ) > 0;
      }
   }
}
