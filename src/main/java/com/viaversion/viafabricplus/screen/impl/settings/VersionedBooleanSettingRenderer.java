package com.viaversion.viafabricplus.screen.impl.settings;

import com.viaversion.viafabricplus.api.settings.type.VersionedBooleanSetting;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class VersionedBooleanSettingRenderer extends VFPListEntry {
   private final VersionedBooleanSetting value;

   public VersionedBooleanSettingRenderer(VersionedBooleanSetting value) {
      this.value = value;
   }

   public Text getNarration() {
      return this.value.getName();
   }

   @Override
   public void mappedMouseClicked(double mouseX, double mouseY, int button) {
      this.value.setValue((Integer)this.value.getValue() + 1);
      if ((Integer)this.value.getValue() % 3 == 0) {
         this.value.setValue(0);
      }
   }

   @Override
   public void mappedRender(
      DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
   ) {
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      Text text = Text.translatable("base.viafabricplus." + (this.value.isAuto() ? "auto" : (this.value.isEnabled() ? "on" : "off")));
      Color color = this.value.isAuto() ? Color.ORANGE : (this.value.isEnabled() ? Color.GREEN : Color.RED);
      int offset = textRenderer.getWidth(text) + 6;
      this.renderScrollableText(
         Text.of(Formatting.GRAY + this.value.getName().getString() + " " + Formatting.RESET + this.value.getProtocolRange().toString()), offset
      );
      context.drawTextWithShadow(textRenderer, text, entryWidth - offset, entryHeight / 2 - 9 / 2, color.getRGB());
      this.renderTooltip(this.value.getTooltip(), mouseX, mouseY);
   }
}
