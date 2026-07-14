package net.minecraft.client.network;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public class WorldLoadingState {
   private final ClientPlayerEntity player;
   private final ClientWorld world;
   private final WorldRenderer renderer;
   public WorldLoadingState.Step currentStep = WorldLoadingState.Step.WAITING_FOR_SERVER;

   public WorldLoadingState(ClientPlayerEntity player, ClientWorld world, WorldRenderer renderer) {
      this.player = player;
      this.world = world;
      this.renderer = renderer;
   }

   public void tick() {
      switch (this.currentStep) {
         case WAITING_FOR_PLAYER_CHUNK:
            BlockPos blockPos = BlockPos.ofFloored(this.player.getX(), this.player.getEyeY(), this.player.getZ());
            boolean bl = this.world.isOutOfHeightLimit(blockPos.getY());
            if (bl || this.renderer.isRenderingReady(blockPos) || this.player.isSpectator() || !this.player.isAlive()) {
               this.currentStep = WorldLoadingState.Step.LEVEL_READY;
            }
         case WAITING_FOR_SERVER:
         case LEVEL_READY:
      }
   }

   public boolean isReady() {
      return this.currentStep == WorldLoadingState.Step.LEVEL_READY;
   }

   public void handleChunksComingPacket() {
      if (this.currentStep == WorldLoadingState.Step.WAITING_FOR_SERVER) {
         this.currentStep = WorldLoadingState.Step.WAITING_FOR_PLAYER_CHUNK;
      }
   }

   public enum Step {
      WAITING_FOR_SERVER,
      WAITING_FOR_PLAYER_CHUNK,
      LEVEL_READY;
   }
}
