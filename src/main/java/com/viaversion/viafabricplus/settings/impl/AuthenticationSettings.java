package com.viaversion.viafabricplus.settings.impl;

import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.api.settings.type.BooleanSetting;
import net.minecraft.text.Text;

public final class AuthenticationSettings extends SettingGroup {
   public static final AuthenticationSettings INSTANCE = new AuthenticationSettings();
   public final BooleanSetting useBetaCraftAuthentication = new BooleanSetting(
      this, Text.translatable("authentication_settings.viafabricplus.use_beta_craft_authentication"), true
   );
   public final BooleanSetting verifySessionForOnlineModeServers = new BooleanSetting(
      this, Text.translatable("authentication_settings.viafabricplus.verify_session_for_online_mode"), true
   );
   public final BooleanSetting automaticallySelectCPEInClassiCubeServerList = new BooleanSetting(
      this, Text.translatable("authentication_settings.viafabricplus.automatically_select_cpe_when_using_classicube"), true
   );
   public final BooleanSetting setSessionNameToClassiCubeNameInServerList = new BooleanSetting(
      this, Text.translatable("authentication_settings.viafabricplus.set_session_name_to_classicube_name"), true
   );

   public AuthenticationSettings() {
      super(Text.translatable("setting_group_name.viafabricplus.authentication"));
   }
}
