package jerozgen.languagereload.config;

import jerozgen.languagereload.LanguageReload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public class ConfigScreen extends GameOptionsScreen {
   private final SimpleOption<Boolean> multilingualSearch = SimpleOption.ofBoolean(
      "options.languagereload.multilingualItemSearch",
      SimpleOption.constantTooltip(Text.translatable("options.languagereload.multilingualItemSearch.tooltip")),
      Config.getInstance().multilingualItemSearch,
      value -> {
         Config.getInstance().multilingualItemSearch = value;
         Config.save();
         LanguageReload.reloadLanguages();
      }
   );
   private final SimpleOption<Boolean> removableDefault = SimpleOption.ofBoolean(
      "options.languagereload.removableDefaultLanguage",
      value -> Tooltip.of(Text.translatable(value
         ? "options.languagereload.removableDefaultLanguage.removable.tooltip"
         : "options.languagereload.removableDefaultLanguage.fixed.tooltip")),
      (caption, value) -> Text.translatable(value
         ? "options.languagereload.removableDefaultLanguage.removable"
         : "options.languagereload.removableDefaultLanguage.fixed"),
      Config.getInstance().removableDefaultLanguage,
      value -> {
         Config.getInstance().removableDefaultLanguage = value;
         Config.save();
      }
   );

   public ConfigScreen(Screen parent) {
      super(parent, MinecraftClient.getInstance().options, Text.translatable("options.languagereload.title"));
   }

   @Override
   protected void addOptions() {
      if (this.body != null) this.body.addAll(this.multilingualSearch, this.removableDefault);
   }
}
