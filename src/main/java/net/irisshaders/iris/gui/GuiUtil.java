package net.irisshaders.iris.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class GuiUtil {
   public static final Identifier IRIS_WIDGETS_TEX = Identifier.of("iris", "textures/gui/widgets.png");
   private static final Text ELLIPSIS = Text.literal("...");

   private GuiUtil() {
   }

   private static MinecraftClient client() {
      return MinecraftClient.getInstance();
   }

   public static void bindIrisWidgetsTexture() {
      RenderSystem.setShaderTexture(0, IRIS_WIDGETS_TEX);
   }

   public static void drawButton(DrawContext guiGraphics, int x, int y, int width, int height, boolean hovered, boolean disabled) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      int vOffset = disabled ? 46 : (hovered ? 86 : 66);
      RenderSystem.enableBlend();
      guiGraphics.drawTexture(RenderLayer::getGuiTextured, IRIS_WIDGETS_TEX, x, y, 0.0F, vOffset, halfWidth, halfHeight, 256, 256);
      guiGraphics.drawTexture(
         RenderLayer::getGuiTextured, IRIS_WIDGETS_TEX, x + halfWidth, y, 200 - (width - halfWidth), vOffset, width - halfWidth, halfHeight, 256, 256
      );
      guiGraphics.drawTexture(
         RenderLayer::getGuiTextured,
         IRIS_WIDGETS_TEX,
         x,
         y + halfHeight,
         0.0F,
         vOffset + (20 - (height - halfHeight)),
         halfWidth,
         height - halfHeight,
         256,
         256
      );
      guiGraphics.drawTexture(
         RenderLayer::getGuiTextured,
         IRIS_WIDGETS_TEX,
         x + halfWidth,
         y + halfHeight,
         200 - (width - halfWidth),
         vOffset + (20 - (height - halfHeight)),
         width - halfWidth,
         height - halfHeight,
         256,
         256
      );
   }

   public static void drawPanel(DrawContext guiGraphics, int x, int y, int width, int height) {
      int borderColor = -555819298;
      int innerColor = -570425344;
      guiGraphics.fill(RenderLayer.getGuiOverlay(), x, y, x + width, y + 1, borderColor);
      guiGraphics.fill(RenderLayer.getGuiOverlay(), x, y + height - 1, x + width, y + height, borderColor);
      guiGraphics.fill(RenderLayer.getGuiOverlay(), x, y + 1, x + 1, y + height - 1, borderColor);
      guiGraphics.fill(RenderLayer.getGuiOverlay(), x + width - 1, y + 1, x + width, y + height - 1, borderColor);
      guiGraphics.fill(RenderLayer.getGuiOverlay(), x + 1, y + 1, x + width - 1, y + height - 1, innerColor);
   }

   public static void drawTextPanel(TextRenderer font, DrawContext guiGraphics, Text text, int x, int y) {
      drawPanel(guiGraphics, x, y, font.getWidth(text) + 8, 16);
      guiGraphics.drawTextWithShadow(font, text, x + 4, y + 4, 16777215);
   }

   public static MutableText shortenText(TextRenderer font, MutableText text, int width) {
      return font.getWidth(text) > width
         ? Text.literal(font.trimToWidth(text.getString(), width - font.getWidth(ELLIPSIS))).append(ELLIPSIS).setStyle(text.getStyle())
         : text;
   }

   public static MutableText translateOrDefault(MutableText defaultText, String translationDesc, Object... format) {
      return I18n.hasTranslation(translationDesc) ? Text.translatable(translationDesc, format) : defaultText;
   }

   public static void playButtonClickSound() {
      client().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public static class Icon {
      public static final GuiUtil.Icon SEARCH = new GuiUtil.Icon(0, 0, 7, 8);
      public static final GuiUtil.Icon CLOSE = new GuiUtil.Icon(7, 0, 5, 6);
      public static final GuiUtil.Icon REFRESH = new GuiUtil.Icon(12, 0, 10, 10);
      public static final GuiUtil.Icon EXPORT = new GuiUtil.Icon(22, 0, 7, 8);
      public static final GuiUtil.Icon EXPORT_COLORED = new GuiUtil.Icon(29, 0, 7, 8);
      public static final GuiUtil.Icon IMPORT = new GuiUtil.Icon(22, 8, 7, 8);
      public static final GuiUtil.Icon IMPORT_COLORED = new GuiUtil.Icon(29, 8, 7, 8);
      private final int u;
      private final int v;
      private final int width;
      private final int height;

      public Icon(int u, int v, int width, int height) {
         this.u = u;
         this.v = v;
         this.width = width;
         this.height = height;
      }

      public void draw(DrawContext guiGraphics, int x, int y) {
         RenderSystem.enableBlend();
         guiGraphics.drawTexture(RenderLayer::getGuiTextured, GuiUtil.IRIS_WIDGETS_TEX, x, y, this.u, this.v, this.width, this.height, 256, 256);
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }
   }
}
