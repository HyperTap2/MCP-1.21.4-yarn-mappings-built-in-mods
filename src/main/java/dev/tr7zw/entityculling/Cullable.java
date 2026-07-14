package dev.tr7zw.entityculling;

public interface Cullable {
   CullingState entityCulling$getState();

   default void entityCulling$setTimeout() {
      this.entityCulling$getState().setTimeout();
   }

   default boolean entityCulling$isForcedVisible() {
      return this.entityCulling$getState().isForcedVisible();
   }

   default void entityCulling$setCulled(boolean culled) {
      this.entityCulling$getState().setCulled(culled);
   }

   default boolean entityCulling$isCulled() {
      return EntityCullingManager.getInstance().isEnabled() && this.entityCulling$getState().isCulled();
   }

   default void entityCulling$setOutOfCamera(boolean outOfCamera) {
      this.entityCulling$getState().setOutOfCamera(outOfCamera);
   }

   default boolean entityCulling$isOutOfCamera() {
      return EntityCullingManager.getInstance().isEnabled() && this.entityCulling$getState().isOutOfCamera();
   }
}
