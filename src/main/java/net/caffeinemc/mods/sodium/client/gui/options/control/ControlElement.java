package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ControlElement<T> extends AbstractWidget {
   protected final Option<T> option;
   protected final Dim2i dim;

   public ControlElement(Option<T> option, Dim2i dim) {
      this.option = option;
      this.dim = dim;
   }

   public int getContentWidth() {
      return this.option.getControl().getMaxWidth();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      String name = this.option.getName().getString();
      if (this.option.isAvailable() && this.option.hasChanged()) {
         name = name + " *";
      }

      if (this.hovered || this.isFocused()) {
         name = this.truncateLabelToFit(name);
      }

      String label;
      if (this.option.isAvailable()) {
         if (this.option.hasChanged()) {
            label = Formatting.ITALIC + name;
         } else {
            label = Formatting.WHITE + name;
         }
      } else {
         label = "" + Formatting.GRAY + Formatting.STRIKETHROUGH + name;
      }

      this.hovered = this.dim.containsCursor(mouseX, mouseY);
      this.drawRect(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), this.hovered ? -536870912 : -1879048192);
      this.drawString(context, label, this.dim.x() + 6, this.dim.getCenterY() - 4, -1);
      if (this.isFocused()) {
         this.drawBorder(context, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
      }
   }

   @NotNull
   private String truncateLabelToFit(String name) {
      String suffix = "...";
      int suffixWidth = this.font.getWidth(suffix);
      int nameFontWidth = this.font.getWidth(name);
      int targetWidth = this.dim.width() - this.getContentWidth() - 20;
      if (nameFontWidth > targetWidth) {
         targetWidth -= suffixWidth;
         int maxLabelChars = name.length() - 3;
         int minLabelChars = 1;

         while (maxLabelChars - minLabelChars > 1) {
            int mid = (maxLabelChars + minLabelChars) / 2;
            String midName = name.substring(0, mid);
            int midWidth = this.font.getWidth(midName);
            if (midWidth > targetWidth) {
               maxLabelChars = mid;
            } else {
               minLabelChars = mid;
            }
         }

         name = name.substring(0, minLabelChars).trim() + suffix;
      }

      return name;
   }

   public Option<T> getOption() {
      return this.option;
   }

   public Dim2i getDimensions() {
      return this.dim;
   }

   @Nullable
   @Override
   public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
      return !this.option.isAvailable() ? null : super.getNavigationPath(navigation);
   }

   @Override
   public ScreenRect getNavigationFocus() {
      return new ScreenRect(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return this.dim.containsCursor(mouseX, mouseY);
   }
}
