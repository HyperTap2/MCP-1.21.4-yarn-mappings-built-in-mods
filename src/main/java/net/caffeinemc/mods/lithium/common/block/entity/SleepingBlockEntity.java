package net.caffeinemc.mods.lithium.common.block.entity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.WorldChunk;

public interface SleepingBlockEntity {
   BlockEntityTickInvoker SLEEPING_TICKER = new BlockEntityTickInvoker() {
      @Override public void tick() {}
      @Override public boolean isRemoved() { return false; }
      @Override public BlockPos getPos() { return null; }
      @Override public String getName() { return "<lithium_sleeping>"; }
   };

   WorldChunk.WrappedBlockEntityTickInvoker lithium$getTickWrapper();

   void lithium$setTickWrapper(WorldChunk.WrappedBlockEntityTickInvoker tickWrapper);

   BlockEntityTickInvoker lithium$getSleepingTicker();

   void lithium$setSleepingTicker(BlockEntityTickInvoker sleepingTicker);

   default boolean lithium$startSleeping() {
      if (this.lithium$isSleeping()) {
         return false;
      }
      WorldChunk.WrappedBlockEntityTickInvoker wrapper = this.lithium$getTickWrapper();
      if (wrapper == null) {
         return false;
      }
      this.lithium$setSleepingTicker(wrapper.lithium$getWrapped());
      wrapper.setWrapped(SLEEPING_TICKER);
      return true;
   }

   default void lithium$sleepOnlyCurrentTick() {
      WorldChunk.WrappedBlockEntityTickInvoker wrapper = this.lithium$getTickWrapper();
      if (wrapper == null) {
         return;
      }
      BlockEntityTickInvoker delegate = this.lithium$getSleepingTicker();
      if (delegate == null) {
         delegate = wrapper.lithium$getWrapped();
      }
      BlockEntity blockEntity = (BlockEntity)this;
      wrapper.setWrapped(new SleepUntilTimeBlockEntityTickInvoker(blockEntity, blockEntity.getWorld().getTime() + 1L, delegate));
      this.lithium$setSleepingTicker(null);
   }

   default void lithium$wakeUpNow() {
      BlockEntityTickInvoker sleepingTicker = this.lithium$getSleepingTicker();
      if (sleepingTicker != null) {
         this.lithium$setTicker(sleepingTicker);
         this.lithium$setSleepingTicker(null);
      }
   }

   default void lithium$setTicker(BlockEntityTickInvoker delegate) {
      WorldChunk.WrappedBlockEntityTickInvoker wrapper = this.lithium$getTickWrapper();
      if (wrapper != null) {
         wrapper.setWrapped(delegate);
      }
   }

   default boolean lithium$isSleeping() {
      return this.lithium$getSleepingTicker() != null;
   }
}
