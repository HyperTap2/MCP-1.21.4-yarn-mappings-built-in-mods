package net.caffeinemc.mods.sodium.client.gui.options.storage;

import java.io.IOException;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;

public class SodiumOptionsStorage implements OptionStorage<SodiumGameOptions> {
   private final SodiumGameOptions options = SodiumClientMod.options();

   public SodiumGameOptions getData() {
      return this.options;
   }

   @Override
   public void save() {
      try {
         SodiumGameOptions.writeToDisk(this.options);
      } catch (IOException var2) {
         throw new RuntimeException("Couldn't save configuration changes", var2);
      }

      SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
   }
}
