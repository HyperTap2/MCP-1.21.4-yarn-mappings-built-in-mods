package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;

public final class ViaFabricPlusBaseVersionProvider extends BaseVersionProvider {
   public ProtocolVersion getClosestServerProtocol(UserConnection connection) throws Exception {
      return connection.isClientSide() ? ProtocolTranslator.getTargetVersion(connection.getChannel()) : super.getClosestServerProtocol(connection);
   }
}
