package net.irisshaders.iris.gui;

import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class FeatureMissingErrorScreen extends Screen {
   private final Screen parent;
   private final Text messageTemp;
   private MultilineText message;

   public FeatureMissingErrorScreen(Screen parent, Text title, Text message) {
      super(title);
      this.parent = parent;
      this.messageTemp = message;
   }

   protected void init() {
      super.init();
      this.message = MultilineText.create(this.textRenderer, this.width - 50, new Text[]{this.messageTemp});
      this.addDrawableChild(
         ButtonWidget.builder(ScreenTexts.BACK, arg -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, 140, 200, 20).build()
      );
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 90, 16777215);
      this.message.drawCenterWithShadow(context, this.width / 2, 110, 9, 16777215);
      super.render(context, mouseX, mouseY, delta);
   }
}
