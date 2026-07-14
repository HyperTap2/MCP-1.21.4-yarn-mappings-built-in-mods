package net.caffeinemc.mods.sodium.client.util;

import java.util.Arrays;
import net.minecraft.util.math.Direction;

public class DirectionUtil {
   public static final Direction[] ALL_DIRECTIONS = Direction.values();
   public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private static final Direction[] OPPOSITE_DIRECTIONS = Arrays.stream(ALL_DIRECTIONS).map(Direction::getOpposite).toArray(Direction[]::new);

   public static Direction getOpposite(Direction dir) {
      return OPPOSITE_DIRECTIONS[dir.ordinal()];
   }
}
