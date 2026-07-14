package net.irisshaders.iris.shadows.frustum.fallback;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class BoxCullingFrustum extends Frustum implements net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum, ViewportProvider {
   private final BoxCuller boxCuller;
   private final Vector3d position = new Vector3d();

   public BoxCullingFrustum(BoxCuller boxCuller) {
      super(new Matrix4f(), new Matrix4f());
      this.boxCuller = boxCuller;
   }

   public void setPosition(double cameraX, double cameraY, double cameraZ) {
      this.position.set(cameraX, cameraY, cameraZ);
      this.boxCuller.setPosition(cameraX, cameraY, cameraZ);
   }

   public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return false;
   }

   public boolean isVisible(Box box) {
      return !this.boxCuller.isCulled(box);
   }

   public Viewport sodium$createViewport() {
      return new Viewport(this, this.position);
   }

   public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
      return !this.boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
