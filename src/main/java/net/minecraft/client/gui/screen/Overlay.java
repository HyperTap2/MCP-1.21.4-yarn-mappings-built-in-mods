package net.minecraft.client.gui.screen;

import net.minecraft.client.gui.Drawable;

public abstract class Overlay implements Drawable {
   public boolean pausesGame() {
      return true;
   }
}
