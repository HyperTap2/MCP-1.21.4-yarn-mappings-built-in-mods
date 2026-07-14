package net.caffeinemc.mods.sodium.client.services;

import java.util.List;

public interface PlatformMixinOverrides {
   PlatformMixinOverrides INSTANCE = Services.load(PlatformMixinOverrides.class);

   static PlatformMixinOverrides getInstance() {
      return INSTANCE;
   }

   List<PlatformMixinOverrides.MixinOverride> applyModOverrides();

   public record MixinOverride(String modId, String option, boolean enabled) {
   }
}
