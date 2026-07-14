package com.viaversion.viafabricplus.save.impl;

import com.google.gson.JsonObject;
import com.viaversion.viafabricplus.api.settings.AbstractSetting;
import com.viaversion.viafabricplus.api.settings.SettingGroup;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.save.AbstractSave;
import com.viaversion.viafabricplus.settings.SettingsManager;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

public final class SettingsSave extends AbstractSave {
   private String selectedProtocolVersion;

   public SettingsSave() {
      super("settings");
   }

   @Override
   public void write(JsonObject object) {
      for (SettingGroup group : SettingsManager.INSTANCE.getGroups()) {
         JsonObject groupObject = new JsonObject();

         for (AbstractSetting<?> setting : group.getSettings()) {
            setting.write(groupObject);
         }

         object.add(AbstractSetting.mapTranslationKey(ChatUtil.uncoverTranslationKey(group.getName())), groupObject);
      }

      object.addProperty("selected-protocol-version", ProtocolTranslator.getTargetVersion().getName());
   }

   @Override
   public void read(JsonObject object) {
      for (SettingGroup group : SettingsManager.INSTANCE.getGroups()) {
         String translationKey = ChatUtil.uncoverTranslationKey(group.getName());
         JsonObject groupObject = object.getAsJsonObject(AbstractSetting.mapTranslationKey(translationKey));
         if (groupObject != null) {
            for (AbstractSetting<?> setting : group.getSettings()) {
               if (groupObject.has(setting.getTranslationKey())) {
                  setting.read(groupObject);
               }
            }
         }
      }

      if (object.has("selected-protocol-version")) {
         this.selectedProtocolVersion = object.get("selected-protocol-version").getAsString();
      }
   }

   @Override
   public void postInit() {
      if (this.selectedProtocolVersion != null) {
         if ((Boolean)GeneralSettings.INSTANCE.saveSelectedProtocolVersion.getValue()) {
            ProtocolVersion protocolVersion = protocolVersionByName(this.selectedProtocolVersion);
            if (protocolVersion != null) {
               ProtocolTranslator.setTargetVersion(protocolVersion);
            }
         } else {
            ProtocolTranslator.setTargetVersion(ProtocolTranslator.NATIVE_VERSION);
         }
      }
   }

   public static ProtocolVersion protocolVersionByName(String name) {
      return name.contains("Bedrock") ? BedrockProtocolVersion.bedrockLatest : ProtocolVersion.getClosest(name);
   }
}
