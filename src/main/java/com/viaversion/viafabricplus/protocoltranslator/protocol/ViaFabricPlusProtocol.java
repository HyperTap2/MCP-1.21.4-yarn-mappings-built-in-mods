package com.viaversion.viafabricplus.protocoltranslator.protocol;

import com.viaversion.viafabricplus.features.classic.cpe_extension.CPEAdditions;
import com.viaversion.viafabricplus.features.entity.metadata_handling.WolfHealthTracker1_14_4;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.protocol.storage.BedrockJoinGameTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.AbstractSimpleProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.network.packet.s2c.custom.DebugGameTestAddMarkerCustomPayload;
import net.minecraft.network.packet.s2c.custom.DebugGameTestClearCustomPayload;
import net.minecraft.util.Pair;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public final class ViaFabricPlusProtocol extends AbstractSimpleProtocol {
   public static final ViaFabricPlusProtocol INSTANCE = new ViaFabricPlusProtocol();
   private final Map<String, Pair<ProtocolVersion, ViaFabricPlusProtocol.PacketReader>> payloadDiff = new HashMap<>();

   public ViaFabricPlusProtocol() {
      this.registerMapping(BrandCustomPayload.ID, LegacyProtocolVersion.c0_0_15a_1, wrapper -> wrapper.passthrough(Types.STRING));
      this.registerMapping(DebugGameTestAddMarkerCustomPayload.ID, ProtocolVersion.v1_14, wrapper -> {
         wrapper.passthrough(Types.BLOCK_POSITION1_14);
         wrapper.passthrough(Types.INT);
         wrapper.passthrough(Types.STRING);
         wrapper.passthrough(Types.INT);
      });
      this.registerMapping(DebugGameTestClearCustomPayload.ID, ProtocolVersion.v1_14, wrapper -> {});
   }

   protected void registerPackets() {
      this.registerClientbound(State.PLAY, getCustomPayload().getId(), getCustomPayload().getId(), wrapper -> {
         String channel = Key.namespaced((String)wrapper.passthrough(Types.STRING));
         if (channel.startsWith("minecraft")) {
            ProtocolVersion version = wrapper.user().getProtocolInfo().serverProtocolVersion();
            if (this.payloadDiff.containsKey(channel) && !version.olderThan((ProtocolVersion)this.payloadDiff.get(channel).getLeft())) {
               if (version.olderThanOrEqualTo(ProtocolVersion.v1_20)) {
                  ViaFabricPlusProtocol.PacketReader reader = (ViaFabricPlusProtocol.PacketReader)this.payloadDiff.get(channel).getRight();

                  try {
                     reader.read(wrapper);
                     wrapper.read(Types.REMAINING_BYTES);
                  } catch (Exception ignored) {
                     wrapper.cancel();
                  }
               }
            } else {
               wrapper.cancel();
            }
         }
      });
   }

   public void init(UserConnection connection) {
      super.init(connection);
      CPEAdditions.setSnowing(false);
      ProtocolVersion serverVersion = ProtocolTranslator.getTargetVersion(connection.getChannel());
      if (serverVersion.equals(BedrockProtocolVersion.bedrockLatest)) {
         connection.put(new BedrockJoinGameTracker());
      } else if (serverVersion.olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
         connection.put(new WolfHealthTracker1_14_4());
      }
   }

   private void registerMapping(Id<?> id, ProtocolVersion version, ViaFabricPlusProtocol.PacketReader reader) {
      this.payloadDiff.put(id.id().toString(), new Pair(version, reader));
   }

   public static ServerboundPacketType getSetCreativeModeSlot() {
      return ServerboundPackets1_21_4.SET_CREATIVE_MODE_SLOT;
   }

   public static ClientboundPacketType getCustomPayload() {
      return ClientboundPackets1_21_2.CUSTOM_PAYLOAD;
   }

   @FunctionalInterface
   interface PacketReader {
      void read(PacketWrapper var1);
   }
}
