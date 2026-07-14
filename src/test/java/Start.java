import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.SodiumBootstrap;
import net.minecraft.client.main.Main;

public class Start {
   public static void main(String[] args) {
      SodiumBootstrap.preLaunch();
      String configuredAssets = System.getenv("assetDirectory");
      Path cachedAssets = Path.of(System.getProperty("user.home"), ".gradle", "caches", "fabric-loom", "assets");
      String assets = configuredAssets != null ? configuredAssets : Files.isDirectory(cachedAssets) ? cachedAssets.toString() : "assets";
      Main.main(concat(new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", "1.21.4-19", "--userProperties", "{}"}, args));
   }

   public static <T> T[] concat(T[] first, T[] second) {
      T[] result = Arrays.copyOf(first, first.length + second.length);
      System.arraycopy(second, 0, result, first.length, second.length);
      return result;
   }
}
