package customskinloader.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class MinecraftUtil {
   private static String minecraftMainVersion;

   private MinecraftUtil() {
   }

   public static File getMinecraftDataDir() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client == null ? new File(System.getProperty("user.dir", ".")) : client.runDirectory;
   }

   public static String getMinecraftMainVersion() {
      if (minecraftMainVersion != null) return minecraftMainVersion;
      URL versionFile = ClassLoader.getSystemClassLoader().getResource("version.json");
      if (versionFile != null) {
         try (InputStream stream = versionFile.openStream(); InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            minecraftMainVersion = object.get("name").getAsString();
            return minecraftMainVersion;
         } catch (Exception ignored) {
         }
      }
      return "1.21.4";
   }

   public static String getServerAddress() {
      MinecraftClient client = MinecraftClient.getInstance();
      ServerInfo server = client == null ? null : client.getCurrentServerEntry();
      return server == null ? null : server.address;
   }

   public static String getStandardServerAddress() {
      return HttpUtil0.parseAddress(getServerAddress());
   }

   public static boolean isLanServer() {
      return HttpUtil0.isLanServer(getStandardServerAddress());
   }

   public static String getCredential(GameProfile profile) {
      return profile == null ? null : profile.toString();
   }
}
