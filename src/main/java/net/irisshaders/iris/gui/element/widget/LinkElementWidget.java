package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuLinkElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.NavigationAxis;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class LinkElementWidget extends CommentedElementWidget<OptionMenuLinkElement> {
   private static final Text ARROW = Text.literal(">");
   private final String targetScreenId;
   private final MutableText label;
   private NavigationController navigation;
   private MutableText trimmedLabel = null;
   private boolean isLabelTrimmed = false;

   public LinkElementWidget(OptionMenuLinkElement element) {
      super(element);
      this.targetScreenId = element.targetScreenId;
      this.label = GuiUtil.translateOrDefault(Text.literal(element.targetScreenId), "screen." + element.targetScreenId);
   }

   @Override
   public void init(ShaderPackScreen screen, NavigationController navigation) {
      this.navigation = navigation;
   }

   @Override
   public void render(DrawContext guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
      GuiUtil.bindIrisWidgetsTexture();
      GuiUtil.drawButton(
         guiGraphics, this.bounds.position().x(), this.bounds.position().y(), this.bounds.width(), this.bounds.height(), hovered || this.isFocused(), false
      );
      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      int maxLabelWidth = this.bounds.width() - 9;
      if (font.getWidth(this.label) > maxLabelWidth) {
         this.isLabelTrimmed = true;
      }

      if (this.trimmedLabel == null) {
         this.trimmedLabel = GuiUtil.shortenText(font, this.label, maxLabelWidth);
      }

      int labelWidth = font.getWidth(this.trimmedLabel);
      guiGraphics.drawTextWithShadow(
         font,
         this.trimmedLabel,
         this.bounds.getCenter(NavigationAxis.HORIZONTAL) - (int)(labelWidth * 0.5) - (int)(0.5 * Math.max(labelWidth - (this.bounds.width() - 18), 0)),
         this.bounds.position().y() + 7,
         16777215
      );
      guiGraphics.drawTextWithShadow(font, ARROW, this.bounds.getBoundingCoordinate(NavigationDirection.RIGHT) - 9, this.bounds.position().y() + 7, 16777215);
      if (hovered && this.isLabelTrimmed) {
         ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(font, guiGraphics, this.label, mouseX + 2, mouseY - 16));
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.navigation.open(this.targetScreenId);
         GuiUtil.playButtonClickSound();
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257) {
         this.navigation.open(this.targetScreenId);
         GuiUtil.playButtonClickSound();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public Optional<Text> getCommentTitle() {
      return Optional.of(this.label);
   }

   @Override
   public Optional<Text> getCommentBody() {
      String translation = "screen." + this.targetScreenId + ".comment";
      return Optional.ofNullable(I18n.hasTranslation(translation) ? Text.translatable(translation) : null);
   }
}
