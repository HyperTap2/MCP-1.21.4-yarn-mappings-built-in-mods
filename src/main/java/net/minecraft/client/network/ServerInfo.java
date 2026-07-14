package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.injection.access.base.IServerInfo;
import com.viaversion.viafabricplus.save.impl.SettingsSave;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.ServerMetadata.Players;
import net.minecraft.text.Text;
import net.minecraft.util.PngMetadata;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ServerInfo implements IServerInfo {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_FAVICON_SIZE = 1024;
   public String name;
   public String address;
   public Text playerCountLabel;
   public Text label;
   @Nullable
   public Players players;
   public long ping;
   public int protocolVersion = SharedConstants.getGameVersion().getProtocolVersion();
   public Text version = Text.literal(SharedConstants.getGameVersion().getName());
   public List<Text> playerListSummary = Collections.emptyList();
   private ServerInfo.ResourcePackPolicy resourcePackPolicy = ServerInfo.ResourcePackPolicy.PROMPT;
   @Nullable
   private byte[] favicon;
   private ServerInfo.ServerType serverType;
   private ServerInfo.Status status = ServerInfo.Status.INITIAL;
   private ProtocolVersion viaFabricPlus$forcedVersion;
   private boolean viaFabricPlus$passedDirectConnectScreen;
   private ProtocolVersion viaFabricPlus$translatingVersion;

   public ServerInfo(String name, String address, ServerInfo.ServerType serverType) {
      this.name = name;
      this.address = address;
      this.serverType = serverType;
   }

   public NbtCompound toNbt() {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("name", this.name);
      nbtCompound.putString("ip", this.address);
      if (this.favicon != null) {
         nbtCompound.putString("icon", Base64.getEncoder().encodeToString(this.favicon));
      }

      if (this.resourcePackPolicy == ServerInfo.ResourcePackPolicy.ENABLED) {
         nbtCompound.putBoolean("acceptTextures", true);
      } else if (this.resourcePackPolicy == ServerInfo.ResourcePackPolicy.DISABLED) {
         nbtCompound.putBoolean("acceptTextures", false);
      }

      if (this.viaFabricPlus$forcedVersion != null) {
         nbtCompound.putString("viafabricplus_forcedversion", this.viaFabricPlus$forcedVersion.getName());
      }

      return nbtCompound;
   }

   public ServerInfo.ResourcePackPolicy getResourcePackPolicy() {
      return this.resourcePackPolicy;
   }

   public void setResourcePackPolicy(ServerInfo.ResourcePackPolicy resourcePackPolicy) {
      this.resourcePackPolicy = resourcePackPolicy;
   }

   public static ServerInfo fromNbt(NbtCompound root) {
      ServerInfo serverInfo = new ServerInfo(root.getString("name"), root.getString("ip"), ServerInfo.ServerType.OTHER);
      if (root.contains("icon", 8)) {
         try {
            byte[] bs = Base64.getDecoder().decode(root.getString("icon"));
            serverInfo.setFavicon(validateFavicon(bs));
         } catch (IllegalArgumentException illegalArgumentException) {
            LOGGER.warn("Malformed base64 server icon", illegalArgumentException);
         }
      }

      if (root.contains("acceptTextures", 99)) {
         if (root.getBoolean("acceptTextures")) {
            serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.ENABLED);
         } else {
            serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
         }
      } else {
         serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.PROMPT);
      }

      if (root.contains("viafabricplus_forcedversion")) {
         ProtocolVersion version = SettingsSave.protocolVersionByName(root.getString("viafabricplus_forcedversion"));
         if (version != null) {
            serverInfo.viaFabricPlus$forceVersion(version);
         }
      }

      return serverInfo;
   }

   @Nullable
   public byte[] getFavicon() {
      return this.favicon;
   }

   public void setFavicon(@Nullable byte[] favicon) {
      this.favicon = favicon;
   }

   public boolean isLocal() {
      return this.serverType == ServerInfo.ServerType.LAN;
   }

   public boolean isRealm() {
      return this.serverType == ServerInfo.ServerType.REALM;
   }

   public ServerInfo.ServerType getServerType() {
      return this.serverType;
   }

   public void copyFrom(ServerInfo serverInfo) {
      this.address = serverInfo.address;
      this.name = serverInfo.name;
      this.favicon = serverInfo.favicon;
      this.viaFabricPlus$forcedVersion = serverInfo.viaFabricPlus$forcedVersion;
   }

   @Override
   public ProtocolVersion viaFabricPlus$forcedVersion() {
      return this.viaFabricPlus$forcedVersion;
   }

   @Override
   public void viaFabricPlus$forceVersion(ProtocolVersion version) {
      this.viaFabricPlus$forcedVersion = version;
   }

   @Override
   public boolean viaFabricPlus$passedDirectConnectScreen() {
      return this.viaFabricPlus$passedDirectConnectScreen;
   }

   @Override
   public void viaFabricPlus$passDirectConnectScreen(boolean state) {
      this.viaFabricPlus$passedDirectConnectScreen = state;
   }

   @Override
   public ProtocolVersion viaFabricPlus$translatingVersion() {
      return this.viaFabricPlus$translatingVersion;
   }

   @Override
   public void viaFabricPlus$setTranslatingVersion(ProtocolVersion version) {
      this.viaFabricPlus$translatingVersion = version;
   }

   public void copyWithSettingsFrom(ServerInfo serverInfo) {
      this.copyFrom(serverInfo);
      this.setResourcePackPolicy(serverInfo.getResourcePackPolicy());
      this.serverType = serverInfo.serverType;
   }

   public ServerInfo.Status getStatus() {
      return this.status;
   }

   public void setStatus(ServerInfo.Status status) {
      this.status = status;
   }

   @Nullable
   public static byte[] validateFavicon(@Nullable byte[] favicon) {
      if (favicon != null) {
         try {
            PngMetadata pngMetadata = PngMetadata.fromBytes(favicon);
            if (pngMetadata.width() <= 1024 && pngMetadata.height() <= 1024) {
               return favicon;
            }
         } catch (IOException iOException) {
            LOGGER.warn("Failed to decode server icon", iOException);
         }
      }

      return null;
   }

   public enum ResourcePackPolicy {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      private final Text name;

      ResourcePackPolicy(final String name) {
         this.name = Text.translatable("addServer.resourcePack." + name);
      }

      public Text getName() {
         return this.name;
      }
   }

   public enum ServerType {
      LAN,
      REALM,
      OTHER;
   }

   public enum Status {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;
   }
}
