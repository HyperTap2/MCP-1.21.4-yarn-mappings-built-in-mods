package io.github.reserveword.imblocker.rules;

import io.github.reserveword.imblocker.common.Common;
import io.github.reserveword.imblocker.common.Config;
import io.github.reserveword.imblocker.common.IMManager;

public final class ScreenListRule implements Rule {
   private static boolean blacklistedScreen;
   private static boolean whitelistedScreen;

   public static void checkScreen(Object screen) {
      blacklistedScreen = false;
      whitelistedScreen = false;
      if (screen == null) return;
      try {
         Config.INSTANCE.recoverScreen(screen.getClass());
         blacklistedScreen = Config.INSTANCE.inScreenBlacklist(screen.getClass());
         whitelistedScreen = Config.INSTANCE.inScreenWhitelist(screen.getClass()) && !blacklistedScreen;
      } catch (Throwable exception) {
         Common.LOGGER.info("Failed to inspect screen for IMBlocker", exception);
      }
   }

   public static boolean isBlacklistedScreen() {
      return blacklistedScreen;
   }

   @Override public double priority() { return 100.0; }

   @Override
   public boolean apply() {
      if (whitelistedScreen) {
         IMManager.setState(true);
         return true;
      }
      return false;
   }
}
