package com.viaversion.viafabricplus.screen;

import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;

public class VFPList extends AlwaysSelectedEntryListWidget<VFPListEntry> {
   public VFPList(MinecraftClient minecraftClient, int width, int height, int top, int bottom, int entryHeight) {
      super(minecraftClient, width, height - top - bottom, top, entryHeight);
   }

   public void initScrollY(double scrollY) {
      if ((Boolean)GeneralSettings.INSTANCE.saveScrollPositionInSlotScreens.getValue()) {
         this.setScrollY(scrollY);
      }
   }

   public void setScrollY(double scrollY) {
      super.setScrollY(scrollY);
      this.updateSlotAmount(this.getScrollY());
   }

   protected void drawSelectionHighlight(DrawContext context, int y, int entryWidth, int entryHeight, int borderColor, int fillColor) {
   }

   protected void updateSlotAmount(double amount) {
   }
}
