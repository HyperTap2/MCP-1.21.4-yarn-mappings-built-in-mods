package me.pepperbell.continuity.client.config;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.client.gui.options.OptionFlag;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.text.Text;

public final class ContinuityGameOptionPage {
   private static final OptionStorage<ContinuityConfig> STORAGE = new OptionStorage<>() {
      @Override
      public ContinuityConfig getData() {
         return ContinuityConfig.INSTANCE;
      }

      @Override
      public void save() {
         ContinuityConfig.INSTANCE.save();
      }
   };

   private ContinuityGameOptionPage() {
   }

   public static OptionPage create() {
      OptionGroup group = OptionGroup.createBuilder()
         .add(toggle("connected_textures", ContinuityConfig.INSTANCE.connectedTextures))
         .add(toggle("emissive_textures", ContinuityConfig.INSTANCE.emissiveTextures))
         .add(toggle("custom_block_layers", ContinuityConfig.INSTANCE.customBlockLayers))
         .build();
      return new OptionPage(Text.translatable("options.continuity.title"), ImmutableList.of(group));
   }

   private static OptionImpl<ContinuityConfig, Boolean> toggle(String key, Option.BooleanOption setting) {
      String translationKey = "options.continuity." + key;
      return OptionImpl.createBuilder(boolean.class, STORAGE)
         .setName(Text.translatable(translationKey))
         .setTooltip(Text.translatable(translationKey + ".tooltip"))
         .setControl(TickBoxControl::new)
         .setBinding((config, value) -> setting.set(value), config -> setting.get())
         .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
         .build();
   }
}
