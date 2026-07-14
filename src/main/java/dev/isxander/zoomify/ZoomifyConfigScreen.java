package dev.isxander.zoomify;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;

public final class ZoomifyConfigScreen extends GameOptionsScreen {
   private final ZoomifyConfig config = ZoomifyConfig.get();
   private final SimpleOption<Double> zoom = slider(
      "yacl3.config.zoomify.category.behaviour.group.basic.option.initialZoom", 1.0, 50.0, this.config.zoomDivisor,
      value -> this.config.zoomDivisor = value
   );
   private final SimpleOption<Double> transition = slider(
      "yacl3.config.zoomify.category.behaviour.group.basic.option.zoomInTime", 0.0, 1.0, this.config.transitionSeconds,
      value -> this.config.transitionSeconds = value
   );
   private final SimpleOption<Double> sensitivity = slider(
      "yacl3.config.zoomify.category.controls.root.option.relativeSensitivity", 0.01, 1.0, this.config.zoomedSensitivity,
      value -> this.config.zoomedSensitivity = value
   );
   private final SimpleOption<Boolean> scroll = toggle(
      "yacl3.config.zoomify.category.behaviour.group.scrolling.option.scrollZoom", this.config.useScrollWheel,
      value -> this.config.useScrollWheel = value
   );
   private final SimpleOption<Boolean> retain = toggle(
      "yacl3.config.zoomify.category.behaviour.group.scrolling.option.retainZoomSteps", !this.config.resetZoomOnRelease,
      value -> this.config.resetZoomOnRelease = !value
   );
   private final SimpleOption<Boolean> cinematic = toggle(
      "yacl3.config.zoomify.category.controls.root.option.cinematicCamera", this.config.cinematicCamera,
      value -> this.config.cinematicCamera = value
   );
   private final SimpleOption<Boolean> toggleMode = toggle(
      "yacl3.config.zoomify.category.controls.root.option.zoomKeyBehaviour", this.config.toggleMode,
      value -> this.config.toggleMode = value
   );
   private final SimpleOption<Boolean> hideHand = toggle(
      "yacl3.config.zoomify.category.behaviour.group.basic.option.affectHandFov", this.config.hideHand,
      value -> this.config.hideHand = value
   );
   private final SimpleOption<Boolean> affectHandFov = toggle(
      "yacl3.config.zoomify.category.behaviour.group.basic.option.affectHandFov", this.config.affectHandFov,
      value -> this.config.affectHandFov = value
   );
   private final SimpleOption<Boolean> relativeBobbing = toggle(
      "yacl3.config.zoomify.category.controls.root.option.relativeViewBobbing", this.config.relativeViewBobbing,
      value -> this.config.relativeViewBobbing = value
   );
   private final SimpleOption<Double> secondaryZoom = slider(
      "yacl3.config.zoomify.category.secondary.root.option.secondaryZoomAmount", 1.0, 50.0, this.config.secondaryZoomDivisor,
      value -> this.config.secondaryZoomDivisor = value
   );
   private final SimpleOption<Boolean> secondaryHud = toggle(
      "yacl3.config.zoomify.category.secondary.root.option.secondaryHideHUDOnZoom", this.config.secondaryHideHud,
      value -> this.config.secondaryHideHud = value
   );
   private final SimpleOption<Boolean> spyglassOverride = toggle(
      "yacl3.config.zoomify.category.behaviour.group.spyglass.option.spyglassBehaviour",
      this.config.spyglassMode == ZoomifyConfig.SpyglassMode.OVERRIDE,
      value -> this.config.spyglassMode = value ? ZoomifyConfig.SpyglassMode.OVERRIDE : ZoomifyConfig.SpyglassMode.COMBINE
   );
   private final SimpleOption<Boolean> spyglassOverlay = toggle(
      "yacl3.config.zoomify.category.behaviour.group.spyglass.option.spyglassOverlayVisibility",
      this.config.overlayVisibility != ZoomifyConfig.OverlayVisibility.NEVER,
      value -> this.config.overlayVisibility = value ? ZoomifyConfig.OverlayVisibility.HOLDING : ZoomifyConfig.OverlayVisibility.NEVER
   );
   private final SimpleOption<Boolean> spyglassSound = toggle(
      "yacl3.config.zoomify.category.behaviour.group.spyglass.option.spyglassSoundBehaviour",
      this.config.soundMode != ZoomifyConfig.SoundMode.NEVER,
      value -> this.config.soundMode = value ? ZoomifyConfig.SoundMode.ALWAYS : ZoomifyConfig.SoundMode.NEVER
   );

   public ZoomifyConfigScreen(Screen parent) {
      super(parent, MinecraftClient.getInstance().options, Text.translatable("yacl3.config.zoomify.title"));
   }

   private SimpleOption<Boolean> toggle(String key, boolean initial, java.util.function.Consumer<Boolean> setter) {
      return SimpleOption.ofBoolean(key, initial, value -> { setter.accept(value); ZoomifyConfig.save(); });
   }

   private SimpleOption<Double> slider(String key, double min, double max, double initial, java.util.function.DoubleConsumer setter) {
      return new SimpleOption<>(key, SimpleOption.emptyTooltip(), (caption, value) -> Text.literal(caption.getString() + ": " + String.format("%.2f", value)),
         SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(progress -> min + progress * (max - min), value -> (value - min) / (max - min)),
         initial, value -> { setter.accept(value); ZoomifyConfig.save(); });
   }

   @Override
   protected void addOptions() {
      if (this.body != null) this.body.addAll(
         this.zoom, this.transition, this.sensitivity, this.scroll, this.retain, this.cinematic, this.toggleMode, this.affectHandFov,
         this.relativeBobbing, this.secondaryZoom, this.secondaryHud, this.spyglassOverride, this.spyglassOverlay, this.spyglassSound
      );
   }
}
