package net.irisshaders.iris.gui.element;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.FileDialogUtil;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.AbstractElementWidget;
import net.irisshaders.iris.gui.element.widget.OptionMenuConstructor;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ElementListWidget.Entry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShaderPackOptionList extends IrisContainerObjectSelectionList<ShaderPackOptionList.BaseEntry> {
   private static final Identifier MENU_LIST_BACKGROUND = Identifier.ofVanilla("textures/gui/menu_background.png");
   private final List<AbstractElementWidget<?>> elementWidgets = new ArrayList<>();
   private final ShaderPackScreen screen;
   private final NavigationController navigation;
   private OptionMenuContainer container;

   public ShaderPackOptionList(
      ShaderPackScreen screen,
      NavigationController navigation,
      ShaderPack pack,
      MinecraftClient client,
      int width,
      int height,
      int top,
      int bottom,
      int left,
      int right
   ) {
      super(client, width, bottom, top + 4, bottom, left, right, 24);
      this.navigation = navigation;
      this.screen = screen;
      this.applyShaderPack(pack);
   }

   public void applyShaderPack(ShaderPack pack) {
      this.container = pack.getMenuContainer();
   }

   public void rebuild() {
      this.clearEntries();
      this.setScrollY(0.0);
      OptionMenuConstructor.constructAndApplyToScreen(this.container, this.screen, this, this.navigation);
   }

   public void refresh() {
      this.elementWidgets.forEach(widget -> widget.init(this.screen, this.navigation));
   }

   public int getRowWidth() {
      return Math.min(400, this.width - 12);
   }

   protected void drawMenuListBackground(DrawContext context) {
      float transition = this.screen.listTransition.getAsFloat();
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(this.screen.listTransition.getAsFloat(), 0.01F));
      context.drawTexture(
         RenderLayer::getGuiTextured,
         MENU_LIST_BACKGROUND,
         this.getX(),
         this.getY(),
         this.getRight(),
         this.getBottom() + (int)this.getScrollY(),
         this.getWidth(),
         this.getHeight(),
         32,
         32
      );
      if (transition < 0.99F) {
         context.draw();
      }

      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void drawHeaderAndFooterSeparators(DrawContext context) {
      float transition = this.screen.listTransition.getAsFloat();
      if (!(transition < 0.02F)) {
         if (transition < 0.99F) {
            context.draw();
         }

         RenderSystem.enableBlend();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(this.screen.listTransition.getAsFloat(), 0.01F));
         context.drawTexture(
            RenderLayer::getGuiTextured, CreateWorldScreen.HEADER_SEPARATOR_TEXTURE, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2
         );
         context.drawTexture(
            RenderLayer::getGuiTextured, CreateWorldScreen.FOOTER_SEPARATOR_TEXTURE, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2
         );
         if (transition < 0.99F) {
            context.draw();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
      }
   }

   public void addHeader(Text text, boolean backButton) {
      this.addEntry(new ShaderPackOptionList.HeaderEntry(this.screen, this.navigation, text, backButton));
   }

   public void addWidgets(int columns, List<AbstractElementWidget<?>> elements) {
      this.elementWidgets.addAll(elements);
      List<AbstractElementWidget<?>> row = new ArrayList<>();

      for (AbstractElementWidget<?> element : elements) {
         row.add(element);
         if (row.size() >= columns) {
            this.addEntry(new ShaderPackOptionList.ElementRowEntry(this.screen, this.navigation, row));
            row = new ArrayList<>();
         }
      }

      if (!row.isEmpty()) {
         while (row.size() < columns) {
            row.add(AbstractElementWidget.EMPTY);
         }

         this.addEntry(new ShaderPackOptionList.ElementRowEntry(this.screen, this.navigation, row));
      }
   }

   public NavigationController getNavigation() {
      return this.navigation;
   }

   public abstract static class BaseEntry extends Entry<ShaderPackOptionList.BaseEntry> {
      protected final NavigationController navigation;

      protected BaseEntry(NavigationController navigation) {
         this.navigation = navigation;
      }
   }

   public static class ElementRowEntry extends ShaderPackOptionList.BaseEntry {
      private final List<AbstractElementWidget<?>> widgets;
      private final ShaderPackScreen screen;
      private int cachedWidth;
      private int cachedPosX;

      public ElementRowEntry(ShaderPackScreen screen, NavigationController navigation, List<AbstractElementWidget<?>> widgets) {
         super(navigation);
         this.screen = screen;
         this.widgets = widgets;
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         this.cachedWidth = entryWidth;
         this.cachedPosX = x;
         int totalWidthWithoutMargins = entryWidth - 2 * (this.widgets.size() - 1);
         totalWidthWithoutMargins -= 3;
         float singleWidgetWidth = (float)totalWidthWithoutMargins / this.widgets.size();

         for (int i = 0; i < this.widgets.size(); i++) {
            AbstractElementWidget<?> widget = this.widgets.get(i);
            boolean widgetHovered = hovered && this.getHoveredWidget(mouseX) == i || this.getFocused() == widget;
            widget.bounds = new ScreenRect(x + (int)((singleWidgetWidth + 2.0F) * i), y, (int)singleWidgetWidth, entryHeight + 2);
            widget.render(context, mouseX, mouseY, tickDelta, widgetHovered);
            this.screen.setElementHoveredStatus(widget, widgetHovered);
         }
      }

      public int getHoveredWidget(int mouseX) {
         float positionAcrossWidget = (float)MathHelper.clamp(mouseX - this.cachedPosX, 0, this.cachedWidth) / this.cachedWidth;
         return MathHelper.clamp((int)Math.floor(this.widgets.size() * positionAcrossWidget), 0, this.widgets.size() - 1);
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         return this.widgets.get(this.getHoveredWidget((int)mouseX)).mouseClicked(mouseX, mouseY, button);
      }

      public boolean mouseReleased(double mouseX, double mouseY, int button) {
         return this.widgets.get(this.getHoveredWidget((int)mouseX)).mouseReleased(mouseX, mouseY, button);
      }

      @NotNull
      public List<? extends Element> children() {
         return ImmutableList.copyOf(this.widgets);
      }

      @NotNull
      public List<? extends Selectable> selectableChildren() {
         return ImmutableList.copyOf(this.widgets);
      }
   }

   public class HeaderEntry extends ShaderPackOptionList.BaseEntry {
      public static final Text BACK_BUTTON_TEXT = Text.literal("< ").append(Text.translatable("options.iris.back").formatted(Formatting.ITALIC));
      public static final MutableText RESET_BUTTON_TEXT_INACTIVE = Text.translatable("options.iris.reset").formatted(Formatting.GRAY);
      public static final MutableText RESET_BUTTON_TEXT_ACTIVE = Text.translatable("options.iris.reset").formatted(Formatting.YELLOW);
      public static final MutableText RESET_HOLD_SHIFT_TOOLTIP = Text.translatable("options.iris.reset.tooltip.holdShift").formatted(Formatting.GOLD);
      public static final MutableText RESET_TOOLTIP = Text.translatable("options.iris.reset.tooltip").formatted(Formatting.RED);
      public static final MutableText IMPORT_TOOLTIP = Text.translatable("options.iris.importSettings.tooltip")
         .styled(style -> style.withColor(TextColor.fromRgb(5089023)));
      public static final MutableText EXPORT_TOOLTIP = Text.translatable("options.iris.exportSettings.tooltip")
         .styled(style -> style.withColor(TextColor.fromRgb(16547133)));
      private static final int MIN_SIDE_BUTTON_WIDTH = 42;
      private static final int BUTTON_HEIGHT = 16;
      private final ShaderPackScreen screen;
      @Nullable
      private final IrisElementRow backButton;
      private final IrisElementRow utilityButtons = new IrisElementRow();
      private final IrisElementRow.TextButtonElement resetButton;
      private final IrisElementRow.IconButtonElement importButton;
      private final IrisElementRow.IconButtonElement exportButton;
      private final Text text;

      public HeaderEntry(ShaderPackScreen screen, NavigationController navigation, Text text, boolean hasBackButton) {
         super(navigation);
         if (hasBackButton) {
            this.backButton = new IrisElementRow()
               .add(
                  new IrisElementRow.TextButtonElement(BACK_BUTTON_TEXT, this::backButtonClicked),
                  Math.max(42, MinecraftClient.getInstance().textRenderer.getWidth(BACK_BUTTON_TEXT) + 8)
               );
         } else {
            this.backButton = null;
         }

         this.resetButton = new IrisElementRow.TextButtonElement(RESET_BUTTON_TEXT_INACTIVE, this::resetButtonClicked);
         this.importButton = new IrisElementRow.IconButtonElement(GuiUtil.Icon.IMPORT, GuiUtil.Icon.IMPORT_COLORED, this::importSettingsButtonClicked);
         this.exportButton = new IrisElementRow.IconButtonElement(GuiUtil.Icon.EXPORT, GuiUtil.Icon.EXPORT_COLORED, this::exportSettingsButtonClicked);
         this.utilityButtons
            .add(this.importButton, 15)
            .add(this.exportButton, 15)
            .add(this.resetButton, Math.max(42, MinecraftClient.getInstance().textRenderer.getWidth(RESET_BUTTON_TEXT_INACTIVE) + 8));
         this.screen = screen;
         this.text = text;
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         context.fill(x - 3, y + entryHeight - 2, x + entryWidth, y + entryHeight - 1, 1723776702);
         TextRenderer font = MinecraftClient.getInstance().textRenderer;
         ShaderPackOptionList.drawScrollableText(
            context, font, this.text, x + (int)(entryWidth * 0.5), x + 5, y + 5, x + entryWidth - 10 - this.utilityButtons.getWidth(), y + 15, 16777215
         );
         GuiUtil.bindIrisWidgetsTexture();
         if (this.backButton != null) {
            this.backButton.render(context, x, y, 16, mouseX, mouseY, tickDelta, hovered);
         }

         boolean shiftDown = Screen.hasShiftDown();
         this.resetButton.disabled = !shiftDown && !this.resetButton.isFocused();
         this.resetButton.text = !this.resetButton.disabled ? RESET_BUTTON_TEXT_ACTIVE : RESET_BUTTON_TEXT_INACTIVE;
         this.utilityButtons.renderRightAligned(context, x + entryWidth - 3, y, 16, mouseX, mouseY, tickDelta, hovered);
         if (this.resetButton.isHovered() || this.resetButton.isFocused()) {
            Text tooltip = !this.resetButton.disabled ? RESET_TOOLTIP : RESET_HOLD_SHIFT_TOOLTIP;
            this.queueBottomRightAnchoredTooltip(
               context,
               this.resetButton.getNavigationFocus().getBoundingCoordinate(NavigationDirection.RIGHT),
               this.resetButton.getNavigationFocus().position().y(),
               font,
               tooltip
            );
         }

         if (this.importButton.isHovered() || this.importButton.isFocused()) {
            this.queueBottomRightAnchoredTooltip(
               context,
               this.importButton.getNavigationFocus().getBoundingCoordinate(NavigationDirection.RIGHT),
               this.importButton.getNavigationFocus().position().y(),
               font,
               IMPORT_TOOLTIP
            );
         }

         if (this.exportButton.isHovered() || this.exportButton.isFocused()) {
            this.queueBottomRightAnchoredTooltip(
               context,
               this.exportButton.getNavigationFocus().getBoundingCoordinate(NavigationDirection.RIGHT),
               this.exportButton.getNavigationFocus().position().y(),
               font,
               EXPORT_TOOLTIP
            );
         }
      }

      private void queueBottomRightAnchoredTooltip(DrawContext guiGraphics, int x, int y, TextRenderer font, Text text) {
         ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(font, guiGraphics, text, x - (font.getWidth(text) + 10), y - 16));
      }

      public List<? extends Element> children() {
         return this.backButton != null
            ? ImmutableList.copyOf(Iterables.concat(this.utilityButtons.children(), this.backButton.children()))
            : ImmutableList.copyOf(this.utilityButtons.children());
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         boolean backButtonResult = this.backButton != null && this.backButton.mouseClicked(mouseX, mouseY, button);
         boolean utilButtonResult = this.utilityButtons.mouseClicked(mouseX, mouseY, button);
         return backButtonResult || utilButtonResult;
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         return this.backButton != null && this.backButton.keyPressed(keyCode, scanCode, modifiers)
            ? true
            : this.utilityButtons.keyPressed(keyCode, scanCode, modifiers);
      }

      public List<? extends Selectable> selectableChildren() {
         return ImmutableList.of();
      }

      private boolean backButtonClicked(IrisElementRow.TextButtonElement button) {
         this.navigation.back();
         GuiUtil.playButtonClickSound();
         return true;
      }

      private boolean resetButtonClicked(IrisElementRow.TextButtonElement button) {
         if (Screen.hasShiftDown()) {
            Iris.resetShaderPackOptionsOnNextReload();
            this.screen.applyChanges();
            GuiUtil.playButtonClickSound();
            return true;
         } else {
            return false;
         }
      }

      private boolean importSettingsButtonClicked(IrisElementRow.IconButtonElement button) {
         GuiUtil.playButtonClickSound();
         if (Iris.getCurrentPack().isEmpty()) {
            return false;
         } else if (MinecraftClient.getInstance().getWindow().isFullscreen()) {
            this.screen.displayNotification(Text.translatable("options.iris.mustDisableFullscreen").formatted(Formatting.RED).formatted(Formatting.BOLD));
            return false;
         } else {
            ShaderPackScreen originalScreen = this.screen;
            FileDialogUtil.fileSelectDialog(
                  FileDialogUtil.DialogType.OPEN,
                  "Import Shader Settings from File",
                  Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt"),
                  "Shader Pack Settings (.txt)",
                  "*.txt"
               )
               .whenComplete((path, err) -> {
                  if (err != null) {
                     Iris.logger.error("Error selecting shader settings from file", err);
                  } else {
                     if (MinecraftClient.getInstance().currentScreen == originalScreen) {
                        path.ifPresent(originalScreen::importPackOptions);
                     }
                  }
               });
            return true;
         }
      }

      private boolean exportSettingsButtonClicked(IrisElementRow.IconButtonElement button) {
         GuiUtil.playButtonClickSound();
         if (Iris.getCurrentPack().isEmpty()) {
            return false;
         } else if (MinecraftClient.getInstance().getWindow().isFullscreen()) {
            this.screen.displayNotification(Text.translatable("options.iris.mustDisableFullscreen").formatted(Formatting.RED).formatted(Formatting.BOLD));
            return false;
         } else {
            FileDialogUtil.fileSelectDialog(
                  FileDialogUtil.DialogType.SAVE,
                  "Export Shader Settings to File",
                  Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt"),
                  "Shader Pack Settings (.txt)",
                  "*.txt"
               )
               .whenComplete((path, err) -> {
                  if (err != null) {
                     Iris.logger.error("Error selecting file to export shader settings", err);
                  } else {
                     path.ifPresent(p -> {
                        Properties toSave = new Properties();
                        Path sourceTxtPath = Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt");
                        if (Files.exists(sourceTxtPath)) {
                           try (InputStream in = Files.newInputStream(sourceTxtPath)) {
                              toSave.load(in);
                           } catch (IOException var11) {
                           }
                        }

                        try (OutputStream out = Files.newOutputStream(p)) {
                           toSave.store(out, null);
                        } catch (IOException e) {
                           Iris.logger.error("Error saving properties to \"" + p + "\"", e);
                        }
                     });
                  }
               });
            return true;
         }
      }
   }
}
