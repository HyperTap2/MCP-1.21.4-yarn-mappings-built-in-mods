package net.minecraft.client.gui.tooltip;

import org.joml.Vector2ic;

public interface TooltipPositioner {
   Vector2ic getPosition(int screenWidth, int screenHeight, int x, int y, int width, int height);
}
