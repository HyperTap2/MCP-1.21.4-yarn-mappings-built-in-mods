package net.minecraft.client.gui.screen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.visuals.settings.VisualSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.navigation.Navigable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.narration.ScreenNarrator;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType.Default;
import net.minecraft.sound.MusicSound;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Screen extends AbstractParentElement implements Drawable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Text SCREEN_USAGE_TEXT = Text.translatable("narrator.screen.usage");
   protected static final CubeMapRenderer PANORAMA_RENDERER = new CubeMapRenderer(Identifier.ofVanilla("textures/gui/title/background/panorama"));
   protected static final RotatingCubeMapRenderer ROTATING_PANORAMA_RENDERER = new RotatingCubeMapRenderer(PANORAMA_RENDERER);
   public static final Identifier MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/menu_background.png");
   public static final Identifier HEADER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/header_separator.png");
   public static final Identifier FOOTER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/footer_separator.png");
   private static final Identifier INWORLD_MENU_BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_menu_background.png");
   public static final Identifier INWORLD_HEADER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_header_separator.png");
   public static final Identifier INWORLD_FOOTER_SEPARATOR_TEXTURE = Identifier.ofVanilla("textures/gui/inworld_footer_separator.png");
   protected final Text title;
   private final List<Element> children = Lists.newArrayList();
   private final List<Selectable> selectables = Lists.newArrayList();
   @Nullable
   protected MinecraftClient client;
   private boolean screenInitialized;
   public int width;
   public int height;
   private final List<Drawable> drawables = Lists.newArrayList();
   protected TextRenderer textRenderer;
   private static final long SCREEN_INIT_NARRATION_DELAY = TimeUnit.SECONDS.toMillis(2L);
   private static final long NARRATOR_MODE_CHANGE_DELAY = SCREEN_INIT_NARRATION_DELAY;
   private static final long MOUSE_MOVE_NARRATION_DELAY = 750L;
   private static final long MOUSE_PRESS_SCROLL_NARRATION_DELAY = 200L;
   private static final long KEY_PRESS_NARRATION_DELAY = 200L;
   private final ScreenNarrator narrator = new ScreenNarrator();
   private long elementNarrationStartTime = Long.MIN_VALUE;
   private long screenNarrationStartTime = Long.MAX_VALUE;
   @Nullable
   protected CyclingButtonWidget<NarratorMode> narratorToggleButton;
   @Nullable
   private Selectable selected;
   @Nullable
   private Screen.PositionedTooltip tooltip;
   protected final Executor executor = runnable -> this.client.execute(() -> {
      if (this.client.currentScreen == this) {
         runnable.run();
      }
   });

   protected Screen(Text title) {
      this.title = title;
   }

   public Text getTitle() {
      return this.title;
   }

   public Text getNarratedTitle() {
      return this.getTitle();
   }

   public final void renderWithTooltip(DrawContext context, int mouseX, int mouseY, float delta) {
      this.render(context, mouseX, mouseY, delta);
      if (this.tooltip != null) {
         context.drawTooltip(this.textRenderer, this.tooltip.tooltip(), this.tooltip.positioner(), mouseX, mouseY);
         this.tooltip = null;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.renderBackground(context, mouseX, mouseY, delta);

      for (Drawable drawable : this.drawables) {
         drawable.render(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256 && this.shouldCloseOnEsc()) {
         this.close();
         return true;
      }

      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      }

      GuiNavigation guiNavigation = switch (keyCode) {
         case 258 -> this.getTabNavigation();
         default -> null;
         case 262 -> this.getArrowNavigation(NavigationDirection.RIGHT);
         case 263 -> this.getArrowNavigation(NavigationDirection.LEFT);
         case 264 -> this.getArrowNavigation(NavigationDirection.DOWN);
         case 265 -> this.getArrowNavigation(NavigationDirection.UP);
      };
      if (guiNavigation != null) {
         GuiNavigationPath guiNavigationPath = super.getNavigationPath(guiNavigation);
         if (guiNavigationPath == null && guiNavigation instanceof GuiNavigation.Tab) {
            this.blur();
            guiNavigationPath = super.getNavigationPath(guiNavigation);
         }

         if (guiNavigationPath != null) {
            this.switchFocus(guiNavigationPath);
         }
      }

      return false;
   }

   private GuiNavigation.Tab getTabNavigation() {
      boolean bl = !hasShiftDown();
      return new GuiNavigation.Tab(bl);
   }

   private GuiNavigation.Arrow getArrowNavigation(NavigationDirection direction) {
      return new GuiNavigation.Arrow(direction);
   }

   protected void setInitialFocus() {
      if (this.client.getNavigationType().isKeyboard()) {
         GuiNavigation.Tab tab = new GuiNavigation.Tab(true);
         GuiNavigationPath guiNavigationPath = super.getNavigationPath(tab);
         if (guiNavigationPath != null) {
            this.switchFocus(guiNavigationPath);
         }
      }
   }

   protected void setInitialFocus(Element element) {
      GuiNavigationPath guiNavigationPath = GuiNavigationPath.of(this, element.getNavigationPath(new GuiNavigation.Down()));
      if (guiNavigationPath != null) {
         this.switchFocus(guiNavigationPath);
      }
   }

   public void blur() {
      GuiNavigationPath guiNavigationPath = this.getFocusedPath();
      if (guiNavigationPath != null) {
         guiNavigationPath.setFocused(false);
      }
   }

   @VisibleForTesting
   protected void switchFocus(GuiNavigationPath path) {
      this.blur();
      path.setFocused(true);
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void close() {
      this.client.setScreen(null);
   }

   protected <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement) {
      if (drawableElement instanceof TexturedButtonWidget && this instanceof RecipeBookScreen<?>) {
         boolean furnace = this instanceof FurnaceScreen;
         if (VisualSettings.INSTANCE.hideFurnaceRecipeBook.isEnabled() && furnace
            || VisualSettings.INSTANCE.hideCraftingRecipeBook.isEnabled() && !furnace) {
            return drawableElement;
         }
      }

      this.drawables.add(drawableElement);
      return this.addSelectableChild(drawableElement);
   }

   protected <T extends Drawable> T addDrawable(T drawable) {
      this.drawables.add(drawable);
      return drawable;
   }

   protected <T extends Element & Selectable> T addSelectableChild(T child) {
      this.children.add(child);
      this.selectables.add(child);
      return child;
   }

   protected void remove(Element child) {
      if (child instanceof Drawable) {
         this.drawables.remove((Drawable)child);
      }

      if (child instanceof Selectable) {
         this.selectables.remove((Selectable)child);
      }

      this.children.remove(child);
   }

   protected void clearChildren() {
      this.drawables.clear();
      this.children.clear();
      this.selectables.clear();
   }

   public static List<Text> getTooltipFromItem(MinecraftClient client, ItemStack stack) {
      return stack.getTooltip(TooltipContext.create(client.world), client.player, client.options.advancedItemTooltips ? Default.ADVANCED : Default.BASIC);
   }

   protected void insertText(String text, boolean override) {
   }

   public boolean handleTextClick(@Nullable Style style) {
      if (style == null) {
         return false;
      }

      ClickEvent clickEvent = style.getClickEvent();
      if (hasShiftDown()) {
         if (style.getInsertion() != null) {
            this.insertText(style.getInsertion(), false);
         }
      } else if (clickEvent != null) {
         if (clickEvent.getAction() == Action.OPEN_URL) {
            if (!this.client.options.getChatLinks().getValue()) {
               return false;
            }

            try {
               URI uRI = Util.validateUri(clickEvent.getValue());
               if (this.client.options.getChatLinksPrompt().getValue()) {
                  this.client.setScreen(new ConfirmLinkScreen(confirmed -> {
                     if (confirmed) {
                        Util.getOperatingSystem().open(uRI);
                     }

                     this.client.setScreen(this);
                  }, clickEvent.getValue(), false));
               } else {
                  Util.getOperatingSystem().open(uRI);
               }
            } catch (URISyntaxException uRISyntaxException) {
               LOGGER.error("Can't open url for {}", clickEvent, uRISyntaxException);
            }
         } else if (clickEvent.getAction() == Action.OPEN_FILE) {
            Util.getOperatingSystem().open(new File(clickEvent.getValue()));
         } else if (clickEvent.getAction() == Action.SUGGEST_COMMAND) {
            this.insertText(StringHelper.stripInvalidChars(clickEvent.getValue()), true);
         } else if (clickEvent.getAction() == Action.RUN_COMMAND) {
            String string = StringHelper.stripInvalidChars(clickEvent.getValue());
            if (string.startsWith("/")) {
               if (!this.client.player.networkHandler.sendCommand(string.substring(1))) {
                  LOGGER.error("Not allowed to run command with signed argument from click event: '{}'", string);
               }
            } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19)) {
               this.client.player.networkHandler.sendChatMessage(StringHelper.stripInvalidChars(clickEvent.getValue()));
            } else {
               LOGGER.error("Failed to run command without '/' prefix from click event: '{}'", string);
            }
         } else if (clickEvent.getAction() == Action.COPY_TO_CLIPBOARD) {
            this.client.keyboard.setClipboard(clickEvent.getValue());
         } else {
            LOGGER.error("Don't know how to handle {}", clickEvent);
         }

         return true;
      }

      return false;
   }

   public final void init(MinecraftClient client, int width, int height) {
      this.client = client;
      this.textRenderer = client.textRenderer;
      this.width = width;
      this.height = height;
      if (!this.screenInitialized) {
         this.init();
         this.setInitialFocus();
      } else {
         this.refreshWidgetPositions();
      }

      this.screenInitialized = true;
      this.narrateScreenIfNarrationEnabled(false);
      this.setElementNarrationDelay(SCREEN_INIT_NARRATION_DELAY);
   }

   protected void clearAndInit() {
      this.clearChildren();
      this.blur();
      this.init();
      this.setInitialFocus();
   }

   @Override
   public List<? extends Element> children() {
      return this.children;
   }

   protected void init() {
   }

   public void tick() {
   }

   public void removed() {
   }

   public void onDisplayed() {
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.client.world == null) {
         this.renderPanoramaBackground(context, delta);
      }

      this.applyBlur();
      this.renderDarkening(context);
   }

   protected void applyBlur() {
      this.client.gameRenderer.renderBlur();
      this.client.getFramebuffer().beginWrite(false);
   }

   protected void renderPanoramaBackground(DrawContext context, float delta) {
      ROTATING_PANORAMA_RENDERER.render(context, this.width, this.height, 1.0F, delta);
   }

   protected void renderDarkening(DrawContext context) {
      this.renderDarkening(context, 0, 0, this.width, this.height);
   }

   protected void renderDarkening(DrawContext context, int x, int y, int width, int height) {
      renderBackgroundTexture(context, this.client.world == null ? MENU_BACKGROUND_TEXTURE : INWORLD_MENU_BACKGROUND_TEXTURE, x, y, 0.0F, 0.0F, width, height);
   }

   public static void renderBackgroundTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height) {
      int i = 32;
      context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, u, v, width, height, 32, 32);
   }

   public void renderInGameBackground(DrawContext context) {
      context.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
   }

   public boolean shouldPause() {
      return true;
   }

   public static boolean hasControlDown() {
      return MinecraftClient.IS_SYSTEM_MAC
         ? InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 343)
            || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 347)
         : InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 341)
            || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 345);
   }

   public static boolean hasShiftDown() {
      return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 340)
         || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 344);
   }

   public static boolean hasAltDown() {
      return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 342)
         || InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 346);
   }

   public static boolean isCut(int code) {
      return code == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isPaste(int code) {
      return code == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isCopy(int code) {
      return code == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   public static boolean isSelectAll(int code) {
      return code == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
   }

   protected void refreshWidgetPositions() {
      this.clearAndInit();
   }

   public void resize(MinecraftClient client, int width, int height) {
      this.width = width;
      this.height = height;
      this.refreshWidgetPositions();
   }

   public void addCrashReportSection(CrashReport report) {
      CrashReportSection crashReportSection = report.addElement("Affected screen", 1);
      crashReportSection.add("Screen name", () -> this.getClass().getCanonicalName());
   }

   protected boolean isValidCharacterForName(String name, char character, int cursorPos) {
      int i = name.indexOf(58);
      int j = name.indexOf(47);
      if (character == ':') {
         return (j == -1 || cursorPos <= j) && i == -1;
      } else {
         return character == '/'
            ? cursorPos > i
            : character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
      }
   }

   @Override
   public boolean isMouseOver(double mouseX, double mouseY) {
      return true;
   }

   public void onFilesDropped(List<Path> paths) {
   }

   private void setScreenNarrationDelay(long delayMs, boolean restartElementNarration) {
      this.screenNarrationStartTime = Util.getMeasuringTimeMs() + delayMs;
      if (restartElementNarration) {
         this.elementNarrationStartTime = Long.MIN_VALUE;
      }
   }

   private void setElementNarrationDelay(long delayMs) {
      this.elementNarrationStartTime = Util.getMeasuringTimeMs() + delayMs;
   }

   public void applyMouseMoveNarratorDelay() {
      this.setScreenNarrationDelay(750L, false);
   }

   public void applyMousePressScrollNarratorDelay() {
      this.setScreenNarrationDelay(200L, true);
   }

   public void applyKeyPressNarratorDelay() {
      this.setScreenNarrationDelay(200L, true);
   }

   private boolean isNarratorActive() {
      return this.client.getNarratorManager().isActive();
   }

   public void updateNarrator() {
      if (this.isNarratorActive()) {
         long l = Util.getMeasuringTimeMs();
         if (l > this.screenNarrationStartTime && l > this.elementNarrationStartTime) {
            this.narrateScreen(true);
            this.screenNarrationStartTime = Long.MAX_VALUE;
         }
      }
   }

   public void narrateScreenIfNarrationEnabled(boolean onlyChangedNarrations) {
      if (this.isNarratorActive()) {
         this.narrateScreen(onlyChangedNarrations);
      }
   }

   private void narrateScreen(boolean onlyChangedNarrations) {
      this.narrator.buildNarrations(this::addScreenNarrations);
      String string = this.narrator.buildNarratorText(!onlyChangedNarrations);
      if (!string.isEmpty()) {
         this.client.getNarratorManager().narrate(string);
      }
   }

   protected boolean hasUsageText() {
      return true;
   }

   protected void addScreenNarrations(NarrationMessageBuilder messageBuilder) {
      messageBuilder.put(NarrationPart.TITLE, this.getNarratedTitle());
      if (this.hasUsageText()) {
         messageBuilder.put(NarrationPart.USAGE, SCREEN_USAGE_TEXT);
      }

      this.addElementNarrations(messageBuilder);
   }

   protected void addElementNarrations(NarrationMessageBuilder builder) {
      List<? extends Selectable> list = this.selectables
         .stream()
         .flatMap(selectable -> selectable.getNarratedParts().stream())
         .filter(Selectable::isNarratable)
         .sorted(Comparator.comparingInt(Navigable::getNavigationOrder))
         .toList();
      Screen.SelectedElementNarrationData selectedElementNarrationData = findSelectedElementData(list, this.selected);
      if (selectedElementNarrationData != null) {
         if (selectedElementNarrationData.selectType.isFocused()) {
            this.selected = selectedElementNarrationData.selectable;
         }

         if (list.size() > 1) {
            builder.put(
               NarrationPart.POSITION, Text.translatable("narrator.position.screen", new Object[]{selectedElementNarrationData.index + 1, list.size()})
            );
            if (selectedElementNarrationData.selectType == Selectable.SelectionType.FOCUSED) {
               builder.put(NarrationPart.USAGE, this.getUsageNarrationText());
            }
         }

         selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage());
      }
   }

   protected Text getUsageNarrationText() {
      return Text.translatable("narration.component_list.usage");
   }

   @Nullable
   public static Screen.SelectedElementNarrationData findSelectedElementData(List<? extends Selectable> selectables, @Nullable Selectable selectable) {
      Screen.SelectedElementNarrationData selectedElementNarrationData = null;
      Screen.SelectedElementNarrationData selectedElementNarrationData2 = null;
      int i = 0;

      for (int j = selectables.size(); i < j; i++) {
         Selectable selectable2 = selectables.get(i);
         Selectable.SelectionType selectionType = selectable2.getType();
         if (selectionType.isFocused()) {
            if (selectable2 != selectable) {
               return new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
            }

            selectedElementNarrationData2 = new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
         } else if (selectionType.compareTo(selectedElementNarrationData != null ? selectedElementNarrationData.selectType : Selectable.SelectionType.NONE) > 0
            )
          {
            selectedElementNarrationData = new Screen.SelectedElementNarrationData(selectable2, i, selectionType);
         }
      }

      return selectedElementNarrationData != null ? selectedElementNarrationData : selectedElementNarrationData2;
   }

   public void refreshNarrator(boolean previouslyDisabled) {
      if (previouslyDisabled) {
         this.setScreenNarrationDelay(NARRATOR_MODE_CHANGE_DELAY, false);
      }

      if (this.narratorToggleButton != null) {
         this.narratorToggleButton.setValue(this.client.options.getNarrator().getValue());
      }
   }

   protected void clearTooltip() {
      this.tooltip = null;
   }

   public void setTooltip(List<OrderedText> tooltip) {
      this.setTooltip(tooltip, HoveredTooltipPositioner.INSTANCE, true);
   }

   public void setTooltip(List<OrderedText> tooltip, TooltipPositioner positioner, boolean focused) {
      if (this.tooltip == null || focused) {
         this.tooltip = new Screen.PositionedTooltip(tooltip, positioner);
      }
   }

   public void setTooltip(Text tooltip) {
      this.setTooltip(Tooltip.wrapLines(this.client, tooltip));
   }

   public void setTooltip(Tooltip tooltip, TooltipPositioner positioner, boolean focused) {
      this.setTooltip(tooltip.getLines(this.client), positioner, focused);
   }

   public TextRenderer getTextRenderer() {
      return this.textRenderer;
   }

   public boolean shouldHideStatusEffectHud() {
      return false;
   }

   @Override
   public ScreenRect getNavigationFocus() {
      return new ScreenRect(0, 0, this.width, this.height);
   }

   @Nullable
   public MusicSound getMusic() {
      return null;
   }

   record PositionedTooltip(List<OrderedText> tooltip, TooltipPositioner positioner) {
   }

   public static class SelectedElementNarrationData {
      public final Selectable selectable;
      public final int index;
      public final Selectable.SelectionType selectType;

      public SelectedElementNarrationData(Selectable selectable, int index, Selectable.SelectionType selectType) {
         this.selectable = selectable;
         this.index = index;
         this.selectType = selectType;
      }
   }
}
