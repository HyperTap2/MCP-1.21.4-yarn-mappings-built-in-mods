package net.caffeinemc.mods.lithium.common.block.redstone;

import net.caffeinemc.mods.lithium.common.util.DirectionConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RedstoneController;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

public class RedstoneWirePowerCalculations {
   private static final int MIN = 0;
   private static final int MAX = 15;
   private static final int MAX_WIRE = 14;

   public static int getNeighborBlockSignal(Block wireBlock, RedstoneController evaluator, World level, BlockPos pos) {
      return getNeighborSignal(wireBlock, evaluator, level, pos, false, true);
   }

   public static int getNeighborWireSignal(Block wireBlock, RedstoneController evaluator, World level, BlockPos pos) {
      return getNeighborSignal(wireBlock, evaluator, level, pos, true, false);
   }

   public static int getNeighborSignal(Block wireBlock, RedstoneController evaluator, World level, BlockPos pos, boolean ignoreNonWires, boolean ignoreWires) {
      int signal = 0;
      WorldChunk chunk = level.getWorldChunk(pos);
      if (!ignoreNonWires) {
         for (Direction dir : DirectionConstants.VERTICAL) {
            BlockPos side = pos.offset(dir);
            BlockState neighbor = chunk.getBlockState(side);
            if (!neighbor.isAir() && !neighbor.isOf(wireBlock)) {
               signal = Math.max(signal, getSignalFromVertical(level, side, neighbor, dir, wireBlock));
               if (signal >= 15) {
                  return 15;
               }
            }
         }
      }

      boolean checkWiresAbove = false;
      if (!ignoreWires) {
         BlockPos above = pos.up();
         checkWiresAbove = !chunk.getBlockState(above).isSolidBlock(level, above);
      }

      for (Direction dir : DirectionConstants.HORIZONTAL) {
         signal = Math.max(signal, getSignalFromSide(level, pos.offset(dir), dir, checkWiresAbove, ignoreNonWires, ignoreWires, wireBlock, evaluator));
         if (signal >= 15) {
            return 15;
         }
      }

      return signal;
   }

   private static int getSignalFromVertical(World level, BlockPos pos, BlockState state, Direction toDir, Block wireBlock) {
      int signal = state.getWeakRedstonePower(level, pos, toDir);
      if (signal >= 15) {
         return 15;
      } else {
         return state.isSolidBlock(level, pos) ? Math.max(signal, getDirectSignalTo(level, pos, toDir.getOpposite(), wireBlock)) : signal;
      }
   }

   private static int getSignalFromSide(
      World level,
      BlockPos pos,
      Direction toDir,
      boolean checkWiresAbove,
      boolean ignoreNonWires,
      boolean ignoreWires,
      Block wireBlock,
      RedstoneController evaluator
   ) {
      WorldChunk chunk = level.getWorldChunk(pos);
      BlockState state = chunk.getBlockState(pos);
      if (state.isOf(wireBlock)) {
         return !ignoreWires ? evaluator.getWirePowerAt(pos, state) - 1 : 0;
      }

      int signal = 0;
      if (!ignoreNonWires) {
         signal = state.getWeakRedstonePower(level, pos, toDir);
         if (signal >= 15) {
            return 15;
         }
      }

      if (state.isSolidBlock(level, pos)) {
         if (!ignoreNonWires) {
            signal = Math.max(signal, getDirectSignalTo(level, pos, toDir.getOpposite(), wireBlock));
            if (signal >= 15) {
               return 15;
            }
         }

         if (!ignoreWires && checkWiresAbove && signal < 14) {
            BlockPos above = pos.up();
            BlockState aboveState = chunk.getBlockState(above);
            if (aboveState.isOf(wireBlock)) {
               signal = Math.max(signal, evaluator.getWirePowerAt(above, aboveState) - 1);
            }
         }
      } else if (!ignoreWires && signal < 14) {
         BlockPos below = pos.down();
         BlockState belowState = chunk.getBlockState(below);
         if (belowState.isOf(wireBlock)) {
            signal = Math.max(signal, evaluator.getWirePowerAt(below, belowState) - 1);
         }
      }

      return signal;
   }

   private static int getDirectSignalTo(World level, BlockPos pos, Direction ignore, Block wireBlock) {
      int signal = 0;

      for (Direction dir : DirectionConstants.ALL) {
         if (dir != ignore) {
            BlockPos side = pos.offset(dir);
            BlockState neighbor = level.getBlockState(side);
            if (!neighbor.isAir() && !neighbor.isOf(wireBlock)) {
               signal = Math.max(signal, neighbor.getStrongRedstonePower(level, side, dir));
               if (signal >= 15) {
                  return 15;
               }
            }
         }
      }

      return signal;
   }
}

