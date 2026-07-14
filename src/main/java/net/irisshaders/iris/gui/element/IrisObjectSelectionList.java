package net.irisshaders.iris.gui.element;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget.Entry;

public class IrisObjectSelectionList<E extends Entry<E>> extends EntryListWidget<E> {
   public IrisObjectSelectionList(MinecraftClient client, int width, int height, int top, int bottom, int left, int right, int itemHeight) {
      super(client, width, height, top, itemHeight);
   }

   protected int getScrollbarX() {
      return this.width - 6;
   }

   public void select(int entry) {
      this.setSelected(this.getEntry(entry));
   }

   public void appendClickableNarrations(NarrationMessageBuilder builder) {
   }
}
