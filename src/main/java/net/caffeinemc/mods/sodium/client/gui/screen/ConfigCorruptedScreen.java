package net.caffeinemc.mods.sodium.client.gui.screen;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ConfigCorruptedScreen extends Screen {
   private static final String TEXT_BODY_RAW = "A problem occurred while trying to load the configuration file. This\ncan happen when the file has been corrupted on disk, or when trying\nto manually edit the file by hand.\n\nIf you continue, the configuration file will be reset back to known-good\ndefaults, and you will lose any changes that have since been made to your\nVideo Settings.\n\nMore information about the error can be found in the log file.\n";
   private static final List<Text> TEXT_BODY = Arrays.stream(
         "A problem occurred while trying to load the configuration file. This\ncan happen when the file has been corrupted on disk, or when trying\nto manually edit the file by hand.\n\nIf you continue, the configuration file will be reset back to known-good\ndefaults, and you will lose any changes that have since been made to your\nVideo Settings.\n\nMore information about the error can be found in the log file.\n"
            .split("\n")
      )
      .map(Text::literal)
      .collect(Collectors.toList());
   private static final int BUTTON_WIDTH = 140;
   private static final int BUTTON_HEIGHT = 20;
   private static final int SCREEN_PADDING = 32;
   @Nullable
   private final Screen prevScreen;
   private final Function<Screen, Screen> nextScreen;

   public ConfigCorruptedScreen(@Nullable Screen prevScreen, @Nullable Function<Screen, Screen> nextScreen) {
      super(Text.literal("Sodium failed to load the configuration file"));
      this.prevScreen = prevScreen;
      this.nextScreen = nextScreen;
   }

   @Override
   protected void init() {
      super.init();
      int buttonY = this.height - 32 - 20;
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Continue"), btn -> {
         Console.instance().logMessage(MessageLevel.INFO, "sodium.console.config_file_was_reset", true, 3.0);
         SodiumClientMod.restoreDefaultOptions();
         MinecraftClient.getInstance().setScreen(this.nextScreen.apply(this.prevScreen));
      }).dimensions(this.width - 32 - 140, buttonY, 140, 20).build());
      this.addDrawableChild(
         ButtonWidget.builder(Text.literal("Go back"), btn -> MinecraftClient.getInstance().setScreen(this.prevScreen))
            .dimensions(32, buttonY, 140, 20)
            .build()
      );
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Sodium Renderer"), 32, 32, 16777215);
      context.drawTextWithShadow(this.textRenderer, Text.literal("Could not load the configuration file"), 32, 48, 16711680);

      for (int i = 0; i < TEXT_BODY.size(); i++) {
         if (!TEXT_BODY.get(i).getString().isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, TEXT_BODY.get(i), 32, 68 + i * 12, 16777215);
         }
      }
   }
}
