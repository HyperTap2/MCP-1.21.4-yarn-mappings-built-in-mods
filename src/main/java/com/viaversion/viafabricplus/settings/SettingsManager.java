package com.viaversion.viafabricplus.settings;

import com.viaversion.viafabricplus.api.events.LoadingCycleCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback.LoadingCycle;
import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.settings.impl.AuthenticationSettings;
import com.viaversion.viafabricplus.settings.impl.BedrockSettings;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettingsManager {
   public static final SettingsManager INSTANCE = new SettingsManager();
   private final List<SettingGroup> groups = new ArrayList<>();

   public void init() {
      ((LoadingCycleCallback)Events.LOADING_CYCLE.invoker()).onLoadCycle(LoadingCycle.PRE_SETTINGS_LOAD);
      this.addGroup(GeneralSettings.INSTANCE, BedrockSettings.INSTANCE, AuthenticationSettings.INSTANCE, DebugSettings.INSTANCE);
      ((LoadingCycleCallback)Events.LOADING_CYCLE.invoker()).onLoadCycle(LoadingCycle.POST_SETTINGS_LOAD);
   }

   public void addGroup(SettingGroup... groups) {
      Collections.addAll(this.groups, groups);
   }

   public List<SettingGroup> getGroups() {
      return this.groups;
   }
}
