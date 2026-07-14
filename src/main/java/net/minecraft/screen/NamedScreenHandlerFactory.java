package net.minecraft.screen;

import net.minecraft.text.Text;

public interface NamedScreenHandlerFactory extends ScreenHandlerFactory {
   Text getDisplayName();
}
