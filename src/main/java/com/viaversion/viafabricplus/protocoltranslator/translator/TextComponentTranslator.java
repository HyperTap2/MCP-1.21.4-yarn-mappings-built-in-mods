package com.viaversion.viafabricplus.protocoltranslator.translator;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;

public final class TextComponentTranslator {
   private static final UserConnection DUMMY_USER_CONNECTION = ProtocolTranslator.createDummyUserConnection(
      ProtocolTranslator.NATIVE_VERSION, ProtocolVersion.v1_14
   );

   public static Tag via1_14toViaLatest(JsonElement component) {
      try {
         PacketWrapper openScreen = PacketWrapper.create(ClientboundPackets1_14.OPEN_SCREEN, DUMMY_USER_CONNECTION);
         openScreen.write(Types.VAR_INT, 1);
         openScreen.write(Types.VAR_INT, 0);
         openScreen.write(Types.COMPONENT, component);
         openScreen.resetReader();
         openScreen.user().getProtocolInfo().getPipeline().transform(Direction.CLIENTBOUND, State.PLAY, openScreen);
         openScreen.read(Types.VAR_INT);
         openScreen.read(Types.VAR_INT);
         return (Tag)openScreen.read(Types.TAG);
      } catch (Throwable t) {
         ViaFabricPlusImpl.INSTANCE.logger().error("Error converting ViaVersion 1.14 text component to native text component", t);
         return null;
      }
   }
}
