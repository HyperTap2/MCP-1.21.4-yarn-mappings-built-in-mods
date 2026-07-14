package net.irisshaders.iris.shadows.frustum.fallback;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.minecraft.client.render.Frustum;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class NonCullingFrustum extends Frustum implements ViewportProvider, net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum {
   private final Vector3d position = new Vector3d();

   public NonCullingFrustum() {
      super(new Matrix4f(), new Matrix4f());
   }

   public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return false;
   }

   public boolean isVisible(Box box) {
      return true;
   }

   public int intersectAab(BlockBox box) {
      return -2;
   }

   public double getX() {
      return this.position.x;
   }

   public double getY() {
      return this.position.y;
   }

   public double getZ() {
      return this.position.z;
   }

   public void setPosition(double cameraX, double cameraY, double cameraZ) {
      this.position.set(cameraX, cameraY, cameraZ);
   }

   public Viewport sodium$createViewport() {
      return new Viewport(this, this.position);
   }

   public boolean testAab(float v, float v1, float v2, float v3, float v4, float v5) {
      return true;
   }
}
