package net.caffeinemc.mods.sodium.client.gui.options.binding.compat;

import net.caffeinemc.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

public class VanillaBooleanOptionBinding implements OptionBinding<GameOptions, Boolean> {
   private final SimpleOption<Boolean> option;

   public VanillaBooleanOptionBinding(SimpleOption<Boolean> option) {
      this.option = option;
   }

   public void setValue(GameOptions storage, Boolean value) {
      this.option.setValue(value);
   }

   public Boolean getValue(GameOptions storage) {
      return this.option.getValue();
   }
}
