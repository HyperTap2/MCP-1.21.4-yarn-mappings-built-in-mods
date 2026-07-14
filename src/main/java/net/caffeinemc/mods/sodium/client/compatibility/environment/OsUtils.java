package net.caffeinemc.mods.sodium.client.compatibility.environment;

import java.util.Locale;

public class OsUtils {
   private static final OsUtils.OperatingSystem OS = determineOs();

   public static OsUtils.OperatingSystem determineOs() {
      String name = System.getProperty("os.name");
      if (name != null) {
         String normalized = name.toLowerCase(Locale.ROOT);
         if (normalized.startsWith("windows")) {
            return OsUtils.OperatingSystem.WIN;
         }

         if (normalized.startsWith("mac")) {
            return OsUtils.OperatingSystem.MAC;
         }

         if (normalized.startsWith("linux")) {
            return OsUtils.OperatingSystem.LINUX;
         }
      }

      return OsUtils.OperatingSystem.UNKNOWN;
   }

   public static OsUtils.OperatingSystem getOs() {
      return OS;
   }

   public static enum OperatingSystem {
      WIN,
      MAC,
      LINUX,
      UNKNOWN;
   }
}
