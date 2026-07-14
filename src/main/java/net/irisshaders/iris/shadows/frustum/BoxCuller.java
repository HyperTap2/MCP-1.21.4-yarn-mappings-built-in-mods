package net.irisshaders.iris.shadows.frustum;

import net.minecraft.util.math.Box;

public class BoxCuller {
   private final double maxDistance;
   private double minAllowedX;
   private double maxAllowedX;
   private double minAllowedY;
   private double maxAllowedY;
   private double minAllowedZ;
   private double maxAllowedZ;

   public BoxCuller(double maxDistance) {
      this.maxDistance = maxDistance;
   }

   public void setPosition(double cameraX, double cameraY, double cameraZ) {
      this.minAllowedX = cameraX - this.maxDistance;
      this.maxAllowedX = cameraX + this.maxDistance;
      this.minAllowedY = cameraY - this.maxDistance;
      this.maxAllowedY = cameraY + this.maxDistance;
      this.minAllowedZ = cameraZ - this.maxDistance;
      this.maxAllowedZ = cameraZ + this.maxDistance;
   }

   public boolean isCulled(Box aabb) {
      return this.isCulled((float)aabb.minX, (float)aabb.minY, (float)aabb.minZ, (float)aabb.maxX, (float)aabb.maxY, (float)aabb.maxZ);
   }

   public boolean isCulled(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (maxX < this.minAllowedX || minX > this.maxAllowedX) {
         return true;
      } else {
         return !(maxY < this.minAllowedY) && !(minY > this.maxAllowedY) ? maxZ < this.minAllowedZ || minZ > this.maxAllowedZ : true;
      }
   }

   public boolean isCulledSodium(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (maxX < -this.maxDistance || minX > this.maxDistance) {
         return true;
      } else {
         return !(maxY < -this.maxDistance) && !(minY > this.maxDistance) ? maxZ < -this.maxDistance || minZ > this.maxDistance : true;
      }
   }

   @Override
   public String toString() {
      return "Box Culling active; max distance " + this.maxDistance;
   }
}
