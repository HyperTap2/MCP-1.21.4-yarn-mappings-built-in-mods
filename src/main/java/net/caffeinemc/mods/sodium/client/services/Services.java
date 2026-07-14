package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.services.standalone.StandalonePlatform;

public class Services {
   public static <T> T load(Class<T> clazz) {
      T loadedService = StandalonePlatform.get(clazz);
      SodiumClientMod.logger().debug("Loaded {} for service {}", loadedService, clazz);
      return loadedService;
   }
}
