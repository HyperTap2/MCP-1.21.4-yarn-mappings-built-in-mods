package com.viaversion.viafabricplus.save;

import com.viaversion.viafabricplus.api.events.LoadingCycleCallback;
import com.viaversion.viafabricplus.api.events.LoadingCycleCallback.LoadingCycle;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.save.impl.AccountsSave;
import com.viaversion.viafabricplus.save.impl.SettingsSave;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SaveManager {
   public static final SaveManager INSTANCE = new SaveManager();
   private final List<AbstractSave> saves = new ArrayList<>();
   private SettingsSave settingsSave;
   private AccountsSave accountsSave;

   public void init() {
      ((LoadingCycleCallback)Events.LOADING_CYCLE.invoker()).onLoadCycle(LoadingCycle.PRE_FILES_LOAD);
      this.add(this.settingsSave = new SettingsSave(), this.accountsSave = new AccountsSave());

      for (AbstractSave save : this.saves) {
         save.init();
      }

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         for (AbstractSave savex : this.saves) {
            savex.save();
         }
      }));
   }

   public void postInit() {
      for (AbstractSave save : this.saves) {
         save.postInit();
      }

      ((LoadingCycleCallback)Events.LOADING_CYCLE.invoker()).onLoadCycle(LoadingCycle.POST_FILES_LOAD);
   }

   public void add(AbstractSave... saves) {
      this.saves.addAll(Arrays.asList(saves));
   }

   public SettingsSave getSettingsSave() {
      return this.settingsSave;
   }

   public AccountsSave getAccountsSave() {
      return this.accountsSave;
   }
}
