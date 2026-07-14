package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;

public class SliderControl implements Control<Integer> {
   private final Option<Integer> option;
   private final int min;
   private final int max;
   private final int interval;
   private final ControlValueFormatter mode;

   public SliderControl(Option<Integer> option, int min, int max, int interval, ControlValueFormatter mode) {
      Validate.isTrue(max > min, "The maximum value must be greater than the minimum value", new Object[0]);
      Validate.isTrue(interval > 0, "The slider interval must be greater than zero", new Object[0]);
      Validate.isTrue((max - min) % interval == 0, "The maximum value must be divisable by the interval", new Object[0]);
      Validate.notNull(mode, "The slider mode must not be null", new Object[0]);
      this.option = option;
      this.min = min;
      this.max = max;
      this.interval = interval;
      this.mode = mode;
   }

   @Override
   public ControlElement<Integer> createElement(Dim2i dim) {
      return new SliderControl.Button(this.option, dim, this.min, this.max, this.interval, this.mode);
   }

   @Override
   public Option<Integer> getOption() {
      return this.option;
   }

   @Override
   public int getMaxWidth() {
      return 170;
   }

   private static class Button extends ControlElement<Integer> {
      private static final int THUMB_WIDTH = 2;
      private static final int TRACK_HEIGHT = 1;
      private final Rect2i sliderBounds;
      private int contentWidth;
      private final ControlValueFormatter formatter;
      private final int min;
      private final int max;
      private final int range;
      private final int interval;
      private double thumbPosition;
      private boolean sliderHeld;

      public Button(Option<Integer> option, Dim2i dim, int min, int max, int interval, ControlValueFormatter formatter) {
         super(option, dim);
         this.min = min;
         this.max = max;
         this.range = max - min;
         this.interval = interval;
         this.thumbPosition = this.getThumbPositionForValue(option.getValue());
         this.formatter = formatter;
         this.sliderBounds = new Rect2i(dim.getLimitX() - 96, dim.getCenterY() - 5, 90, 10);
         this.sliderHeld = false;
      }

      @Override
      public void render(DrawContext context, int mouseX, int mouseY, float delta) {
         int sliderX = this.sliderBounds.getX();
         int sliderY = this.sliderBounds.getY();
         int sliderWidth = this.sliderBounds.getWidth();
         int sliderHeight = this.sliderBounds.getHeight();
         MutableText label = this.formatter.format(this.option.getValue()).copy();
         if (!this.option.isAvailable()) {
            label.setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
         }

         int labelWidth = this.font.getWidth(label);
         boolean drawSlider = this.option.isAvailable() && (this.hovered || this.isFocused());
         if (drawSlider) {
            this.contentWidth = sliderWidth + labelWidth;
         } else {
            this.contentWidth = labelWidth;
         }

         super.render(context, mouseX, mouseY, delta);
         if (drawSlider) {
            this.thumbPosition = this.getThumbPositionForValue(this.option.getValue());
            double thumbOffset = MathHelper.clamp((double)(this.getIntValue() - this.min) / this.range * sliderWidth, 0.0, (double)sliderWidth);
            int thumbX = (int)(sliderX + thumbOffset - 2.0);
            int trackY = (int)(sliderY + sliderHeight / 2.0F - 0.5);
            this.drawRect(context, thumbX, sliderY, thumbX + 4, sliderY + sliderHeight, -1);
            this.drawRect(context, sliderX, trackY, sliderX + sliderWidth, trackY + 1, -1);
            this.drawString(context, label, sliderX - labelWidth - 6, sliderY + sliderHeight / 2 - 4, -1);
         } else {
            this.drawString(context, label, sliderX + sliderWidth - labelWidth, sliderY + sliderHeight / 2 - 4, -1);
         }
      }

      @Override
      public int getContentWidth() {
         return this.contentWidth;
      }

      public int getIntValue() {
         return this.min + this.interval * (int)Math.round(this.getSnappedThumbPosition() / this.interval);
      }

      public double getSnappedThumbPosition() {
         return this.thumbPosition / (1.0 / this.range);
      }

      public double getThumbPositionForValue(int value) {
         return (value - this.min) * (1.0 / this.range);
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         this.sliderHeld = false;
         if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            if (this.sliderBounds.contains((int)mouseX, (int)mouseY)) {
               this.setValueFromMouse(mouseX);
               this.sliderHeld = true;
            }

            return true;
         } else {
            return false;
         }
      }

      private void setValueFromMouse(double d) {
         this.setValue((d - this.sliderBounds.getX()) / this.sliderBounds.getWidth());
      }

      public void setValue(double d) {
         this.thumbPosition = MathHelper.clamp(d, 0.0, 1.0);
         int value = this.getIntValue();
         if (this.option.getValue() != value) {
            this.option.setValue(value);
         }
      }

      @Override
      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (!this.isFocused()) {
            return false;
         } else if (keyCode == 263) {
            this.option.setValue(MathHelper.clamp(this.option.getValue() - this.interval, this.min, this.max));
            return true;
         } else if (keyCode == 262) {
            this.option.setValue(MathHelper.clamp(this.option.getValue() + this.interval, this.min, this.max));
            return true;
         } else {
            return false;
         }
      }

      @Override
      public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
         if (this.option.isAvailable() && button == 0) {
            if (this.sliderHeld) {
               this.setValueFromMouse(mouseX);
            }

            return true;
         } else {
            return false;
         }
      }
   }
}
