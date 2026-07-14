package dev.tr7zw.entityculling;

public final class CullingState {
   private volatile long forcedVisibleUntil;
   private volatile boolean culled;
   private volatile boolean outOfCamera;

   public void setTimeout() {
      this.forcedVisibleUntil = System.currentTimeMillis() + 1000L;
   }

   public boolean isForcedVisible() {
      return this.forcedVisibleUntil > System.currentTimeMillis();
   }

   public void setCulled(boolean culled) {
      this.culled = culled;
      if (!culled) {
         this.setTimeout();
      }
   }

   public boolean isCulled() {
      return this.culled;
   }

   public void setOutOfCamera(boolean outOfCamera) {
      this.outOfCamera = outOfCamera;
   }

   public boolean isOutOfCamera() {
      return this.outOfCamera;
   }
}
