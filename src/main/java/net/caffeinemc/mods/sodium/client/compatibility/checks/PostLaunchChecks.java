package net.caffeinemc.mods.sodium.client.compatibility.checks;

import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostLaunchChecks {
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-PostlaunchChecks");

   public static void onContextInitialized(NativeWindowHandle window, GlContextInfo context) {
      GraphicsDriverChecks.postContextInit(window, context);
      NvidiaWorkarounds.applyContextChanges(context);
      if (isUsingPojavLauncher()) {
         throw new RuntimeException("It appears that you are using PojavLauncher, which is not supported when using Sodium. Please check your mods list.");
      }
   }

   private static boolean isUsingPojavLauncher() {
      if (System.getenv("POJAV_RENDERER") != null) {
         LOGGER.warn("Detected presence of environment variable POJAV_LAUNCHER, which seems to indicate we are running on Android");
         return true;
      } else {
         String librarySearchPaths = System.getProperty("java.library.path", null);
         if (librarySearchPaths != null) {
            for (String path : librarySearchPaths.split(":")) {
               if (isKnownAndroidPathFragment(path)) {
                  LOGGER.warn("Found a library search path which seems to be hosted in an Android filesystem: {}", path);
                  return true;
               }
            }
         }

         String workingDirectory = System.getProperty("user.home", null);
         if (workingDirectory != null && isKnownAndroidPathFragment(workingDirectory)) {
            LOGGER.warn("Working directory seems to be hosted in an Android filesystem: {}", workingDirectory);
         }

         return false;
      }
   }

   private static boolean isKnownAndroidPathFragment(String path) {
      return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
   }
}
