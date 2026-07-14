package net.irisshaders.iris.gui.debug;

import java.io.IOException;
import net.irisshaders.iris.Iris;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.SimplePositioningWidget;
import net.minecraft.text.Text;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class DebugLoadFailedGridScreen extends Screen {
   private final Exception exception;
   private final Screen parent;

   public DebugLoadFailedGridScreen(Screen parent, Text arg, Exception exception) {
      super(arg);
      this.parent = parent;
      this.exception = exception;
   }

   protected void init() {
      super.init();
      GridWidget widget = new GridWidget();
      Positioner layoutSettings = widget.copyPositioner().alignTop().alignHorizontalCenter();
      Positioner layoutSettings4 = widget.copyPositioner().alignTop().marginTop(30).alignHorizontalCenter();
      Positioner layoutSettings2 = widget.copyPositioner().alignTop().marginTop(30).alignLeft();
      Positioner layoutSettings3 = widget.copyPositioner().alignTop().marginTop(30).alignRight();
      int numWidgets = 0;
      widget.add(new DebugTextWidget(0, 0, this.width - 80, 9 * 15, this.textRenderer, this.exception), ++numWidgets, 0, 1, 2, layoutSettings);
      widget.add(
         ButtonWidget.builder(Text.translatable("menu.returnToGame"), arg2 -> this.client.setScreen(this.parent)).width(100).build(),
         ++numWidgets,
         0,
         1,
         2,
         layoutSettings2
      );
      widget.add(ButtonWidget.builder(Text.literal("Reload pack"), arg2 -> {
         MinecraftClient.getInstance().setScreen(this.parent);

         try {
            Iris.reload();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }).width(100).build(), numWidgets, 0, 1, 2, layoutSettings3);
      widget.add(
         ButtonWidget.builder(Text.literal("Copy error"), arg2 -> this.client.keyboard.setClipboard(ExceptionUtils.getStackTrace(this.exception)))
            .width(100)
            .build(),
         numWidgets,
         0,
         1,
         2,
         layoutSettings4
      );
      widget.refreshPositions();
      SimplePositioningWidget.setPos(widget, 0, 0, this.width, this.height);
      widget.forEachChild(x$0 -> {
         ClickableWidget var10000 = (ClickableWidget)this.addDrawableChild(x$0);
      });
   }
}
