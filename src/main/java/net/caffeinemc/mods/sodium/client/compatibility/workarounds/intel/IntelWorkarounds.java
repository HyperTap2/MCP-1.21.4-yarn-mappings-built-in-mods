package net.caffeinemc.mods.sodium.client.compatibility.workarounds.intel;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterInfo;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;
import net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMT;
import org.jetbrains.annotations.Nullable;

public class IntelWorkarounds {
   @Nullable
   public static WindowsFileVersion findIntelDriverMatchingBug899() {
      if (OsUtils.getOs() != OsUtils.OperatingSystem.WIN) {
         return null;
      } else {
         for (GraphicsAdapterInfo adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter instanceof D3DKMT.WDDMAdapterInfo wddmAdapterInfo) {
               String driverName = wddmAdapterInfo.getOpenGlIcdName();
               if (driverName != null) {
                  WindowsFileVersion driverVersion = wddmAdapterInfo.openglIcdVersion();
                  if (driverName.matches("ig7icd(32|64).dll") && driverVersion.z() == 10 && driverVersion.w() < 5161) {
                     return driverVersion;
                  }
               }
            }
         }

         return null;
      }
   }

   public static boolean isUsingIntelGen8OrOlder() {
      if (OsUtils.getOs() != OsUtils.OperatingSystem.WIN) {
         return false;
      } else {
         for (GraphicsAdapterInfo adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter instanceof D3DKMT.WDDMAdapterInfo wddmAdapterInfo) {
               String driverName = wddmAdapterInfo.getOpenGlIcdName();
               if (driverName != null && driverName.matches("ig(7|75|8)icd(32|64)\\.dll")) {
                  return true;
               }
            }
         }

         return false;
      }
   }
}
