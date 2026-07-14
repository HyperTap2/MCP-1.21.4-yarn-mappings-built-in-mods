package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

public abstract class BaseOptionElementWidget<T extends OptionMenuElement> extends CommentedElementWidget<T> {
   protected static final Text SET_TO_DEFAULT = Text.translatable("options.iris.setToDefault").formatted(Formatting.GREEN);
   protected static final Text DIVIDER = Text.literal(": ");
   protected MutableText unmodifiedLabel;
   protected ShaderPackScreen screen;
   protected NavigationController navigation;
   protected Text trimmedLabel;
   protected Text valueLabel;
   protected boolean usedKeyboard;
   private MutableText label;
   private boolean isLabelTrimmed;
   private int maxLabelWidth;
   private int valueSectionWidth;

   public BaseOptionElementWidget(T element) {
      super(element);
   }

   @Override
   public void init(ShaderPackScreen screen, NavigationController navigation) {
      this.screen = screen;
      this.navigation = navigation;
      this.valueLabel = null;
      this.trimmedLabel = null;
   }

   protected final void setLabel(MutableText label) {
      this.label = label.copy().append(DIVIDER);
      this.unmodifiedLabel = label;
   }

   protected final void updateRenderParams(int minValueSectionWidth) {
      this.usedKeyboard = this.isFocused();
      if (this.valueLabel == null) {
         this.valueLabel = this.createValueLabel();
      }

      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      this.valueSectionWidth = Math.max(minValueSectionWidth, font.getWidth(this.valueLabel) + 8);
      this.maxLabelWidth = this.bounds.width() - 8 - this.valueSectionWidth;
      if (this.trimmedLabel == null || font.getWidth(this.label) > this.maxLabelWidth != this.isLabelTrimmed) {
         this.updateLabels();
      }

      this.isLabelTrimmed = font.getWidth(this.label) > this.maxLabelWidth;
   }

   protected final void renderOptionWithValue(DrawContext guiGraphics, boolean hovered, float sliderPosition, int sliderWidth) {
      GuiUtil.bindIrisWidgetsTexture();
      GuiUtil.drawButton(guiGraphics, this.bounds.position().x(), this.bounds.position().y(), this.bounds.width(), this.bounds.height(), hovered, false);
      GuiUtil.drawButton(
         guiGraphics,
         this.bounds.getBoundingCoordinate(NavigationDirection.RIGHT) - (this.valueSectionWidth + 2),
         this.bounds.position().y() + 2,
         this.valueSectionWidth,
         this.bounds.height() - 4,
         false,
         true
      );
      if (sliderPosition >= 0.0F) {
         int sliderSpace = this.valueSectionWidth - 4 - sliderWidth;
         int sliderPos = this.bounds.getBoundingCoordinate(NavigationDirection.RIGHT) - this.valueSectionWidth + (int)(sliderPosition * sliderSpace);
         GuiUtil.drawButton(guiGraphics, sliderPos, this.bounds.position().y() + 4, sliderWidth, this.bounds.height() - 8, false, false);
      }

      TextRenderer font = MinecraftClient.getInstance().textRenderer;
      guiGraphics.drawTextWithShadow(font, this.trimmedLabel, this.bounds.position().x() + 6, this.bounds.position().y() + 7, 16777215);
      guiGraphics.drawTextWithShadow(
         font,
         this.valueLabel,
         this.bounds.getBoundingCoordinate(NavigationDirection.RIGHT) - 2 - (int)(this.valueSectionWidth * 0.5) - (int)(font.getWidth(this.valueLabel) * 0.5),
         this.bounds.position().y() + 7,
         16777215
      );
   }

   protected final void renderOptionWithValue(DrawContext guiGraphics, boolean hovered) {
      this.renderOptionWithValue(guiGraphics, hovered, -1.0F, 0);
   }

   protected final void tryRenderTooltip(DrawContext guiGraphics, int mouseX, int mouseY, boolean hovered) {
      if (Screen.hasShiftDown()) {
         this.renderTooltip(guiGraphics, SET_TO_DEFAULT, mouseX, mouseY, hovered);
      } else if (this.isLabelTrimmed && !this.screen.isDisplayingComment()) {
         this.renderTooltip(guiGraphics, this.unmodifiedLabel, mouseX, mouseY, hovered);
      }
   }

   protected final void renderTooltip(DrawContext guiGraphics, Text text, int mouseX, int mouseY, boolean hovered) {
      if (hovered) {
         ShaderPackScreen.TOP_LAYER_RENDER_QUEUE
            .add(() -> GuiUtil.drawTextPanel(MinecraftClient.getInstance().textRenderer, guiGraphics, text, mouseX + 2, mouseY - 16));
      }
   }

   protected final void updateLabels() {
      this.trimmedLabel = this.createTrimmedLabel();
      this.valueLabel = this.createValueLabel();
   }

   protected final Text createTrimmedLabel() {
      MutableText label = GuiUtil.shortenText(MinecraftClient.getInstance().textRenderer, this.label.copy(), this.maxLabelWidth);
      if (this.isValueModified()) {
         label = label.styled(style -> style.withColor(TextColor.fromRgb(16763210)));
      }

      return label;
   }

   protected abstract Text createValueLabel();

   public abstract boolean applyNextValue();

   public abstract boolean applyPreviousValue();

   public abstract boolean applyOriginalValue();

   public abstract boolean isValueModified();

   @Nullable
   public abstract String getCommentKey();

   @Override
   public Optional<Text> getCommentTitle() {
      return Optional.of(this.unmodifiedLabel);
   }

   @Override
   public Optional<Text> getCommentBody() {
      return Optional.ofNullable(this.getCommentKey()).map(key -> I18n.hasTranslation(key) ? Text.translatable(key) : null);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0 && button != 1) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      boolean refresh = false;
      if (Screen.hasShiftDown()) {
         refresh = this.applyOriginalValue();
      }

      if (!refresh) {
         if (button == 0) {
            refresh = this.applyNextValue();
         } else {
            refresh = this.applyPreviousValue();
         }
      }

      if (refresh) {
         this.navigation.refresh();
      }

      GuiUtil.playButtonClickSound();
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 257) {
         boolean refresh = Screen.hasControlDown() ? this.applyOriginalValue() : (Screen.hasShiftDown() ? this.applyPreviousValue() : this.applyNextValue());
         if (refresh) {
            this.navigation.refresh();
         }

         GuiUtil.playButtonClickSound();
         return true;
      } else {
         return false;
      }
   }
}
