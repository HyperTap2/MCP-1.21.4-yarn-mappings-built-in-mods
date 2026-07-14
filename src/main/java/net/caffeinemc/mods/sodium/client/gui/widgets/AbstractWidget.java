package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.Selectable.SelectionType;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractWidget implements Drawable, Element, Selectable {
   protected final TextRenderer font = MinecraftClient.getInstance().textRenderer;
   protected boolean focused;
   protected boolean hovered;

   protected AbstractWidget() {
   }

   protected void drawString(DrawContext graphics, String text, int x, int y, int color) {
      graphics.drawTextWithShadow(this.font, text, x, y, color);
   }

   protected void drawString(DrawContext graphics, Text text, int x, int y, int color) {
      graphics.drawTextWithShadow(this.font, text, x, y, color);
   }

   public boolean isHovered() {
      return this.hovered;
   }

   protected void drawRect(DrawContext graphics, int x1, int y1, int x2, int y2, int color) {
      graphics.fill(x1, y1, x2, y2, color);
   }

   protected void playClickSound() {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
   }

   protected int getStringWidth(StringVisitable text) {
      return this.font.getWidth(text);
   }

   @NotNull
   @Override
   public SelectionType getType() {
      if (this.focused) {
         return SelectionType.FOCUSED;
      } else {
         return this.hovered ? SelectionType.HOVERED : SelectionType.NONE;
      }
   }

   @Override
   public void appendNarrations(NarrationMessageBuilder builder) {
      if (this.focused) {
         builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.focused"));
      } else if (this.hovered) {
         builder.put(NarrationPart.USAGE, Text.translatable("narration.button.usage.hovered"));
      }
   }

   @Nullable
   @Override
   public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
      return !this.isFocused() ? GuiNavigationPath.of(this) : null;
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   @Override
   public void setFocused(boolean focused) {
      if (!focused) {
         this.focused = false;
      } else {
         GuiNavigationType inputType = MinecraftClient.getInstance().getNavigationType();
         if (inputType == GuiNavigationType.KEYBOARD_TAB || inputType == GuiNavigationType.KEYBOARD_ARROW) {
            this.focused = true;
         }
      }
   }

   protected void drawBorder(DrawContext graphics, int x1, int y1, int x2, int y2, int color) {
      graphics.fill(x1, y1, x2, y1 + 1, color);
      graphics.fill(x1, y2 - 1, x2, y2, color);
      graphics.fill(x1, y1, x1 + 1, y2, color);
      graphics.fill(x2 - 1, y1, x2, y2, color);
   }

   @Override
   public abstract boolean isMouseOver(double mouseX, double mouseY);
}
