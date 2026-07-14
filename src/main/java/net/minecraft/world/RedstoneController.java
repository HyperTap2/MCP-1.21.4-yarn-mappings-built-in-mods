package net.minecraft.world;

import net.caffeinemc.mods.lithium.common.block.redstone.RedstoneWirePowerCalculations;
import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

public abstract class RedstoneController {
   protected final RedstoneWireBlock wire;

   protected RedstoneController(RedstoneWireBlock wire) {
      this.wire = wire;
   }

   public abstract void update(World world, BlockPos pos, BlockState state, @Nullable WireOrientation orientation, boolean blockAdded);

   protected int getStrongPowerAt(World world, BlockPos pos) {
      return this.wire.getStrongPower(world, pos);
   }

   public int getWirePowerAt(BlockPos world, BlockState pos) {
      return pos.isOf(this.wire) ? pos.get(RedstoneWireBlock.POWER) : 0;
   }

   protected int calculateWirePowerAt(World world, BlockPos pos) {
      return RedstoneWirePowerCalculations.getNeighborWireSignal(this.wire, this, world, pos);
   }
}
