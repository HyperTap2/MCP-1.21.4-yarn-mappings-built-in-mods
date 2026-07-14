package net.caffeinemc.mods.lithium.common.hopper;

import net.minecraft.util.math.Direction;

public interface UpdateReceiver {
   void lithium$invalidateCacheOnNeighborUpdate(boolean fromAbove);
   void lithium$invalidateCacheOnUndirectedNeighborUpdate();
   void lithium$invalidateCacheOnNeighborUpdate(Direction fromDirection);
}
