package com.viaversion.viafabricplus.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget.Entry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

public abstract class VFPListEntry extends Entry<VFPListEntry> {
   protected static final int SCISSORS_OFFSET = 4;
   public static final int SLOT_MARGIN = 3;
   private DrawContext context;
   private int x;
   private int y;
   private int entryWidth;
   private int entryHeight;

   public void mappedRender(
      DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
   ) {
   }

   public void mappedMouseClicked(double mouseX, double mouseY, int button) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.mappedMouseClicked(mouseX, mouseY, button);
      ClickableWidget.playClickSound(MinecraftClient.getInstance().getSoundManager());
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void renderScrollableText(Text name, int offset) {
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      this.renderScrollableText(name, this.entryHeight / 2 - 9 / 2, offset);
   }

   public void renderScrollableText(Text text, int textY, int offset) {
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      int fontWidth = font.getWidth(text);
      if (fontWidth > this.entryWidth - offset) {
         double time = Util.getMeasuringTimeMs() / 1000.0;
         double interpolateEnd = fontWidth - (this.entryWidth - offset - 7);
         double interpolatedValue = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * time / Math.max(interpolateEnd * 0.5, 3.0))) / 2.0 + 0.5;
         this.context.enableScissor(0, 0, this.entryWidth - offset - 4, this.entryHeight);
         this.context.drawTextWithShadow(font, text, 3 - (int)MathHelper.lerp(interpolatedValue, 0.0, interpolateEnd), textY, -1);
         this.context.disableScissor();
      } else {
         this.context.drawTextWithShadow(font, text, 3, textY, -1);
      }
   }

   public void renderTooltip(@Nullable Text tooltip, int mouseX, int mouseY) {
      if (tooltip != null && mouseX >= this.x && mouseX <= this.x + this.entryWidth && mouseY >= this.y && mouseY <= this.y + this.entryHeight) {
         int var10003 = mouseX - this.x;
         this.context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltip, var10003, mouseY - this.y);
      }
   }

   public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
      this.context = context;
      this.x = x;
      this.y = y;
      this.entryWidth = entryWidth;
      this.entryHeight = entryHeight;
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(x, y, 0.0F);
      context.fill(0, 0, entryWidth - 4, entryHeight, Integer.MIN_VALUE);
      this.mappedRender(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
      matrices.pop();
   }
}
