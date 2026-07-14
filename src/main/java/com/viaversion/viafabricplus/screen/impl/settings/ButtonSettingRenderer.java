package com.viaversion.viafabricplus.screen.impl.settings;

import com.viaversion.viafabricplus.api.settings.type.ButtonSetting;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ButtonSettingRenderer extends VFPListEntry {
   private final ButtonSetting value;

   public ButtonSettingRenderer(ButtonSetting value) {
      this.value = value;
   }

   public Text getNarration() {
      return this.value.displayValue();
   }

   @Override
   public void mappedMouseClicked(double mouseX, double mouseY, int button) {
      ((Runnable)this.value.getValue()).run();
   }

   @Override
   public void mappedRender(
      DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
   ) {
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      context.drawCenteredTextWithShadow(textRenderer, this.value.displayValue(), entryWidth / 2, entryHeight / 2 - 9 / 2, -1);
      this.renderTooltip(this.value.getTooltip(), mouseX, mouseY);
   }
}
