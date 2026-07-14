package com.viaversion.viafabricplus.protocoltranslator.translator;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public final class BlockStateTranslator {
   private static final UserConnection DUMMY_USER_CONNECTION = ProtocolTranslator.createDummyUserConnection(
      ProtocolTranslator.NATIVE_VERSION, ProtocolVersion.v1_18_2
   );

   public static BlockState via1_18_2toMc(int blockStateId) {
      try {
         PacketWrapper levelEvent = PacketWrapper.create(ClientboundPackets1_18.LEVEL_EVENT, DUMMY_USER_CONNECTION);
         levelEvent.write(Types.INT, 2001);
         levelEvent.write(Types.BLOCK_POSITION1_14, new BlockPosition(0, 0, 0));
         levelEvent.write(Types.INT, blockStateId);
         levelEvent.write(Types.BOOLEAN, false);
         levelEvent.resetReader();
         levelEvent.user().getProtocolInfo().getPipeline().transform(Direction.CLIENTBOUND, State.PLAY, levelEvent);
         levelEvent.read(Types.INT);
         levelEvent.read(Types.BLOCK_POSITION1_14);
         return Block.getStateFromRawId((Integer)levelEvent.read(Types.INT));
      } catch (Throwable t) {
         ViaFabricPlusImpl.INSTANCE.logger().error("Error converting ViaVersion 1.18.2 block state to native block state", t);
         return Blocks.AIR.getDefaultState();
      }
   }
}
