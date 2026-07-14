package net.caffeinemc.mods.sodium.client;

import java.io.IOException;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SodiumClientMod {
   private static SodiumGameOptions CONFIG;
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium");
   private static String MOD_VERSION;

   public static void onInitialization(String version) {
      MOD_VERSION = version;
      initializeConfig();

      try {
         updateFingerprint();
      } catch (Throwable var2) {
         LOGGER.error("Failed to update fingerprint", var2);
      }
   }

   public static synchronized void initializeConfig() {
      if (CONFIG == null) {
         CONFIG = loadConfig();
      }
   }

   public static SodiumGameOptions options() {
      if (CONFIG == null) {
         throw new IllegalStateException("Config not yet available");
      } else {
         return CONFIG;
      }
   }

   public static Logger logger() {
      if (LOGGER == null) {
         throw new IllegalStateException("Logger not yet available");
      } else {
         return LOGGER;
      }
   }

   private static SodiumGameOptions loadConfig() {
      try {
         return SodiumGameOptions.loadFromDisk();
      } catch (Exception var2) {
         LOGGER.error("Failed to load configuration file", var2);
         LOGGER.error("Using default configuration file in read-only mode");
         Console.instance().logMessage(MessageLevel.SEVERE, "sodium.console.config_not_loaded", true, 12.5);
         SodiumGameOptions config = SodiumGameOptions.defaults();
         config.setReadOnly();
         return config;
      }
   }

   public static void restoreDefaultOptions() {
      CONFIG = SodiumGameOptions.defaults();

      try {
         SodiumGameOptions.writeToDisk(CONFIG);
      } catch (IOException var1) {
         throw new RuntimeException("Failed to write config file", var1);
      }
   }

   public static String getVersion() {
      if (MOD_VERSION == null) {
         throw new NullPointerException("Mod version hasn't been populated yet");
      } else {
         return MOD_VERSION;
      }
   }

   private static void updateFingerprint() {
      FingerprintMeasure current = FingerprintMeasure.create();
      if (current != null) {
         HashedFingerprint saved = null;

         try {
            saved = HashedFingerprint.loadFromDisk();
         } catch (Throwable var4) {
            LOGGER.error("Failed to load existing fingerprint", var4);
         }

         if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());
            CONFIG.notifications.hasSeenDonationPrompt = false;
            CONFIG.notifications.hasClearedDonationButton = false;

            try {
               SodiumGameOptions.writeToDisk(CONFIG);
            } catch (IOException var3) {
               LOGGER.error("Failed to update config file", var3);
            }
         }
      }
   }

   public static boolean allowDebuggingOptions() {
      return PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment();
   }
}
