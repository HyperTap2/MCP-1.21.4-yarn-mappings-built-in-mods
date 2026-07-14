package net.minecraft.client.gui.screen;

import net.minecraft.util.Identifier;

public record ButtonTextures(Identifier enabled, Identifier disabled, Identifier enabledFocused, Identifier disabledFocused) {
   public ButtonTextures(Identifier unfocused, Identifier focused) {
      this(unfocused, unfocused, focused, focused);
   }

   public ButtonTextures(Identifier enabled, Identifier disabled, Identifier focused) {
      this(enabled, disabled, focused, disabled);
   }

   public Identifier get(boolean enabled, boolean focused) {
      if (enabled) {
         return focused ? this.enabledFocused : this.enabled;
      } else {
         return focused ? this.disabledFocused : this.disabled;
      }
   }
}
