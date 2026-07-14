package com.viaversion.viafabricplus.features.world.disable_sequencing;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public final class PendingUpdateManager1_18_2 extends PendingUpdateManager {
   public void addPendingUpdate(BlockPos pos, BlockState state, ClientPlayerEntity player) {
   }

   public boolean hasPendingUpdate(BlockPos pos, BlockState state) {
      return false;
   }

   public void processPendingUpdates(int maxProcessableSequence, ClientWorld world) {
   }

   public PendingUpdateManager incrementSequence() {
      return this;
   }

   public void close() {
   }

   public int getSequence() {
      return 0;
   }

   public boolean hasPendingSequence() {
      return false;
   }
}
