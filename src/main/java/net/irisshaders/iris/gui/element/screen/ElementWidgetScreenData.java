package net.irisshaders.iris.gui.element.screen;

import net.minecraft.text.Text;

public record ElementWidgetScreenData(Text heading, boolean backButton) {
   public static final ElementWidgetScreenData EMPTY = new ElementWidgetScreenData(Text.empty(), true);
}
