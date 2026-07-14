package net.caffeinemc.mods.lithium.common.world.listeners;

import java.util.WeakHashMap;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;

public final class WorldBorderPositionListenerMulti implements WorldBorderListener {
   private final WeakHashMap<WorldBorderListenerOnce, Boolean> listeners = new WeakHashMap<>();

   public void add(WorldBorderListenerOnce listener) {
      this.listeners.put(listener, Boolean.TRUE);
   }

   public void onAreaReplaced(WorldBorder border) {
      for (WorldBorderListenerOnce listener : this.listeners.keySet()) listener.lithium$onAreaReplaced(border);
      this.listeners.clear();
   }

   @Override
   public void onSizeChange(WorldBorder border, double size) {
      for (WorldBorderListenerOnce listener : this.listeners.keySet()) listener.onSizeChange(border, size);
      this.listeners.clear();
   }

   @Override
   public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
      for (WorldBorderListenerOnce listener : this.listeners.keySet()) listener.onInterpolateSize(border, fromSize, toSize, time);
      this.listeners.clear();
   }

   @Override
   public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
      for (WorldBorderListenerOnce listener : this.listeners.keySet()) listener.onCenterChanged(border, centerX, centerZ);
      this.listeners.clear();
   }

   @Override public void onWarningTimeChanged(WorldBorder border, int warningTime) {}
   @Override public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {}
   @Override public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {}
   @Override public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {}
}
