package com.viaversion.viafabricplus.util;

import com.viaversion.viafabricplus.injection.access.base.IServerInfo;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;

public final class ConnectionUtil {
   public static void connect(String address, ProtocolVersion version) {
      connect(address, address, version);
   }

   public static void connect(String name, String address) {
      connect(name, address, null);
   }

   public static void connect(String name, String address, ProtocolVersion version) {
      ServerAddress serverAddress = ServerAddress.parse(address);
      ServerInfo entry = new ServerInfo(name, serverAddress.getAddress(), ServerType.OTHER);
      if (version != null) {
         ((IServerInfo)entry).viaFabricPlus$forceVersion(version);
      }

      ConnectScreen.connect(MinecraftClient.getInstance().currentScreen, MinecraftClient.getInstance(), serverAddress, entry, false, null);
   }
}
