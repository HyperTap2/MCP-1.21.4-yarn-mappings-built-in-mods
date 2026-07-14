package net.caffeinemc.mods.lithium.common.world.blockview;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public record SingleBlockBlockView(BlockState state, BlockPos blockPos) implements BlockView {
   public static SingleBlockBlockView of(BlockState state, BlockPos blockPos) {
      return new SingleBlockBlockView(state, blockPos.toImmutable());
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      if (pos.equals(this.blockPos)) {
         return this.state;
      }
      throw SingleBlockViewException.INSTANCE;
   }

   @Override
   public FluidState getFluidState(BlockPos pos) {
      if (pos.equals(this.blockPos)) {
         return this.state.getFluidState();
      }
      throw SingleBlockViewException.INSTANCE;
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      throw SingleBlockViewException.INSTANCE;
   }

   @Override
   public int getHeight() {
      throw SingleBlockViewException.INSTANCE;
   }

   @Override
   public int getBottomY() {
      throw SingleBlockViewException.INSTANCE;
   }

   public static final class SingleBlockViewException extends RuntimeException {
      private static final SingleBlockViewException INSTANCE = new SingleBlockViewException();

      private SingleBlockViewException() {
         super(null, null, false, false);
      }
   }
}
