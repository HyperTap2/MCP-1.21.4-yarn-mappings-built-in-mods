package net.irisshaders.iris.gui.screen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.OldImageButton;
import net.irisshaders.iris.gui.element.ShaderPackOptionList;
import net.irisshaders.iris.gui.element.ShaderPackSelectionList;
import net.irisshaders.iris.gui.element.screen.IrisButton;
import net.irisshaders.iris.gui.element.widget.AbstractElementWidget;
import net.irisshaders.iris.gui.element.widget.CommentedElementWidget;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class ShaderPackScreen extends Screen implements HudHideable {
   public static final Set<Runnable> TOP_LAYER_RENDER_QUEUE = new HashSet<>();
   private static final Text SELECT_TITLE = Text.translatable("pack.iris.select.title").formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC});
   private static final Text CONFIGURE_TITLE = Text.translatable("pack.iris.configure.title").formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC});
   private static final int COMMENT_PANEL_WIDTH = 314;
   private static final String development = "Development Environment";
   private final Screen parent;
   private final MutableText irisTextComponent;
   private final FrameUpdateNotifier notifier = new FrameUpdateNotifier();
   private ShaderPackSelectionList shaderPackList;
   @Nullable
   private ShaderPackOptionList shaderOptionList = null;
   @Nullable
   private NavigationController navigation = null;
   private ButtonWidget screenSwitchButton;
   private Text notificationDialog = null;
   private int notificationDialogTimer = 0;
   @Nullable
   private AbstractElementWidget<?> hoveredElement = null;
   private Optional<Text> hoveredElementCommentTitle = Optional.empty();
   private List<OrderedText> hoveredElementCommentBody = new ArrayList<>();
   private int hoveredElementCommentTimer = 0;
   private boolean optionMenuOpen = false;
   private boolean dropChanges = false;
   private MutableText developmentComponent;
   private MutableText updateComponent;
   private boolean guiHidden = false;
   public final SmoothedFloat blurTransition = new SmoothedFloat(2.0F, 2.0F, () -> {
      if (this.guiHidden) {
         return 0.0F;
      } else {
         return this.optionMenuOpen ? 0.1F : this.client.options.getMenuBackgroundBlurrinessValue();
      }
   }, this.notifier);
   private float guiButtonHoverTimer = 0.0F;
   private ButtonWidget openFolderButton;
   private float backgroundInit = 0.0F;
   public final SmoothedFloat listTransition = new SmoothedFloat(
      1.0F, 1.0F, () -> !this.guiHidden && !this.optionMenuOpen ? this.backgroundInit : 0.0F, this.notifier
   );
   public final SmoothedFloat buttonTransition = new SmoothedFloat(1.0F, 1.0F, () -> this.guiHidden ? 0.0F : this.backgroundInit, this.notifier);
   private OldImageButton showHideButton;
   private static final Identifier BLUR_POST_CHAIN_ID = Identifier.ofVanilla("blur");

   public ShaderPackScreen(Screen parent) {
      super(Text.translatable("options.iris.shaderPackSelection.title"));
      this.parent = parent;
      String irisName = "Iris " + Iris.getVersion();
      if (IrisPlatformHelpers.getInstance().isDevelopmentEnvironment()) {
         this.developmentComponent = Text.literal("Development Environment").formatted(Formatting.GOLD);
      }

      this.irisTextComponent = Text.literal(irisName).formatted(Formatting.GRAY);
      if (Iris.getUpdateChecker().getUpdateMessage().isPresent()) {
         this.updateComponent = Text.literal("New update available!").formatted(Formatting.GREEN).formatted(Formatting.UNDERLINE);
         this.irisTextComponent.append(Text.literal(" (outdated)").formatted(Formatting.RED));
      }

      this.refreshForChangedPack();
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.notifier.onNewFrame();
      this.backgroundInit = 1.0F;
      if (Screen.hasControlDown() && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 68)) {
         MinecraftClient.getInstance()
            .setScreen(
               new ConfirmScreen(
                  option -> {
                     Iris.setDebug(option);
                     MinecraftClient.getInstance().setScreen(this);
                  },
                  Text.literal("Shader debug mode toggle"),
                  Text.literal("Debug mode helps investigate problems and shows shader errors. Would you like to enable it?"),
                  Text.literal("Yes"),
                  Text.literal("No")
               )
            );
      }

      if (Screen.hasControlDown() && InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 71)) {
         MinecraftClient.getInstance().setScreen(new ConfirmScreen(option -> {
            try {
               Iris.getIrisConfig().setUnknown(option);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }

            MinecraftClient.getInstance().setScreen(this);
         }, Text.literal("Unknown shader toggle"), Text.literal("This allows unknown shaders to load in."), Text.literal("Enable"), Text.literal("Disable")));
      }

      if (!this.guiHidden) {
         super.render(context, mouseX, mouseY, delta);
         if (this.optionMenuOpen && this.shaderOptionList != null) {
            this.shaderOptionList.render(context, mouseX, mouseY, delta);
         } else {
            this.shaderPackList.render(context, mouseX, mouseY, delta);
         }
      } else {
         this.showHideButton.render(context, mouseX, mouseY, delta);
      }

      float previousHoverTimer = this.guiButtonHoverTimer;
      if (previousHoverTimer == this.guiButtonHoverTimer) {
         this.guiButtonHoverTimer = 0.0F;
      }

      if (!this.guiHidden) {
         context.drawCenteredTextWithShadow(this.textRenderer, this.title, (int)(this.width * 0.5), 8, 16777215);
         if (this.notificationDialog != null && this.notificationDialogTimer > 0) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.notificationDialog, (int)(this.width * 0.5), 21, 16777215);
         } else if (this.optionMenuOpen) {
            context.drawCenteredTextWithShadow(this.textRenderer, CONFIGURE_TITLE, (int)(this.width * 0.5), 21, 16777215);
         } else {
            context.drawCenteredTextWithShadow(this.textRenderer, SELECT_TITLE, (int)(this.width * 0.5), 21, 16777215);
         }

         if (this.isDisplayingComment()) {
            int panelHeight = Math.max(50, 18 + this.hoveredElementCommentBody.size() * 10);
            int x = (int)(0.5 * this.width) - 157;
            int y = this.height - (panelHeight + 4);
            GuiUtil.drawPanel(context, x, y, 314, panelHeight);
            context.drawTextWithShadow(this.textRenderer, this.hoveredElementCommentTitle.orElse(Text.empty()), x + 4, y + 4, 16777215);

            for (int i = 0; i < this.hoveredElementCommentBody.size(); i++) {
               context.drawTextWithShadow(this.textRenderer, this.hoveredElementCommentBody.get(i), x + 4, y + 16 + i * 10, 16777215);
            }
         }
      }

      for (Runnable render : TOP_LAYER_RENDER_QUEUE) {
         render.run();
      }

      TOP_LAYER_RENDER_QUEUE.clear();
      if (this.developmentComponent != null) {
         context.drawTextWithShadow(this.textRenderer, this.developmentComponent, 2, this.height - 10, 16777215);
         context.drawTextWithShadow(this.textRenderer, this.irisTextComponent, 2, this.height - 20, 16777215);
      } else if (this.updateComponent != null) {
         context.drawTextWithShadow(this.textRenderer, this.updateComponent, 2, this.height - 10, 16777215);
         context.drawTextWithShadow(this.textRenderer, this.irisTextComponent, 2, this.height - 20, 16777215);
      } else {
         context.drawTextWithShadow(this.textRenderer, this.irisTextComponent, 2, this.height - 10, 16777215);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int widthValue = this.textRenderer.getWidth("New update available!");
      if (this.updateComponent != null && mouseX < widthValue && mouseY > this.height - 10 && mouseY < this.height) {
         this.client.setScreen(new ConfirmLinkScreen(bl -> {
            if (bl) {
               Iris.getUpdateChecker().getUpdateLink().ifPresent(Util.getOperatingSystem()::open);
            }

            this.client.setScreen(this);
         }, Iris.getUpdateChecker().getUpdateLink().orElse(""), true));
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   protected void init() {
      super.init();
      int bottomCenter = this.width / 2 - 50;
      int topCenter = this.width / 2 - 76;
      boolean inWorld = this.client.world != null;
      this.remove(this.shaderPackList);
      this.remove(this.shaderOptionList);
      this.shaderPackList = new ShaderPackSelectionList(this, this.client, this.width, this.height, 32, this.height - 58 - 36, 0, this.width);
      if (Iris.getCurrentPack().isPresent() && this.navigation != null) {
         ShaderPack currentPack = Iris.getCurrentPack().get();
         this.shaderOptionList = new ShaderPackOptionList(
            this, this.navigation, currentPack, this.client, this.width, this.height, 32, this.height - 58 - 36, 0, this.width
         );
         this.navigation.setActiveOptionList(this.shaderOptionList);
         this.shaderOptionList.rebuild();
      } else {
         this.optionMenuOpen = false;
         this.shaderOptionList = null;
      }

      this.clearChildren();
      if (!this.guiHidden) {
         if (this.optionMenuOpen && this.shaderOptionList != null) {
            this.addDrawableChild(this.shaderOptionList);
         } else {
            this.addDrawableChild(this.shaderPackList);
         }

         this.addDrawableChild(
            IrisButton.iris$builder(ScreenTexts.DONE, button -> this.close(), this.buttonTransition)
               .bounds(bottomCenter + 104, this.height - 27, 100, 20)
               .build()
         );
         this.addDrawableChild(
            IrisButton.iris$builder(Text.translatable("options.iris.apply"), button -> this.applyChanges(), this.buttonTransition)
               .bounds(bottomCenter, this.height - 27, 100, 20)
               .build()
         );
         this.addDrawableChild(
            IrisButton.iris$builder(ScreenTexts.CANCEL, button -> this.dropChangesAndClose(), this.buttonTransition)
               .bounds(bottomCenter - 104, this.height - 27, 100, 20)
               .build()
         );
         this.openFolderButton = IrisButton.iris$builder(
               Text.translatable("options.iris.openShaderPackFolder"), button -> this.openShaderPackFolder(), this.buttonTransition
            )
            .bounds(topCenter - 78, this.height - 51, 152, 20)
            .build();
         this.addDrawableChild(this.openFolderButton);
         this.screenSwitchButton = (ButtonWidget)this.addDrawableChild(IrisButton.iris$builder(Text.translatable("options.iris.shaderPackList"), button -> {
            this.optionMenuOpen = !this.optionMenuOpen;
            this.applyChanges();
            this.setFocused(this.shaderPackList.getFocused());
            this.init();
         }, this.buttonTransition).bounds(topCenter + 78, this.height - 51, 152, 20).build());
         this.refreshScreenSwitchButton();
      }

      if (inWorld) {
         Text showOrHide = this.guiHidden ? Text.translatable("options.iris.gui.show") : Text.translatable("options.iris.gui.hide");
         float endOfLastButton = this.width / 2.0F + 154.0F;
         float freeSpace = this.width - endOfLastButton;
         int x;
         if (freeSpace > 100.0F) {
            x = this.width - 50;
         } else if (freeSpace < 20.0F) {
            x = this.width - 20;
         } else {
            x = (int)(endOfLastButton + freeSpace / 2.0F) - 10;
         }

         this.showHideButton = new OldImageButton(
            x, this.height - 39, 20, 20, this.guiHidden ? 20 : 0, 146, 20, GuiUtil.IRIS_WIDGETS_TEX, 256, 256, button -> {
               this.guiHidden = !this.guiHidden;
               this.init();
            }, showOrHide
         );
         this.showHideButton.setTooltip(Tooltip.of(showOrHide));
         this.showHideButton.setTooltipDelay(Duration.ofSeconds(10L));
         this.addDrawableChild(this.showHideButton);
      }

      this.hoveredElement = null;
      this.hoveredElementCommentTimer = 0;
   }

   public void refreshForChangedPack() {
      if (Iris.getCurrentPack().isPresent()) {
         ShaderPack currentPack = Iris.getCurrentPack().get();
         this.navigation = new NavigationController(currentPack.getMenuContainer());
         if (this.shaderOptionList != null) {
            this.shaderOptionList.applyShaderPack(currentPack);
            this.shaderOptionList.rebuild();
         }
      } else {
         this.navigation = null;
      }

      this.refreshScreenSwitchButton();
   }

   public void refreshScreenSwitchButton() {
      if (this.screenSwitchButton != null) {
         this.screenSwitchButton
            .setMessage(this.optionMenuOpen ? Text.translatable("options.iris.shaderPackList") : Text.translatable("options.iris.shaderPackSettings"));
         this.screenSwitchButton.active = this.optionMenuOpen
            || this.shaderPackList.getTopButtonRow().shadersEnabled
               && Iris.getCurrentPack().map(p -> !p.getMenuContainer().mainScreen.elements.isEmpty()).orElse(true);
      }
   }

   private void processFixedBlur() {
      float f = Math.min(this.client.options.getMenuBackgroundBlurrinessValue(), this.blurTransition.getAsFloat());
      if (!(f < 1.0F)) {
         PostEffectProcessor postChain = this.client.getShaderLoader().loadPostEffect(BLUR_POST_CHAIN_ID, DefaultFramebufferSet.MAIN_ONLY);
         if (postChain != null) {
            postChain.setUniforms("Radius", f);
            postChain.render(this.client.getFramebuffer(), ((GameRendererAccessor)this.client.gameRenderer).getResourcePool());
         }
      }
   }

   protected void applyBlur() {
      this.processFixedBlur();
      this.client.getFramebuffer().beginWrite(false);
   }

   public void tick() {
      super.tick();
      if (this.notificationDialogTimer > 0) {
         this.notificationDialogTimer--;
      }

      if (this.hoveredElement != null) {
         this.hoveredElementCommentTimer++;
      } else {
         this.hoveredElementCommentTimer = 0;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         if (this.guiHidden) {
            this.guiHidden = false;
            this.init();
            return true;
         }

         if (this.navigation != null && this.navigation.hasHistory()) {
            this.navigation.back();
            return true;
         }

         if (this.optionMenuOpen) {
            this.optionMenuOpen = false;
            this.init();
            return true;
         }
      } else if (keyCode == 258) {
         if (!this.optionMenuOpen) {
            this.shaderPackList.keyPressed(257, 0, 0);
         }

         this.optionMenuOpen = !this.optionMenuOpen;
         this.applyChanges();
         this.init();
         this.setFocused(null);
      } else if (keyCode == 290 && this.showHideButton != null) {
         this.guiHidden = !this.guiHidden;
         this.init();
      }

      return this.guiHidden || super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void onFilesDropped(List<Path> paths) {
      if (this.optionMenuOpen) {
         this.onOptionMenuFilesDrop(paths);
      } else {
         this.onPackListFilesDrop(paths);
      }
   }

   public void onPackListFilesDrop(List<Path> paths) {
      List<Path> packs = paths.stream().filter(Iris::isValidShaderpack).toList();

      for (Path pack : packs) {
         String fileName = pack.getFileName().toString();

         try {
            Iris.getShaderpacksDirectoryManager().copyPackIntoDirectory(fileName, pack);
         } catch (FileAlreadyExistsException e) {
            this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.copyErrorAlreadyExists", new Object[]{fileName})
               .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
            this.notificationDialogTimer = 100;
            this.shaderPackList.refresh();
            return;
         } catch (IOException e) {
            Iris.logger.warn("Error copying dragged shader pack", e);
            this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.copyError", new Object[]{fileName})
               .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
            this.notificationDialogTimer = 100;
            this.shaderPackList.refresh();
            return;
         }
      }

      this.shaderPackList.refresh();
      if (packs.isEmpty()) {
         if (paths.size() == 1) {
            String fileName = paths.getFirst().getFileName().toString();
            this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.failedAddSingle", new Object[]{fileName})
               .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
         } else {
            this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.failedAdd")
               .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
         }
      } else if (packs.size() == 1) {
         String packName = packs.getFirst().getFileName().toString();
         this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.addedPack", new Object[]{packName})
            .formatted(new Formatting[]{Formatting.ITALIC, Formatting.YELLOW});
         this.shaderPackList.select(packName);
      } else {
         this.notificationDialog = Text.translatable("options.iris.shaderPackSelection.addedPacks", new Object[]{packs.size()})
            .formatted(new Formatting[]{Formatting.ITALIC, Formatting.YELLOW});
      }

      this.notificationDialogTimer = 100;
   }

   public void displayNotification(Text component) {
      this.notificationDialog = component;
      this.notificationDialogTimer = 100;
   }

   public void onOptionMenuFilesDrop(List<Path> paths) {
      if (paths.size() != 1) {
         this.notificationDialog = Text.translatable("options.iris.shaderPackOptions.tooManyFiles")
            .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
         this.notificationDialogTimer = 100;
      } else {
         this.importPackOptions(paths.getFirst());
      }
   }

   public void importPackOptions(Path settingFile) {
      try (InputStream in = Files.newInputStream(settingFile)) {
         Properties properties = new Properties();
         properties.load(in);
         Iris.queueShaderPackOptionsFromProperties(properties);
         this.notificationDialog = Text.translatable("options.iris.shaderPackOptions.importedSettings", new Object[]{settingFile.getFileName().toString()})
            .formatted(new Formatting[]{Formatting.ITALIC, Formatting.YELLOW});
         this.notificationDialogTimer = 100;
         if (this.navigation != null) {
            this.navigation.refresh();
         }
      } catch (Exception e) {
         Iris.logger.error("Error importing shader settings file \"" + settingFile.toString() + "\"", e);
         this.notificationDialog = Text.translatable("options.iris.shaderPackOptions.failedImport", new Object[]{settingFile.getFileName().toString()})
            .formatted(new Formatting[]{Formatting.ITALIC, Formatting.RED});
         this.notificationDialogTimer = 100;
      }
   }

   public void close() {
      if (!this.dropChanges) {
         this.applyChanges();
      } else {
         this.discardChanges();
      }

      try {
         this.shaderPackList.close();
      } catch (IOException e) {
         Iris.logger.error("Failed to safely close shaderpack selection!", e);
      }

      this.client.setScreen(this.parent);
   }

   private void dropChangesAndClose() {
      this.dropChanges = true;
      this.close();
   }

   public void applyChanges() {
      ShaderPackSelectionList.BaseEntry base = (ShaderPackSelectionList.BaseEntry)this.shaderPackList.getSelectedOrNull();
      boolean enabled = this.shaderPackList.getTopButtonRow().shadersEnabled;
      boolean previousShadersEnabled = Iris.getIrisConfig().areShadersEnabled();
      if (enabled != previousShadersEnabled) {
         IrisApi.getInstance().getConfig().setShadersEnabledAndApply(enabled);
      }

      if (base instanceof ShaderPackSelectionList.ShaderPackEntry entry) {
         this.shaderPackList.setApplied(entry);
         String name = entry.getPackName();
         if (!name.equals(Iris.getCurrentPackName())) {
            Iris.clearShaderPackOptionQueue();
         }

         String previousPackName = Iris.getIrisConfig().getShaderPackName().orElse(null);
         if (!name.equals(previousPackName) || !Iris.getShaderPackOptionQueue().isEmpty() || Iris.shouldResetShaderPackOptionsOnNextReload()) {
            Iris.getIrisConfig().setShaderPackName(name);
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(enabled);
         }

         this.refreshForChangedPack();
      }
   }

   private void discardChanges() {
      Iris.clearShaderPackOptionQueue();
   }

   private void openShaderPackFolder() {
      CompletableFuture.runAsync(() -> Util.getOperatingSystem().open(Iris.getShaderpacksDirectoryManager().getDirectoryUri()));
   }

   public void setElementHoveredStatus(AbstractElementWidget<?> widget, boolean hovered) {
      if (hovered && widget != this.hoveredElement) {
         this.hoveredElement = widget;
         if (widget instanceof CommentedElementWidget) {
            this.hoveredElementCommentTitle = ((CommentedElementWidget)widget).getCommentTitle();
            Optional<Text> commentBody = ((CommentedElementWidget)widget).getCommentBody();
            if (commentBody.isEmpty()) {
               this.hoveredElementCommentBody.clear();
            } else {
               String rawCommentBody = commentBody.get().getString();
               if (rawCommentBody.endsWith(".")) {
                  rawCommentBody = rawCommentBody.substring(0, rawCommentBody.length() - 1);
               }

               List<MutableText> splitByPeriods = Arrays.stream(rawCommentBody.split("\\. [ ]*")).<MutableText>map(Text::literal).toList();
               this.hoveredElementCommentBody = new ArrayList<>();

               for (MutableText text : splitByPeriods) {
                  this.hoveredElementCommentBody.addAll(this.textRenderer.wrapLines(text, 306));
               }
            }
         } else {
            this.hoveredElementCommentTitle = Optional.empty();
            this.hoveredElementCommentBody.clear();
         }

         this.hoveredElementCommentTimer = 0;
      } else if (!hovered && widget == this.hoveredElement) {
         this.hoveredElement = null;
         this.hoveredElementCommentTitle = Optional.empty();
         this.hoveredElementCommentBody.clear();
         this.hoveredElementCommentTimer = 0;
      }
   }

   public boolean isDisplayingComment() {
      return this.hoveredElementCommentTimer > 20 && this.hoveredElementCommentTitle.isPresent() && !this.hoveredElementCommentBody.isEmpty();
   }

   public ButtonWidget getBottomRowOption() {
      return this.openFolderButton;
   }
}
