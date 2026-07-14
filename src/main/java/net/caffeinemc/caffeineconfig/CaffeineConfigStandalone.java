package net.caffeinemc.caffeineconfig;

final class CaffeineConfigStandalone implements CaffeineConfigPlatform {
   @Override
   public void applyModOverrides(CaffeineConfig config, String jsonKey) {
      // This standalone client has no runtime mod containers that can provide overrides.
   }
}
