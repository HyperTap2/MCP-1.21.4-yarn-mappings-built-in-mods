package net.caffeinemc.mods.lithium.common.shapes;

import net.minecraft.util.math.Box;

public interface VoxelShapeCaster {
   boolean intersects(Box box, double blockX, double blockY, double blockZ);
}
