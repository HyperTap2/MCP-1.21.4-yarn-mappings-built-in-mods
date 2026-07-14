package com.viaversion.viafabricplus.features.interaction.r1_18_2_block_ack_emulation;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.tuple.Pair;

public final class ClientPlayerInteractionManager1_18_2 {
   private final Object2ObjectLinkedOpenHashMap<Pair<BlockPos, Action>, Pair<Vec3d, Vec2f>> unAckedActions = new Object2ObjectLinkedOpenHashMap();

   public void trackPlayerAction(Action action, BlockPos blockPos) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      Vec2f rotation;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_1)) {
         rotation = null;
      } else {
         rotation = new Vec2f(player.getYaw(), player.getPitch());
      }

      this.unAckedActions.put(Pair.of(blockPos, action), Pair.of(player.getPos(), rotation));
   }

   public void handleBlockBreakAck(BlockPos blockPos, BlockState expectedState, Action action, boolean allGood) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null) {
         ClientWorld world = MinecraftClient.getInstance().getNetworkHandler().getWorld();
         Pair<Vec3d, Vec2f> oldPlayerState = (Pair<Vec3d, Vec2f>)this.unAckedActions.remove(Pair.of(blockPos, action));
         BlockState actualState = world.getBlockState(blockPos);
         if ((oldPlayerState == null || !allGood || action != Action.START_DESTROY_BLOCK && actualState != expectedState)
            && (actualState != expectedState || ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2))) {
            world.setBlockState(blockPos, expectedState, 19);
            if (oldPlayerState != null
               && (
                  ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_1)
                     || world == player.getWorld() && player.collidesWithStateAtPos(blockPos, expectedState)
               )) {
               Vec3d oldPlayerPosition = (Vec3d)oldPlayerState.getKey();
               if (oldPlayerState.getValue() != null) {
                  player.updatePositionAndAngles(
                     oldPlayerPosition.x, oldPlayerPosition.y, oldPlayerPosition.z, ((Vec2f)oldPlayerState.getValue()).x, ((Vec2f)oldPlayerState.getValue()).y
                  );
               } else {
                  player.updatePosition(oldPlayerPosition.x, oldPlayerPosition.y, oldPlayerPosition.z);
               }
            }
         }

         while (this.unAckedActions.size() >= 50) {
            ViaFabricPlusImpl.INSTANCE.logger().warn("Too many unacked block actions, dropping {}", this.unAckedActions.firstKey());
            this.unAckedActions.removeFirst();
         }
      }
   }
}
