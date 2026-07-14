package dev.tr7zw.waveycapes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public final class WaveyCapesConfigScreen extends GameOptionsScreen {
   private final WaveyCapesConfig config = WaveyCapesConfig.get();
   private final SimpleOption<Boolean> enabled = toggle("text.wc.setting.enabled", this.config.enabled, value -> this.config.enabled = value);
   private final SimpleOption<Boolean> smooth = toggle("text.wc.setting.capestyle", this.config.capeStyle == WaveyCapesConfig.CapeStyle.SMOOTH,
      value -> this.config.capeStyle = value ? WaveyCapesConfig.CapeStyle.SMOOTH : WaveyCapesConfig.CapeStyle.BLOCKY);
   private final SimpleOption<Boolean> wind = toggle("text.wc.setting.windmode", this.config.windMode == WaveyCapesConfig.WindMode.WAVES,
      value -> this.config.windMode = value ? WaveyCapesConfig.WindMode.WAVES : WaveyCapesConfig.WindMode.NONE);
   private final SimpleOption<Double> gravity = slider("text.wc.setting.gravity", 0.0, 40.0, this.config.gravity, value -> this.config.gravity = value.floatValue());
   private final SimpleOption<Double> damping = slider("text.wc.setting.damping", 0.0, 15.0, this.config.damping, value -> this.config.damping = value.floatValue());
   private final SimpleOption<Double> windStrength = slider("text.wc.setting.windStrength", 0.0, 10.0, this.config.windStrength, value -> this.config.windStrength = value.floatValue());

   public WaveyCapesConfigScreen(Screen parent) {
      super(parent, MinecraftClient.getInstance().options, Text.translatable("text.wc.title"));
   }

   private SimpleOption<Boolean> toggle(String key, boolean initial, java.util.function.Consumer<Boolean> setter) {
      return SimpleOption.ofBoolean(key, initial, value -> { setter.accept(value); WaveyCapesConfig.save(); });
   }

   private SimpleOption<Double> slider(String key, double min, double max, double initial, java.util.function.Consumer<Double> setter) {
      return new SimpleOption<>(key, SimpleOption.emptyTooltip(), (caption, value) -> Text.literal(caption.getString() + ": " + String.format("%.2f", value)),
         SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(progress -> min + progress * (max - min), value -> (value - min) / (max - min)),
         initial, value -> { setter.accept(value); WaveyCapesConfig.save(); });
   }

   @Override
   protected void addOptions() {
      if (this.body != null) this.body.addAll(this.enabled, this.smooth, this.wind, this.gravity, this.damping, this.windStrength);
   }
}
