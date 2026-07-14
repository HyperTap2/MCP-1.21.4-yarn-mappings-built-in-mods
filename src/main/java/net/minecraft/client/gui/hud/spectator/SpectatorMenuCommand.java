package net.minecraft.client.gui.hud.spectator;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public interface SpectatorMenuCommand {
   void use(SpectatorMenu menu);

   Text getName();

   void renderIcon(DrawContext context, float brightness, float alpha);

   boolean isEnabled();
}
