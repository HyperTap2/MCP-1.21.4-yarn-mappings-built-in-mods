package com.viaversion.viafabricplus.protocoltranslator.impl.platform;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLViaConfig;
import com.viaversion.vialoader.impl.platform.ViaVersionPlatformImpl;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonObject;
import java.io.File;

public final class ViaFabricPlusViaVersionPlatformImpl extends ViaVersionPlatformImpl {
   public ViaFabricPlusViaVersionPlatformImpl(File rootFolder) {
      super(rootFolder);
   }

   public String getPlatformName() {
      return "ViaFabricPlus";
   }

   public String getPlatformVersion() {
      return ViaFabricPlusImpl.INSTANCE.getVersion();
   }

   protected AbstractViaConfig createConfig() {
      return new ViaFabricPlusVLViaConfig(new File(this.getDataFolder(), "viaversion.yml"), this.getLogger());
   }

   public JsonObject getDump() {
      JsonObject platformDump = new JsonObject();
      platformDump.addProperty("impl_version", ViaFabricPlusImpl.INSTANCE.getImplVersion());
      platformDump.addProperty("native_version", ProtocolTranslator.NATIVE_VERSION.toString());
      platformDump.addProperty("target_version", ProtocolTranslator.getTargetVersion().toString());
      JsonArray mods = new JsonArray();
      JsonObject client = new JsonObject();
      client.addProperty("id", "client-mcp");
      client.addProperty("name", "Client MCP");
      client.addProperty("version", "1.0.0");
      mods.add(client);
      platformDump.add("mods", mods);
      return platformDump;
   }

   public File getDataFolder() {
      return ViaFabricPlusImpl.INSTANCE.rootPath().toFile();
   }
}
