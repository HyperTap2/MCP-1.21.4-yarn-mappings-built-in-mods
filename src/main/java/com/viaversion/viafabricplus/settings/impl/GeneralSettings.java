package com.viaversion.viafabricplus.settings.impl;

import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.api.settings.type.BooleanSetting;
import com.viaversion.viafabricplus.api.settings.type.ModeSetting;
import net.minecraft.client.gui.widget.ButtonWidget.Builder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class GeneralSettings extends SettingGroup {
   public static final GeneralSettings INSTANCE = new GeneralSettings();
   private final MutableText[] ORIENTATION_OPTIONS = new MutableText[]{
      Text.translatable("base.viafabricplus.none"),
      Text.translatable("base.viafabricplus.left_top"),
      Text.translatable("base.viafabricplus.right_top"),
      Text.translatable("base.viafabricplus.left_bottom"),
      Text.translatable("base.viafabricplus.right_bottom")
   };
   public final ModeSetting multiplayerScreenButtonOrientation = new ModeSetting(
      this, Text.translatable("general_settings.viafabricplus.multiplayer_screen_button_orientation"), 2, this.ORIENTATION_OPTIONS
   );
   public final ModeSetting addServerScreenButtonOrientation = new ModeSetting(
      this, Text.translatable("general_settings.viafabricplus.add_server_screen_button_orientation"), 2, this.ORIENTATION_OPTIONS
   );
   public final ModeSetting directConnectScreenButtonOrientation = new ModeSetting(
      this, Text.translatable("general_settings.viafabricplus.direct_connect_screen_button_orientation"), 2, this.ORIENTATION_OPTIONS
   );
   public final ModeSetting removeNotAvailableItemsFromCreativeTab = new ModeSetting(
      this,
      Text.translatable("general_settings.viafabricplus.filter_creative_tabs"),
      new MutableText[]{
         Text.translatable("base.viafabricplus.vanilla_and_modded"),
         Text.translatable("base.viafabricplus.vanilla_only"),
         Text.translatable("base.viafabricplus.off")
      }
   );
   public final BooleanSetting saveSelectedProtocolVersion = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.save_selected_protocol_version"), true
   );
   public final BooleanSetting showExtraInformationInDebugHud = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.extra_information_in_debug_hud"), true
   );
   public final BooleanSetting showClassicLoadingProgressInConnectScreen = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.show_classic_loading_progress"), true
   );
   public final BooleanSetting showAdvertisedServerVersion = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.show_advertised_server_version"), true
   );
   public final ModeSetting ignorePacketTranslationErrors = new ModeSetting(
      this,
      Text.translatable("general_settings.viafabricplus.ignore_packet_translation_errors"),
      new MutableText[]{
         Text.translatable("base.viafabricplus.kick"),
         Text.translatable("base.viafabricplus.cancel_and_notify"),
         Text.translatable("base.viafabricplus.cancel")
      }
   );
   public final BooleanSetting loadSkinsAndSkullsInLegacyVersions = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.load_skins_and_skulls_in_legacy_versions"), true
   );
   public final BooleanSetting emulateInventoryActionsInAlphaVersions = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.emulate_inventory_actions_in_alpha_versions"), true
   );
   public final BooleanSetting saveScrollPositionInSlotScreens = new BooleanSetting(
      this, Text.translatable("general_settings.viafabricplus.save_scroll_position_in_slot_screens"), true
   );

   public GeneralSettings() {
      super(Text.translatable("setting_group_name.viafabricplus.general"));
      this.emulateInventoryActionsInAlphaVersions.setTooltip(Text.translatable("base.viafabricplus.this_will_require_a_restart"));
   }

   public static Builder withOrientation(Builder builder, int orientationIndex, int width, int height) {
      return switch (orientationIndex) {
         case 1 -> builder.position(5, 5);
         case 2 -> builder.position(width - 98 - 5, 5);
         case 3 -> builder.position(5, height - 20 - 5);
         case 4 -> builder.position(width - 98 - 5, height - 20 - 5);
         default -> builder;
      };
   }
}
