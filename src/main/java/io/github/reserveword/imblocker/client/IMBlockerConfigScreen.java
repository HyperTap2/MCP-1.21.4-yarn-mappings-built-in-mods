package io.github.reserveword.imblocker.client;

import io.github.reserveword.imblocker.common.Config;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class IMBlockerConfigScreen extends Screen {
   private static final List<String> KEYS = List.of(
      "imblocker.screenWhitelist", "imblocker.screenBlacklist", "imblocker.inputWhitelist", "imblocker.inputBlacklist"
   );
   private final Screen parent;
   private final List<TextFieldWidget> fields = new ArrayList<>();
   private ButtonWidget recoveringButton;

   public IMBlockerConfigScreen(Screen parent) {
      super(Text.translatable("imblocker.settings"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.fields.clear();
      List<List<String>> values = List.of(
         Config.INSTANCE.screenWhitelist,
         Config.INSTANCE.screenBlacklist,
         Config.INSTANCE.inputWhitelist,
         Config.INSTANCE.inputBlacklist
      );
      int fieldWidth = Math.min(480, this.width - 40);
      int startY = 48;
      for (int index = 0; index < KEYS.size(); index++) {
         TextFieldWidget field = new TextFieldWidget(
            this.textRenderer, this.width / 2 - fieldWidth / 2, startY + index * 40, fieldWidth, 20, Text.translatable(KEYS.get(index))
         );
         field.setMaxLength(8192);
         field.setText(String.join(";", values.get(index)));
         this.fields.add(this.addDrawableChild(field));
      }

      int buttonsY = startY + KEYS.size() * 40 + 4;
      this.recoveringButton = this.addDrawableChild(
         ButtonWidget.builder(this.recoveringText(), button -> {
            Config.INSTANCE.enableScreenRecovering = !Config.INSTANCE.enableScreenRecovering;
            button.setMessage(this.recoveringText());
         }).dimensions(this.width / 2 - 154, buttonsY, 150, 20).build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close()).dimensions(this.width / 2 + 4, buttonsY, 150, 20).build()
      );
   }

   private Text recoveringText() {
      return Text.translatable(
         "imblocker.recoverScreens",
         Text.translatable(Config.INSTANCE.enableScreenRecovering ? "options.on" : "options.off")
      );
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 16, -1);
      for (int index = 0; index < this.fields.size(); index++) {
         context.drawTextWithShadow(this.textRenderer, Text.translatable(KEYS.get(index)), this.fields.get(index).getX(), this.fields.get(index).getY() - 12, -1);
      }
   }

   @Override
   public void close() {
      Config.INSTANCE.screenWhitelist = parse(this.fields.get(0).getText());
      Config.INSTANCE.screenBlacklist = parse(this.fields.get(1).getText());
      Config.INSTANCE.inputWhitelist = parse(this.fields.get(2).getText());
      Config.INSTANCE.inputBlacklist = parse(this.fields.get(3).getText());
      Config.INSTANCE.save();
      this.client.setScreen(this.parent);
   }

   private static ArrayList<String> parse(String text) {
      ArrayList<String> result = new ArrayList<>();
      Arrays.stream(text.split(";"))
         .map(String::trim)
         .filter(value -> Config.CLASS_NAME_PATTERN.matcher(value).matches())
         .distinct()
         .forEach(result::add);
      return result;
   }
}
