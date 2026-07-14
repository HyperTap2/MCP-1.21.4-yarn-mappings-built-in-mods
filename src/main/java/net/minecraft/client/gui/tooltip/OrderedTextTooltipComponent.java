package net.minecraft.client.gui.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;

public class OrderedTextTooltipComponent implements TooltipComponent {
   private final OrderedText text;

   public OrderedTextTooltipComponent(OrderedText text) {
      this.text = text;
   }

   @Override
   public int getWidth(TextRenderer textRenderer) {
      return textRenderer.getWidth(this.text);
   }

   @Override
   public int getHeight(TextRenderer textRenderer) {
      return 10;
   }

   @Override
   public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
      textRenderer.draw(this.text, x, y, -1, true, matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 15728880);
   }
}
