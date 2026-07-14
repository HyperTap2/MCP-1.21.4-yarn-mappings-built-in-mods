package net.irisshaders.iris.gui.element;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.ElementListWidget.Entry;

public class IrisContainerObjectSelectionList<E extends Entry<E>> extends ElementListWidget<E> {
   public IrisContainerObjectSelectionList(MinecraftClient client, int width, int height, int top, int bottom, int left, int right, int itemHeight) {
      super(client, width, height, top, itemHeight);
   }

   protected int getScrollbarX() {
      return this.width - 6;
   }

   public void select(int entry) {
      this.setSelected(this.getEntry(entry));
   }
}
