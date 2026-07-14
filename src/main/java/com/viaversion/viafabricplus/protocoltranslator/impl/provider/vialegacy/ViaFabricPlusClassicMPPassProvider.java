package com.viaversion.viafabricplus.protocoltranslator.impl.provider.vialegacy;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.settings.impl.AuthenticationSettings;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import de.florianmichael.classic4j.BetaCraftHandler;
import net.raphimc.vialegacy.protocol.classic.c0_28_30toa1_0_15.provider.ClassicMPPassProvider;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;

public final class ViaFabricPlusClassicMPPassProvider extends ClassicMPPassProvider {
   public static String classicubeMPPass;

   public String getMpPass(UserConnection connection) {
      if (classicubeMPPass != null) {
         String mpPass = classicubeMPPass;
         classicubeMPPass = null;
         return mpPass;
      }

      if ((Boolean)AuthenticationSettings.INSTANCE.useBetaCraftAuthentication.getValue()) {
         BetaCraftHandler.authenticate(serverId -> {
            try {
               ((OldAuthProvider)Via.getManager().getProviders().get(OldAuthProvider.class)).sendAuthRequest(connection, serverId);
            } catch (Throwable e) {
               ViaFabricPlusImpl.INSTANCE.logger().error("Error occurred while verifying session", e);
            }
         }, throwable -> ViaFabricPlusImpl.INSTANCE.logger().error("Error occurred while requesting the MP-Pass to verify session", throwable));
      }

      return super.getMpPass(connection);
   }
}
