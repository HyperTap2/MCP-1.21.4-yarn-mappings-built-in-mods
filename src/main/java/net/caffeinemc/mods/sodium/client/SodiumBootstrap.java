package net.caffeinemc.mods.sodium.client;

import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;

import net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;

public final class SodiumBootstrap {
   public static final String VERSION = "0.6.13+mc1.21.4-hardmerge";
   private static boolean preInitialized;
   private static boolean clientInitialized;

   private SodiumBootstrap() {
   }

   public static synchronized void preLaunch() {
      if (preInitialized) {
         return;
      }

      PreLaunchChecks.checkEnvironment();
      GraphicsAdapterProbe.findAdapters();
      SodiumClientMod.initializeConfig();
      Workarounds.init();
      preInitialized = true;
   }

   public static synchronized void initializeClient() {
      if (clientInitialized) {
         return;
      }
      SodiumClientMod.onInitialization(VERSION);
      SodiumExtraClientMod.options();
      SodiumExtraClientMod.mixinConfig();
      clientInitialized = true;
   }
}
