package net.minecraft.client.gui.navigation;


public enum GuiNavigationType {
   NONE,
   MOUSE,
   KEYBOARD_ARROW,
   KEYBOARD_TAB;

   public boolean isMouse() {
      return this == MOUSE;
   }

   public boolean isKeyboard() {
      return this == KEYBOARD_ARROW || this == KEYBOARD_TAB;
   }
}
