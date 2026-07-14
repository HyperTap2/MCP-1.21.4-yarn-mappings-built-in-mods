package net.caffeinemc.mods.lithium.common.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

public record SleepUntilTimeBlockEntityTickInvoker(
   BlockEntity blockEntity, long sleepUntilTickExclusive, BlockEntityTickInvoker delegate
) implements BlockEntityTickInvoker {
   @Override
   public void tick() {
      if (this.blockEntity.getWorld().getTime() >= this.sleepUntilTickExclusive) {
         ((SleepingBlockEntity)this.blockEntity).lithium$setTicker(this.delegate);
         this.delegate.tick();
      }
   }

   @Override public boolean isRemoved() { return this.blockEntity.isRemoved(); }
   @Override public BlockPos getPos() { return this.blockEntity.getPos(); }
   @Override public String getName() { return BlockEntityType.getId(this.blockEntity.getType()).toString(); }
}
