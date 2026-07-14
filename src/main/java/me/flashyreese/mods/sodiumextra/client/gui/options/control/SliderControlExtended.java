package me.flashyreese.mods.sodiumextra.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;

public class SliderControlExtended implements Control<Integer> {
   private final Option<Integer> option;
   private final int min;
   private final int max;
   private final int interval;
   private final ControlValueFormatter mode;
   private final boolean displayIntValueWhileSliding;

   public SliderControlExtended(Option<Integer> option, int min, int max, int interval, ControlValueFormatter mode, boolean displayIntValueWhileSliding) {
      Validate.isTrue(max > min, "The maximum value must be greater than the minimum value", new Object[0]);
      Validate.isTrue(interval > 0, "The slider interval must be greater than zero", new Object[0]);
      Validate.isTrue((max - min) % interval == 0, "The maximum value must be divisable by the interval", new Object[0]);
      Validate.notNull(mode, "The slider mode must not be null", new Object[0]);
      this.option = option;
      this.min = min;
      this.max = max;
      this.interval = interval;
      this.mode = mode;
      this.displayIntValueWhileSliding = displayIntValueWhileSliding;
   }

   public ControlElement<Integer> createElement(Dim2i dim) {
      return new SliderControlExtended.Slider(this.option, dim, this.min, this.max, this.interval, this.mode, this.displayIntValueWhileSliding);
   }

   public Option<Integer> getOption() {
      return this.option;
   }

   public int getMaxWidth() {
      return 130;
   }

   private static class Slider extends ControlElement<Integer> {
      private static final int THUMB_WIDTH = 2;
      private static final int TRACK_HEIGHT = 1;
      private final ControlValueFormatter formatter;
      private final boolean displayIntValueWhileSliding;
      private final int min;
      private final int range;
      private final int interval;
      private double thumbPosition;

      public Slider(Option<Integer> option, Dim2i dim, int min, int max, int interval, ControlValueFormatter formatter, boolean displayIntValueWhileSliding) {
         super(option, dim);
         this.min = min;
         this.range = max - min;
         this.interval = interval;
         this.thumbPosition = this.getThumbPositionForValue((Integer)option.getValue());
         this.formatter = formatter;
         this.displayIntValueWhileSliding = displayIntValueWhileSliding;
      }

      public Dim2i getSliderBounds() {
         return new Dim2i(this.dim.getLimitX() - 96, this.dim.getCenterY() - 5, 90, 10);
      }

      public void render(DrawContext context, int mouseX, int mouseY, float delta) {
         super.render(context, mouseX, mouseY, delta);
         if (this.option.isAvailable() && this.hovered) {
            this.renderSlider(context);
         } else {
            this.renderStandaloneValue(context);
         }
      }

      private void renderStandaloneValue(DrawContext guiGraphics) {
         int sliderX = this.getSliderBounds().x();
         int sliderY = this.getSliderBounds().y();
         int sliderWidth = this.getSliderBounds().width();
         int sliderHeight = this.getSliderBounds().height();
         Text label = this.formatter.format((Integer)this.option.getValue());
         int labelWidth = this.font.getWidth(label);
         this.drawString(guiGraphics, label, sliderX + sliderWidth - labelWidth, sliderY + sliderHeight / 2 - 4, -1);
      }

      private void renderSlider(DrawContext guiGraphics) {
         int sliderX = this.getSliderBounds().x();
         int sliderY = this.getSliderBounds().y();
         int sliderWidth = this.getSliderBounds().width();
         int sliderHeight = this.getSliderBounds().height();
         this.thumbPosition = this.getThumbPositionForValue((Integer)this.option.getValue());
         double thumbOffset = MathHelper.clamp((double)(this.getIntValue() - this.min) / this.range * sliderWidth, 0.0, sliderWidth);
         double thumbX = sliderX + thumbOffset - 2.0;
         double trackY = sliderY + sliderHeight / 2 - 0.5;
         this.drawRect(guiGraphics, (int)thumbX, sliderY, (int)(thumbX + 4.0), sliderY + sliderHeight, -1);
         this.drawRect(guiGraphics, sliderX, (int)trackY, sliderX + sliderWidth, (int)(trackY + 1.0), -1);
         Text label = (Text)(this.displayIntValueWhileSliding
            ? Text.literal(String.valueOf(this.getIntValue()))
            : this.formatter.format((Integer)this.option.getValue()));
         int labelWidth = this.font.getWidth(label);
         this.drawString(guiGraphics, label, sliderX - labelWidth - 6, sliderY + sliderHeight / 2 - 4, -1);
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

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.option.isAvailable() && button == 0 && this.getSliderBounds().containsCursor(mouseX, mouseY)) {
            this.setValueFromMouse(mouseX);
            return true;
         } else {
            return false;
         }
      }

      private void setValueFromMouse(double d) {
         this.setValue((d - this.getSliderBounds().x()) / this.getSliderBounds().width());
      }

      private void setValue(double d) {
         this.thumbPosition = MathHelper.clamp(d, 0.0, 1.0);
         int value = this.getIntValue();
         if ((Integer)this.option.getValue() != value) {
            this.option.setValue(value);
         }
      }

      public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
         if (this.option.isAvailable() && button == 0) {
            this.setValueFromMouse(mouseX);
            return true;
         } else {
            return false;
         }
      }
   }
}
