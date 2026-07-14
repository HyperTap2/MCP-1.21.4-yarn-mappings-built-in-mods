package com.viaversion.viafabricplus.util;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import org.apache.logging.log4j.Logger;

public final class NotificationUtil {
   public static void warnIncompatibilityPacket(String version, String packet, String yarnMethod, String mojmapMethod) {
      Logger logger = ViaFabricPlusImpl.INSTANCE.logger();
      logger.error("===========================================");
      logger.error("The " + packet + " packet (>= " + version + ") could not be remapped without breaking content!");
      logger.error("Try disabling mods one by one or using a binary search method to identify the problematic mod.");
      logger.error("Mods authors should use " + yarnMethod + " (Yarn) or " + mojmapMethod + " (Mojmap) instead of sending packets directly.");
      logger.error("Need help? Join our Discord: https://discord.gg/viaversion");
      logger.error("===========================================");
   }
}
