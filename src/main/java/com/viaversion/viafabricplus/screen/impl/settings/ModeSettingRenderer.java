package com.viaversion.viafabricplus.screen.impl.settings;

import com.viaversion.viafabricplus.api.settings.type.ModeSetting;
import com.viaversion.viafabricplus.screen.VFPListEntry;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ModeSettingRenderer extends VFPListEntry {
   private final ModeSetting value;

   public ModeSettingRenderer(ModeSetting value) {
      this.value = value;
   }

   public Text getNarration() {
      return this.value.getName();
   }

   @Override
   public void mappedMouseClicked(double mouseX, double mouseY, int button) {
      int currentIndex = Arrays.stream(this.value.getOptions()).toList().indexOf(this.value.getValue()) + 1;
      this.value.setValue(currentIndex > this.value.getOptions().length - 1 ? 0 : currentIndex);
   }

   @Override
   public void mappedRender(
      DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
   ) {
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      int offset = textRenderer.getWidth((StringVisitable)this.value.getValue()) + 6;
      this.renderScrollableText(this.value.getName().formatted(Formatting.GRAY), offset);
      context.drawTextWithShadow(textRenderer, (Text)this.value.getValue(), entryWidth - offset, entryHeight / 2 - 9 / 2, -1);
      this.renderTooltip(this.value.getTooltip(), mouseX, mouseY);
   }
}
