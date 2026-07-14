package net.caffeinemc.mods.lithium.common.util;

import net.minecraft.util.math.Direction;

public final class DirectionConstants {
   public static final Direction[] ALL = Direction.values();
   public static final Direction[] VERTICAL = new Direction[]{Direction.DOWN, Direction.UP};
   public static final Direction[] HORIZONTAL = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
   public static final byte[] HORIZONTAL_OPPOSITE_INDICES = new byte[]{1, 0, 3, 2};

   private DirectionConstants() {
   }
}

