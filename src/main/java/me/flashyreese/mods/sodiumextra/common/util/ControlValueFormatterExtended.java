package me.flashyreese.mods.sodiumextra.common.util;

import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Monitor;
import net.minecraft.text.Text;

public interface ControlValueFormatterExtended extends ControlValueFormatter {
   static ControlValueFormatter resolution() {
      Monitor monitor = MinecraftClient.getInstance().getWindow().getMonitor();
      return v -> {
         if (monitor == null) {
            return Text.translatable("options.fullscreen.unavailable");
         } else {
            return v == 0 ? Text.translatable("options.fullscreen.current") : Text.literal(monitor.getVideoMode(v - 1).toString());
         }
      };
   }

   static ControlValueFormatter fogDistance() {
      return v -> {
         if (v == 0) {
            return Text.translatable("options.gamma.default");
         } else {
            return v == 33 ? Text.translatable("options.off") : Text.translatable("options.chunks", new Object[]{v});
         }
      };
   }

   static ControlValueFormatter ticks() {
      return v -> Text.translatable("sodium-extra.units.ticks", new Object[]{v});
   }
}
