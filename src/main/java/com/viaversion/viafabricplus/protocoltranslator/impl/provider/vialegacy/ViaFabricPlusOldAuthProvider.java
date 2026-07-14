package com.viaversion.viafabricplus.protocoltranslator.impl.provider.vialegacy;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.AuthenticationSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;

public final class ViaFabricPlusOldAuthProvider extends OldAuthProvider {
   public void sendAuthRequest(UserConnection connection, String serverId) {
      if ((Boolean)AuthenticationSettings.INSTANCE.verifySessionForOnlineModeServers.getValue()) {
         try {
            MinecraftClient client = MinecraftClient.getInstance();
            client.getSessionService().joinServer(client.getSession().getUuidOrNull(), client.getSession().getAccessToken(), serverId);
         } catch (Exception e) {
            ((ClientConnection)connection.getChannel().attr(ProtocolTranslator.CLIENT_CONNECTION_ATTRIBUTE_KEY).get())
               .disconnect(ChatUtil.prefixText(Text.translatable("betacraft.viafabricplus.failed_to_verify_session")));
            ViaFabricPlusImpl.INSTANCE.logger().error("Error occurred while calling join server to verify session", e);
         }
      }
   }
}
