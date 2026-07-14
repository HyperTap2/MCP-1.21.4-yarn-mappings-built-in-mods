package net.caffeinemc.mods.sodium.client.gui.console;

import net.caffeinemc.mods.sodium.client.console.Console;
import net.minecraft.client.gui.DrawContext;

public class ConsoleHooks {
   public static void render(DrawContext graphics, double currentTime) {
      ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
      ConsoleRenderer.INSTANCE.draw(graphics);
   }
}
