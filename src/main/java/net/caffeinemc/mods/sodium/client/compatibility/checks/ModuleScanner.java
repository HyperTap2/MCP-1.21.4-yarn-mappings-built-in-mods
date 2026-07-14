package net.caffeinemc.mods.sodium.client.compatibility.checks;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.Tlhelp32.MODULEENTRY32W;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.caffeinemc.mods.sodium.client.platform.MessageBox;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.Version;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.VersionFixedFileInfoStruct;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.VersionInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleScanner {
   private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Win32ModuleChecks");
   private static final String[] RTSS_HOOKS_MODULE_NAMES = new String[]{"RTSSHooks64.dll", "RTSSHooks.dll"};
   private static final String[] ASUS_GPU_TWEAK_MODULE_NAMES = new String[]{
      "GTIII-OSD64-GL.dll", "GTIII-OSD-GL.dll", "GTIII-OSD64-VK.dll", "GTIII-OSD-VK.dll", "GTIII-OSD64.dll", "GTIII-OSD.dll"
   };

   public static void checkModules(NativeWindowHandle window) {
      List<String> modules;
      try {
         modules = listModules();
      } catch (Throwable var3) {
         LOGGER.warn("Failed to scan the currently loaded modules", var3);
         return;
      }

      if (!modules.isEmpty()) {
         if (BugChecks.ISSUE_2048 && isModuleLoaded(modules, RTSS_HOOKS_MODULE_NAMES)) {
            checkRTSSModules(window);
         }

         if (BugChecks.ISSUE_2637 && isModuleLoaded(modules, ASUS_GPU_TWEAK_MODULE_NAMES)) {
            checkASUSGpuTweakIII(window);
         }
      }
   }

   private static List<String> listModules() {
      if (!Platform.isWindows()) {
         return List.of();
      } else {
         int pid = Kernel32.INSTANCE.GetCurrentProcessId();
         ArrayList<String> modules = new ArrayList<>();

         for (MODULEENTRY32W module : Kernel32Util.getModules(pid)) {
            modules.add(module.szModule());
         }

         return Collections.unmodifiableList(modules);
      }
   }

   private static void checkRTSSModules(NativeWindowHandle window) {
      LOGGER.warn("RivaTuner Statistics Server (RTSS) has injected into the process! Attempting to apply workarounds for compatibility...");
      WindowsFileVersion version = null;

      try {
         version = findRTSSModuleVersion();
      } catch (Throwable var3) {
         LOGGER.warn("Exception thrown while reading file version", var3);
      }

      if (version == null) {
         LOGGER.warn("Could not determine version of RivaTuner Statistics Server");
      } else {
         LOGGER.info("Detected RivaTuner Statistics Server version: {}", version);
      }

      if (version == null || !isRTSSCompatible(version)) {
         MessageBox.showMessageBox(
            window,
            MessageBox.IconType.ERROR,
            "Sodium Renderer",
            "You appear to be using an older version of RivaTuner Statistics Server (RTSS) which is not compatible with Sodium.\n\nYou must either update to a newer version (7.3.4 and later) or close the RivaTuner Statistics Server application.\n\nFor more information on how to solve this problem, click the 'Help' button.",
            "https://link.caffeinemc.net/help/sodium/incompatible-software/rivatuner-statistics-server/gh-2048"
         );
         throw new RuntimeException(
            "The installed version of RivaTuner Statistics Server (RTSS) is not compatible with Sodium, see here for more details: https://link.caffeinemc.net/help/sodium/incompatible-software/rivatuner-statistics-server/gh-2048"
         );
      }
   }

   private static boolean isRTSSCompatible(WindowsFileVersion version) {
      int x = version.x();
      int y = version.y();
      int z = version.z();
      return x > 7 || x == 7 && y > 3 || x == 7 && y == 3 && z >= 4;
   }

   private static void checkASUSGpuTweakIII(NativeWindowHandle window) {
      MessageBox.showMessageBox(
         window,
         MessageBox.IconType.ERROR,
         "Sodium Renderer",
         "ASUS GPU Tweak III is not compatible with Minecraft, and causes extreme performance issues and severe graphical corruption when used with Minecraft.\n\nYou *must* do one of the following things to continue:\n\na) Open the settings of ASUS GPU Tweak III, enable the Blacklist option, click \"Browse from file...\", and select the Java runtime (javaw.exe) which is used by Minecraft.\n\nb) Completely uninstall the ASUS GPU Tweak III application.\n\nFor more information on how to solve this problem, click the 'Help' button.",
         "https://link.caffeinemc.net/help/sodium/incompatible-software/asus-gtiii/gh-2637"
      );
      throw new RuntimeException(
         "ASUS GPU Tweak III is not compatible with Minecraft, see here for more details: https://link.caffeinemc.net/help/sodium/incompatible-software/asus-gtiii/gh-2637"
      );
   }

   @Nullable
   private static WindowsFileVersion findRTSSModuleVersion() {
      long module;
      try {
         module = net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32.getModuleHandleByNames(RTSS_HOOKS_MODULE_NAMES);
      } catch (Throwable var9) {
         LOGGER.warn("Failed to locate module", var9);
         return null;
      }

      String moduleFileName;
      try {
         moduleFileName = net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32.getModuleFileName(module);
      } catch (Throwable var8) {
         LOGGER.warn("Failed to get path of module", var8);
         return null;
      }

      Path modulePath = Path.of(moduleFileName);
      Path moduleDirectory = modulePath.getParent();
      LOGGER.info("Searching directory: {}", moduleDirectory);
      Path executablePath = moduleDirectory.resolve("RTSS.exe");
      if (!Files.exists(executablePath)) {
         LOGGER.warn("Could not find executable: {}", executablePath);
         return null;
      } else {
         LOGGER.info("Parsing file: {}", executablePath);
         VersionInfo version = Version.getModuleFileVersion(executablePath.toAbsolutePath().toString());
         if (version == null) {
            LOGGER.warn("Couldn't find version structure");
            return null;
         } else {
            VersionFixedFileInfoStruct fileVersion = version.queryFixedFileInfo();
            if (fileVersion == null) {
               LOGGER.warn("Couldn't query file version");
               return null;
            } else {
               return WindowsFileVersion.fromFileVersion(fileVersion);
            }
         }
      }
   }

   private static boolean isModuleLoaded(List<String> modules, String[] names) {
      for (String name : names) {
         for (String module : modules) {
            if (module.equalsIgnoreCase(name)) {
               return true;
            }
         }
      }

      return false;
   }
}
