package dev.isxander.zoomify;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.item.Items;

public final class Zoomify {
   private static double currentDivisor = 1.0;
   private static double requestedDivisor;
   private static long lastUpdateNanos = System.nanoTime();
   private static boolean toggled;
   private static boolean keyWasDown;
   private static boolean secondaryToggled;
   private static boolean secondaryKeyWasDown;

   private Zoomify() {
   }

   public static void onGameFinishedLoading() {
      ZoomifyConfig.get();
   }

   public static boolean isZooming() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.options == null || client.player == null) return false;
      boolean keyDown = client.options.zoomKey.isPressed();
      ZoomifyConfig config = ZoomifyConfig.get();
      if (config.toggleMode && keyDown && !keyWasDown) toggled = !toggled;
      keyWasDown = keyDown;
      boolean active = config.toggleMode ? toggled : keyDown;
      if (config.spyglassMode == ZoomifyConfig.SpyglassMode.OVERRIDE && client.player.isUsingSpyglass()) active = true;
      if (config.spyglassMode == ZoomifyConfig.SpyglassMode.ONLY_WHILE_HOLDING) active &= client.player.isUsingSpyglass();
      return active && (config.allowThirdPerson || client.options.getPerspective().isFirstPerson());
   }

   public static boolean isSecondaryZooming() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.options == null || client.player == null) return false;
      boolean down = client.options.secondaryZoomKey.isPressed();
      if (down && !secondaryKeyWasDown) secondaryToggled = !secondaryToggled;
      secondaryKeyWasDown = down;
      return secondaryToggled;
   }

   public static float applyFov(float fov) {
      ZoomifyConfig config = ZoomifyConfig.get();
      MinecraftClient client = MinecraftClient.getInstance();
      boolean active = isZooming();
      boolean secondary = isSecondaryZooming();
      if (requestedDivisor == 0.0) requestedDivisor = config.zoomDivisor;
      double target = (active ? requestedDivisor : 1.0) * (secondary ? config.secondaryZoomDivisor : 1.0);
      long now = System.nanoTime();
      double elapsed = Math.min((now - lastUpdateNanos) / 1_000_000_000.0, 0.25);
      lastUpdateNanos = now;
      if (config.transitionSeconds <= 0.0) {
         currentDivisor = target;
      } else {
         currentDivisor = MathHelper.lerp(1.0 - Math.exp(-elapsed * 6.0 / config.transitionSeconds), currentDivisor, target);
      }
      if (!active && !secondary && Math.abs(currentDivisor - 1.0) < 0.001) {
         currentDivisor = 1.0;
         if (config.resetZoomOnRelease) requestedDivisor = config.zoomDivisor;
      }
      return (float)(fov / Math.max(1.0, currentDivisor));
   }

   public static boolean onScroll(double amount) {
      ZoomifyConfig config = ZoomifyConfig.get();
      if (!isZooming() || !config.useScrollWheel || amount == 0.0) return false;
      if (requestedDivisor == 0.0) requestedDivisor = config.zoomDivisor;
      requestedDivisor = Math.clamp(requestedDivisor + Math.signum(amount) * config.scrollStep, config.minimumDivisor, config.maximumDivisor);
      return true;
   }

   public static double mouseScale() {
      return currentDivisor <= 1.001 ? 1.0 : Math.max(ZoomifyConfig.get().zoomedSensitivity, 1.0 / currentDivisor);
   }

   public static boolean shouldHideHand() {
      return ZoomifyConfig.get().hideHand && currentDivisor > 1.01;
   }

   public static boolean shouldUseCinematicCamera() {
      return ZoomifyConfig.get().cinematicCamera && currentDivisor > 1.01;
   }

   public static float applyHandFov(float fov) {
      return ZoomifyConfig.get().affectHandFov ? fov : (float)(fov * Math.max(1.0, currentDivisor));
   }

   public static float viewBobbingScale() {
      if (!ZoomifyConfig.get().relativeViewBobbing) return 1.0F;
      return (float)(1.0 / MathHelper.lerp(0.2, 1.0, Math.max(1.0, currentDivisor)));
   }

   public static float modifySpyglassMultiplier(float vanillaMultiplier) {
      return ZoomifyConfig.get().spyglassMode == ZoomifyConfig.SpyglassMode.COMBINE ? vanillaMultiplier : 1.0F;
   }

   public static boolean shouldRenderSpyglassOverlay(boolean usingSpyglass) {
      MinecraftClient client = MinecraftClient.getInstance();
      return switch (ZoomifyConfig.get().overlayVisibility) {
         case NEVER -> false;
         case HOLDING -> usingSpyglass;
         case CARRYING -> usingSpyglass || client.player != null && client.player.getInventory().contains(stack -> stack.isOf(Items.SPYGLASS));
         case ALWAYS -> true;
      };
   }

   public static boolean shouldPlaySpyglassSound() {
      return switch (ZoomifyConfig.get().soundMode) {
         case NEVER -> false;
         case ALWAYS -> true;
         case WITH_OVERLAY -> ZoomifyConfig.get().overlayVisibility != ZoomifyConfig.OverlayVisibility.NEVER;
      };
   }

   public static boolean shouldHideHud() {
      return ZoomifyConfig.get().secondaryHideHud && isSecondaryZooming();
   }
}
