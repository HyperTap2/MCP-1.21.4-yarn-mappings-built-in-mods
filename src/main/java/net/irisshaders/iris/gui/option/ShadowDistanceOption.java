package net.irisshaders.iris.gui.option;

import java.util.function.Consumer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.SimpleOption.Callbacks;
import net.minecraft.client.option.SimpleOption.TooltipFactory;
import net.minecraft.client.option.SimpleOption.ValueTextGetter;

public class ShadowDistanceOption<T> extends SimpleOption<T> {
   public ShadowDistanceOption(String string, TooltipFactory<T> arg, ValueTextGetter<T> arg2, Callbacks<T> arg3, T object, Consumer<T> consumer) {
      super(string, arg, arg2, arg3, object, consumer);
   }

   public ClickableWidget createWidget(GameOptions options, int x, int y, int width) {
      ClickableWidget widget = super.createWidget(options, x, y, width);
      widget.active = IrisVideoSettings.isShadowDistanceSliderEnabled();
      return widget;
   }
}
