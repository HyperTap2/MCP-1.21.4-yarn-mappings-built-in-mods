package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Monitor;
import net.minecraft.text.Text;

public interface ControlValueFormatter {
   static ControlValueFormatter guiScale() {
      return v -> v == 0 ? Text.translatable("options.guiScale.auto") : Text.literal(v + "x");
   }

   static ControlValueFormatter resolution() {
      return v -> {
         Monitor monitor = MinecraftClient.getInstance().getWindow().getMonitor();
         if (OsUtils.getOs() != OsUtils.OperatingSystem.WIN || monitor == null) {
            return Text.translatable("options.fullscreen.unavailable");
         } else {
            return 0 == v ? Text.translatable("options.fullscreen.current") : Text.literal(monitor.getVideoMode(v - 1).toString().replace(" (24bit)", ""));
         }
      };
   }

   static ControlValueFormatter fpsLimit() {
      return v -> v == 260 ? Text.translatable("options.framerateLimit.max") : Text.translatable("options.framerate", v);
   }

   static ControlValueFormatter brightness() {
      return v -> {
         if (v == 0) {
            return Text.translatable("options.gamma.min");
         } else {
            return v == 100 ? Text.translatable("options.gamma.max") : Text.literal(v + "%");
         }
      };
   }

   static ControlValueFormatter biomeBlend() {
      return v -> v == 0 ? Text.translatable("gui.none") : Text.translatable("sodium.options.biome_blend.value", v);
   }

   Text format(int var1);

   static ControlValueFormatter translateVariable(String key) {
      return v -> Text.translatable(key, v);
   }

   static ControlValueFormatter percentage() {
      return v -> Text.literal(v + "%");
   }

   static ControlValueFormatter multiplier() {
      return v -> Text.literal(v + "x");
   }

   static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
      return v -> Text.literal(v == 0 ? disableText : v + " " + name);
   }

   static ControlValueFormatter number() {
      return v -> Text.literal(String.valueOf(v));
   }
}
