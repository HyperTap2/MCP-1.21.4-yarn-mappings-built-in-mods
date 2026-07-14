package net.caffeinemc.mods.lithium.common.block.entity.inventory_comparator_tracking;

import net.caffeinemc.mods.lithium.common.util.DirectionConstants;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class ComparatorTracking {
   private ComparatorTracking() {
   }

   public static void notifyNearbyBlockEntitiesAboutNewComparator(World world, BlockPos pos) {
      BlockPos.Mutable searchPos = new BlockPos.Mutable();
      for (Direction direction : DirectionConstants.HORIZONTAL) {
         for (int offset = 1; offset <= 2; offset++) {
            searchPos.set(pos).move(direction, offset);
            BlockState state = world.getBlockState(searchPos);
            if (state.getBlock() instanceof BlockEntityProvider) {
               BlockEntity blockEntity = world.getBlockEntity(searchPos);
               if (blockEntity instanceof Inventory && blockEntity instanceof ComparatorTracker tracker) {
                  tracker.lithium$onComparatorAdded(direction, offset);
               }
            }
         }
      }
   }

   public static boolean findNearbyComparators(World world, BlockPos pos) {
      if (world == null) {
         return false;
      }
      BlockPos.Mutable searchPos = new BlockPos.Mutable();
      for (Direction direction : DirectionConstants.HORIZONTAL) {
         for (int offset = 1; offset <= 2; offset++) {
            searchPos.set(pos).move(direction, offset);
            if (world.getBlockState(searchPos).isOf(Blocks.COMPARATOR)) {
               return true;
            }
         }
      }
      return false;
   }
}
