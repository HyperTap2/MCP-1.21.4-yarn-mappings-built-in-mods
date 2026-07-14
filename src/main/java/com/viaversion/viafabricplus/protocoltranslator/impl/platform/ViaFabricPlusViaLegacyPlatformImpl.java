package com.viaversion.viafabricplus.protocoltranslator.impl.platform;

import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.vialoader.impl.platform.ViaLegacyPlatformImpl;
import com.viaversion.viaversion.api.Via;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import net.raphimc.vialegacy.ViaLegacy;
import net.raphimc.vialegacy.platform.ViaLegacyConfig;

public final class ViaFabricPlusViaLegacyPlatformImpl extends ViaLegacyPlatformImpl {
   public ViaFabricPlusViaLegacyPlatformImpl() {
      super();
      installSettingsBackedConfig();
   }

   private static void installSettingsBackedConfig() {
      try {
         ViaLegacyConfig delegate = ViaLegacy.getConfig();
         ViaLegacyConfig proxy = (ViaLegacyConfig)Proxy.newProxyInstance(
            ViaLegacyConfig.class.getClassLoader(),
            new Class<?>[]{ViaLegacyConfig.class},
            (instance, method, args) -> {
               if (method.getName().equals("isLegacySkullLoading") || method.getName().equals("isLegacySkinLoading")) {
                  return GeneralSettings.INSTANCE.loadSkinsAndSkullsInLegacyVersions.getValue();
               }
               try {
                  return method.invoke(delegate, args);
               } catch (InvocationTargetException exception) {
                  throw exception.getCause();
               }
            }
         );
         Field config = ViaLegacy.class.getDeclaredField("config");
         config.setAccessible(true);
         config.set(null, proxy);
      } catch (ReflectiveOperationException exception) {
         throw new IllegalStateException("Unable to install the ViaFabricPlus ViaLegacy configuration adapter", exception);
      }
   }

   public String getCpeAppName() {
      return Via.getPlatform().getPlatformName() + " " + Via.getPlatform().getPlatformVersion();
   }
}
