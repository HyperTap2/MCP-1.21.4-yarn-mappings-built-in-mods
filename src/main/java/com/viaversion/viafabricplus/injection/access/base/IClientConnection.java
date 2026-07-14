package com.viaversion.viafabricplus.injection.access.base;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

public interface IClientConnection {
   void viaFabricPlus$setupPreNettyDecryption();

   ProtocolVersion viaFabricPlus$getTargetVersion();

   void viaFabricPlus$setTargetVersion(ProtocolVersion var1);

   UserConnection viaFabricPlus$getUserConnection();

   void viaFabricPlus$setUserConnection(UserConnection var1);
}
