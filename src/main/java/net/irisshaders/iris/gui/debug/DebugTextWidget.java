package net.irisshaders.iris.gui.debug;

import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.EmptyWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.gui.widget.Positioner;
import net.minecraft.client.gui.widget.ScrollableTextFieldWidget;
import net.minecraft.client.gui.widget.GridWidget.Adder;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class DebugTextWidget extends ScrollableTextFieldWidget {
   private final TextRenderer font;
   private final DebugTextWidget.Content content;

   public DebugTextWidget(int i, int j, int k, int l, TextRenderer arg, Exception exception) {
      super(i, j, k, l, Text.empty());
      this.font = arg;
      this.content = this.buildContent(exception);
   }

   private DebugTextWidget.Content buildContent(Exception exception) {
      if (exception instanceof ShaderCompileException sce) {
         return this.buildContentShader(sce);
      } else {
         DebugTextWidget.ContentBuilder lv = new DebugTextWidget.ContentBuilder(this.containerWidth());
         StackTraceElement[] elements = exception.getStackTrace();
         lv.addHeader(this.font, Text.literal("Error: "));
         lv.addSpacer(9);
         if (exception.getMessage() != null) {
            lv.addLine(this.font, Text.literal(exception.getMessage()));
         }

         lv.addSpacer(9);
         lv.addHeader(this.font, Text.literal("Stack trace: "));
         lv.addSpacer(9);

         for (int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];
            if (element != null) {
               lv.addLine(this.font, Text.literal(element.toString()));
               if (i < elements.length - 1) {
                  lv.addSpacer(9);
               }
            }
         }

         return lv.build();
      }
   }

   private DebugTextWidget.Content buildContentShader(ShaderCompileException sce) {
      DebugTextWidget.ContentBuilder lv = new DebugTextWidget.ContentBuilder(this.containerWidth());
      lv.addHeader(this.font, Text.literal("Shader compile error in " + sce.getFilename() + ": "));
      lv.addSpacer(9);
      lv.addLine(this.font, Text.literal(sce.getError()));
      return lv.build();
   }

   protected int getContentsHeight() {
      return this.content.container().getHeight();
   }

   protected boolean overflows() {
      return this.getContentsHeight() > this.height;
   }

   protected double getDeltaYPerScroll() {
      return 9.0;
   }

   protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
      int k = this.getY() + this.getTextMargin();
      int l = this.getX() + this.getTextMargin();
      context.getMatrices().push();
      context.getMatrices().translate(l, k, 0.0);
      this.content.container().forEachChild(element -> element.render(context, mouseX, mouseY, delta));
      context.getMatrices().pop();
   }

   protected void appendClickableNarrations(NarrationMessageBuilder builder) {
      builder.put(NarrationPart.TITLE, this.content.narration());
   }

   private int containerWidth() {
      return this.width - this.getPadding();
   }

   record Content(GridWidget container, Text narration) {
   }

   static class ContentBuilder {
      private final int width;
      private final GridWidget grid;
      private final Adder helper;
      private final Positioner alignHeader;
      private final MutableText narration = Text.empty();

      public ContentBuilder(int i) {
         this.width = i;
         this.grid = new GridWidget();
         this.grid.getMainPositioner().alignLeft();
         this.helper = this.grid.createAdder(1);
         this.helper.add(EmptyWidget.ofWidth(i));
         this.alignHeader = this.helper.copyPositioner().alignHorizontalCenter().marginX(32);
      }

      public void addLine(TextRenderer arg, Text arg2) {
         this.addLine(arg, arg2, 0);
      }

      public void addLine(TextRenderer arg, Text arg2, int i) {
         this.helper.add(new MultilineTextWidget(this.width, 1, arg2, arg), this.helper.copyPositioner().marginBottom(i));
         this.narration.append(arg2).append("\n");
      }

      public void addHeader(TextRenderer arg, Text arg2) {
         this.helper.add(new MultilineTextWidget(this.width - 64, 1, arg2, arg).setCentered(true), this.alignHeader);
         this.narration.append(arg2).append("\n");
      }

      public void addSpacer(int i) {
         this.helper.add(EmptyWidget.ofHeight(i));
      }

      public DebugTextWidget.Content build() {
         this.grid.refreshPositions();
         return new DebugTextWidget.Content(this.grid, this.narration);
      }
   }
}
