package net.minecraft.client.gui.screen.ingame;

import net.minecraft.screen.ScreenHandler;

public interface ScreenHandlerProvider<T extends ScreenHandler> {
   T getScreenHandler();
}
