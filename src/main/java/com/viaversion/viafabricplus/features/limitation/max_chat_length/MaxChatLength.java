package com.viaversion.viafabricplus.features.limitation.max_chat_length;

import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.data.ClassicProtocolExtension;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.storage.ExtensionProtocolMetadataStorage;

public final class MaxChatLength {
   public static int getChatLength() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
         ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
         ExtensionProtocolMetadataStorage extensionProtocol = (ExtensionProtocolMetadataStorage)((IClientConnection)handler.getConnection())
            .viaFabricPlus$getUserConnection()
            .get(ExtensionProtocolMetadataStorage.class);
         return extensionProtocol != null && extensionProtocol.hasServerExtension(ClassicProtocolExtension.LONGER_MESSAGES, new int[0])
            ? 65534
            : 64 - (MinecraftClient.getInstance().getSession().getUsername().length() + 2);
      } else if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         return 512;
      } else {
         return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_9_3) ? 100 : 256;
      }
   }
}
