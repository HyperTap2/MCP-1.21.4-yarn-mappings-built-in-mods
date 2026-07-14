// SPDX-License-Identifier: GPL-3.0-or-later
package com.viaversion.viafabricplus.visuals.features.force_unicode_font;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.visuals.settings.VisualSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.util.Language;

public final class UnicodeFontFix1_12_2 {
   private static boolean enabled;
   private static Runnable pendingTask;

   private UnicodeFontFix1_12_2() {
   }

   public static void init() {
      ViaFabricPlus.getImpl().registerOnChangeProtocolVersionCallback((oldVersion, newVersion) -> updateUnicodeFontOverride(newVersion));
   }

   public static void updateUnicodeFontOverride(ProtocolVersion version) {
      SimpleOption<Boolean> option = MinecraftClient.getInstance().options.getForceUnicodeFont();
      if (VisualSettings.INSTANCE.forceUnicodeFontForNonAsciiLanguages.isEnabled(version)) {
         if (Language.getInstance() instanceof TranslationStorage storage) {
            enabled = LanguageUtil.isUnicodeFont1_12_2(storage.viaFabricPlus$getTranslations());
            pendingTask = () -> option.setValue(enabled);
         }
      } else if (enabled) {
         enabled = false;
         pendingTask = () -> option.setValue(false);
      }
   }

   public static void tick() {
      Runnable task = pendingTask;
      if (task != null) {
         pendingTask = null;
         task.run();
      }
   }
}
