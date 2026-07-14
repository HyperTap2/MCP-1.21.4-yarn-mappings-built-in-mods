package com.viaversion.viafabricplus.screen.impl.settings;

import com.viaversion.viafabricplus.screen.VFPListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TitleRenderer extends VFPListEntry {
   private final Text name;

   public TitleRenderer(Text name) {
      this.name = name;
   }

   public Text getNarration() {
      return this.name;
   }

   @Override
   public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(x, y, 0.0F);
      this.mappedRender(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
      matrices.pop();
   }

   @Override
   public void mappedRender(
      DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
   ) {
      TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
      context.drawTextWithShadow(textRenderer, this.name.copy().formatted(Formatting.BOLD), 3, entryHeight / 2 - 9 / 2, -1);
   }
}
