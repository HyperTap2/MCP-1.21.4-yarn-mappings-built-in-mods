package net.caffeinemc.mods.sodium.client.compatibility.workarounds;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.intel.IntelWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Workarounds {
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Workarounds");
   private static final AtomicReference<Set<Workarounds.Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Workarounds.Reference.class));

   public static void init() {
      Set<Workarounds.Reference> workarounds = findNecessaryWorkarounds();
      if (!workarounds.isEmpty()) {
         LOGGER.warn(
            "Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [{}]",
            workarounds.stream().map(Enum::name).collect(Collectors.joining(", "))
         );
         LOGGER.warn(
            "This is not necessarily an issue, but it may result in certain features or optimizations being disabled. You can sometimes fix these issues by upgrading your graphics driver."
         );
      }

      ACTIVE_WORKAROUNDS.set(workarounds);
   }

   private static Set<Workarounds.Reference> findNecessaryWorkarounds() {
      EnumSet<Workarounds.Reference> workarounds = EnumSet.noneOf(Workarounds.Reference.class);
      OsUtils.OperatingSystem operatingSystem = OsUtils.getOs();
      if (NvidiaWorkarounds.isNvidiaGraphicsCardPresent()) {
         if (SodiumClientMod.options().advanced.allowNvidiaThreadedOptimizations) {
            LOGGER.warn("NVIDIA driver threaded OpenGL command submission is allowed by the Sodium performance setting.");
            LOGGER.warn(
               "This can improve frame rates, but it disables Sodium's NVIDIA safety workaround and may cause rendering corruption, crashes, or other severe instability. Disable 'NVIDIA Driver Threaded Optimizations' in Video Settings > Advanced and restart the game to restore the workaround."
            );
         } else {
            workarounds.add(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS_BROKEN);
         }
      }

      if (IntelWorkarounds.isUsingIntelGen8OrOlder()) {
         workarounds.add(Workarounds.Reference.INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED);
         workarounds.add(Workarounds.Reference.INTEL_DEPTH_BUFFER_COMPARISON_UNRELIABLE);
      }

      if (operatingSystem == OsUtils.OperatingSystem.LINUX) {
         String session = System.getenv("XDG_SESSION_TYPE");
         if (session == null) {
            LOGGER.warn(
               "Unable to determine desktop session type because the environment variable XDG_SESSION_TYPE is not set! Your user session may not be configured correctly."
            );
         }

         if (Objects.equals(session, "wayland")) {
            workarounds.add(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
         }
      }

      return Collections.unmodifiableSet(workarounds);
   }

   public static boolean isWorkaroundEnabled(Workarounds.Reference id) {
      return ACTIVE_WORKAROUNDS.get().contains(id);
   }

   public static enum Reference {
      NVIDIA_THREADED_OPTIMIZATIONS_BROKEN,
      NO_ERROR_CONTEXT_UNSUPPORTED,
      INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED,
      INTEL_DEPTH_BUFFER_COMPARISON_UNRELIABLE;
   }
}
