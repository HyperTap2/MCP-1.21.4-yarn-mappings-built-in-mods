// SPDX-License-Identifier: GPL-3.0-or-later
package com.viaversion.viafabricplus.visuals;

import com.viaversion.viafabricplus.api.ViaFabricPlusBase;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback.LoadingCycle;
import com.viaversion.viafabricplus.visuals.features.classic.creative_menu.GridItemSelectionScreen;
import com.viaversion.viafabricplus.visuals.features.force_unicode_font.UnicodeFontFix1_12_2;
import com.viaversion.viafabricplus.visuals.settings.VisualSettings;
import net.minecraft.client.MinecraftClient;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public final class ViaFabricPlusVisuals {
   public static final ViaFabricPlusVisuals INSTANCE = new ViaFabricPlusVisuals();

   private ViaFabricPlusVisuals() {
   }

   public void init(ViaFabricPlusBase platform) {
      UnicodeFontFix1_12_2.init();
      platform.registerLoadingCycleCallback(cycle -> {
         if (cycle == LoadingCycle.POST_SETTINGS_LOAD) {
            platform.addSettingGroup(VisualSettings.INSTANCE);
         }
      });
      platform.registerOnChangeProtocolVersionCallback((oldVersion, newVersion) -> MinecraftClient.getInstance().execute(() -> {
         if (newVersion.olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
            GridItemSelectionScreen.INSTANCE.itemGrid = null;
         }
      }));
   }
}
