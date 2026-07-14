package com.viaversion.viafabricplus.features.classic.cpe_extension;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.lenni0451.reflect.Enums;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.Protocolc0_30cpeToc0_28_30;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.data.ClassicProtocolExtension;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.packet.ClientboundPacketsc0_30cpe;

public final class CPEAdditions {
   public static final List<ClassicProtocolExtension> ALLOWED_EXTENSIONS = new ArrayList<>();
   public static final Map<Integer, ClientboundPacketsc0_30cpe> CUSTOM_PACKETS = new HashMap<>();
   public static ClientboundPacketsc0_30cpe EXT_WEATHER_TYPE;
   private static boolean snowing = false;

   public static void init() {
      allowExtension(ClassicProtocolExtension.ENV_WEATHER_TYPE);
   }

   public static void registerPackets() {
      if (EXT_WEATHER_TYPE != null) {
         return;
      }

      EXT_WEATHER_TYPE = createNewPacket(ClassicProtocolExtension.ENV_WEATHER_TYPE, 31, (user, buf) -> buf.readByte());
      Protocolc0_30cpeToc0_28_30 protocol = Via.getManager().getProtocolManager().getProtocol(Protocolc0_30cpeToc0_28_30.class);
      if (protocol == null) {
         throw new IllegalStateException("ViaLegacy did not register the Classic CPE protocol");
      }
      protocol.registerClientbound(State.PLAY, EXT_WEATHER_TYPE.getId(), -1, wrapper -> {
         wrapper.cancel();
         byte weatherType = wrapper.read(Types.BYTE);
         PacketWrapper changeRainState = PacketWrapper.create(ClientboundPackets1_19_4.GAME_EVENT, wrapper.user());
         changeRainState.write(Types.UNSIGNED_BYTE, (short)(weatherType == 0 ? 1 : 2));
         changeRainState.write(Types.FLOAT, 0.0F);
         changeRainState.send(Protocol1_19_3To1_19_4.class);
         if (weatherType == 1 || weatherType == 2) {
            PacketWrapper changeRainType = PacketWrapper.create(ClientboundPackets1_19_4.GAME_EVENT, wrapper.user());
            changeRainType.write(Types.UNSIGNED_BYTE, (short)7);
            changeRainType.write(Types.FLOAT, 1.0F);
            changeRainType.send(Protocol1_19_3To1_19_4.class);
         }
         setSnowing(weatherType == 2);
      }, true);
   }

   public static boolean isSnowing() {
      return ProtocolTranslator.getTargetVersion().equals(LegacyProtocolVersion.c0_30cpe) && snowing;
   }

   public static void setSnowing(boolean snowing) {
      CPEAdditions.snowing = snowing;
   }

   public static void allowExtension(ClassicProtocolExtension classicProtocolExtension) {
      ALLOWED_EXTENSIONS.add(classicProtocolExtension);
      classicProtocolExtension.getSupportedVersions().add(1);
   }

   public static ClientboundPacketsc0_30cpe createNewPacket(
      ClassicProtocolExtension classicProtocolExtension, int packetId, BiConsumer<UserConnection, ByteBuf> packetSplitter
   ) {
      ClientboundPacketsc0_30cpe packet = (ClientboundPacketsc0_30cpe)Enums.newInstance(
         ClientboundPacketsc0_30cpe.class,
         classicProtocolExtension.getName(),
         ClassicProtocolExtension.values().length,
         new Class[]{int.class, BiConsumer.class},
         new Object[]{packetId, packetSplitter}
      );
      Enums.addEnumInstance(ClientboundPacketsc0_30cpe.class, packet);
      try {
         Field registryField = ClientboundPacketsc0_30cpe.class.getDeclaredField("REGISTRY");
         registryField.setAccessible(true);
         ClientboundPacketsc0_30cpe[] registry = (ClientboundPacketsc0_30cpe[])registryField.get(null);
         registry[packetId] = packet;
      } catch (ReflectiveOperationException exception) {
         throw new IllegalStateException("Unable to extend the Classic CPE packet registry", exception);
      }
      CUSTOM_PACKETS.put(packetId, packet);
      return packet;
   }
}
