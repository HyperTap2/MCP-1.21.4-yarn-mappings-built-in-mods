package com.viaversion.viafabricplus.screen.impl.settings;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.api.settings.AbstractSetting;
import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.api.settings.type.BooleanSetting;
import com.viaversion.viafabricplus.api.settings.type.ButtonSetting;
import com.viaversion.viafabricplus.api.settings.type.ModeSetting;
import com.viaversion.viafabricplus.api.settings.type.VersionedBooleanSetting;
import com.viaversion.viafabricplus.screen.VFPList;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viafabricplus.settings.SettingsManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class SettingsScreen extends VFPScreen {
   public static final SettingsScreen INSTANCE = new SettingsScreen();

   public SettingsScreen() {
      super(Text.translatable("screen.viafabricplus.settings"), true);
   }

   @Override
   protected void init() {
      this.setupDefaultSubtitle();
      this.addDrawableChild(new SettingsScreen.SlotList(this.client, this.width, this.height, 6 + (9 + 2) * 3, -5, (9 + 2) * 2));
      super.init();
   }

   public static class SlotList extends VFPList {
      private static double scrollAmount;

      public SlotList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
         super(minecraftClient, width, height, top, bottom, entryHeight);

         for (SettingGroup group : SettingsManager.INSTANCE.getGroups()) {
            this.addEntry(new TitleRenderer(group.getName()));

            for (AbstractSetting<?> setting : group.getSettings()) {
               switch (setting) {
                  case BooleanSetting booleanSetting:
                     this.addEntry(new BooleanSettingRenderer(booleanSetting));
                     break;
                  case ButtonSetting buttonSetting:
                     this.addEntry(new ButtonSettingRenderer(buttonSetting));
                     break;
                  case ModeSetting modeSetting:
                     this.addEntry(new ModeSettingRenderer(modeSetting));
                     break;
                  case VersionedBooleanSetting versionedBooleanSetting:
                     this.addEntry(new VersionedBooleanSettingRenderer(versionedBooleanSetting));
                     break;
                  case null:
                  default:
                     ViaFabricPlusImpl.INSTANCE.logger().warn("Unknown setting type: " + setting.getClass().getName());
               }
            }
         }

         this.initScrollY(scrollAmount);
      }

      public int getRowWidth() {
         return super.getRowWidth() + 140;
      }

      @Override
      protected void updateSlotAmount(double amount) {
         scrollAmount = amount;
      }
   }
}
