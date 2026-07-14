package net.irisshaders.iris.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.navigation.GuiNavigation;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.EntryListWidget.Entry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class ShaderPackSelectionList extends IrisObjectSelectionList<ShaderPackSelectionList.BaseEntry> {
   private static final Text PACK_LIST_LABEL = Text.translatable("pack.iris.list.label").formatted(new Formatting[]{Formatting.ITALIC, Formatting.GRAY});
   private static final Identifier MENU_LIST_BACKGROUND = Identifier.ofVanilla("textures/gui/menu_background.png");
   private final ShaderPackScreen screen;
   private final ShaderPackSelectionList.TopButtonRowEntry topButtonRow;
   private final WatchService watcher;
   private final WatchKey key;
   private final ShaderPackSelectionList.PinnedEntry downloadButton;
   private boolean keyValid;
   private ShaderPackSelectionList.ShaderPackEntry applied = null;

   public ShaderPackSelectionList(ShaderPackScreen screen, MinecraftClient client, int width, int height, int top, int bottom, int left, int right) {
      super(client, width, bottom, top + 4, bottom, left, right, 20);
      this.screen = screen;
      this.topButtonRow = new ShaderPackSelectionList.TopButtonRowEntry(this, Iris.getIrisConfig().areShadersEnabled());
      this.downloadButton = new ShaderPackSelectionList.PinnedEntry(Text.literal("Download Shaders"), () -> this.client.setScreen(new ConfirmLinkScreen(bl -> {
         if (bl) {
            Util.getOperatingSystem().open("https://modrinth.com/shaders");
         }

         this.client.setScreen(this.screen);
      }, "https://modrinth.com/shaders", true)), this);

      WatchKey key1;
      WatchService watcher1;
      try {
         watcher1 = FileSystems.getDefault().newWatchService();
         key1 = Iris.getShaderpacksDirectory()
            .register(watcher1, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
         this.keyValid = true;
      } catch (IOException e) {
         Iris.logger.error("Couldn't register file watcher!", e);
         watcher1 = null;
         key1 = null;
         this.keyValid = false;
      }

      this.key = key1;
      this.watcher = watcher1;
      this.refresh();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return keyCode == 265 && this.getFocused() == this.getFirst() ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }

   public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
      if (this.keyValid) {
         for (WatchEvent<?> event : this.key.pollEvents()) {
            if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
               this.refresh();
               break;
            }
         }

         this.keyValid = this.key.reset();
      }

      super.renderWidget(context, mouseX, mouseY, delta);
   }

   public void close() throws IOException {
      if (this.key != null) {
         this.key.cancel();
      }

      if (this.watcher != null) {
         this.watcher.close();
      }
   }

   protected void drawMenuListBackground(DrawContext context) {
      float transition = this.screen.listTransition.getAsFloat();
      if (!(transition < 0.02F)) {
         if (transition < 0.99F) {
            context.draw();
         }

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

   public int getRowWidth() {
      return Math.min(308, this.width - 50);
   }

   public int getRowTop(int index) {
      return super.getRowTop(index) + 2;
   }

   public void refresh() {
      this.clearEntries();

      List<String> names;
      try {
         names = Iris.getShaderpacksDirectoryManager().enumerate();
      } catch (Throwable e) {
         Iris.logger.error("Error reading files while constructing selection UI", e);
         this.addLabelEntries(
            Text.empty(),
            Text.literal("There was an error reading your shaderpacks directory").formatted(new Formatting[]{Formatting.RED, Formatting.BOLD}),
            Text.empty(),
            Text.literal("Check your logs for more information."),
            Text.literal("Please file an issue report including a log file."),
            Text.literal("If you are able to identify the file causing this, please include it in your report as well."),
            Text.literal("Note that this might be an issue with folder permissions; ensure those are correct first.")
         );
         return;
      }

      this.addEntry(this.topButtonRow);
      if (names.isEmpty()) {
         this.addEntry(this.downloadButton);
      }

      this.topButtonRow.allowEnableShadersButton = !names.isEmpty();
      int index = 0;

      for (String name : names) {
         this.addPackEntry(++index, name);
      }

      this.addLabelEntries(PACK_LIST_LABEL);
   }

   public void addPackEntry(int index, String name) {
      ShaderPackSelectionList.ShaderPackEntry entry = new ShaderPackSelectionList.ShaderPackEntry(index, this, name);
      Iris.getIrisConfig().getShaderPackName().ifPresent(currentPackName -> {
         if (name.equals(currentPackName)) {
            this.setSelected(entry);
            this.setFocused(entry);
            this.centerScrollOn(entry);
            this.setApplied(entry);
         }
      });
      this.addEntry(entry);
   }

   public void addLabelEntries(Text... lines) {
      for (Text text : lines) {
         this.addEntry(new ShaderPackSelectionList.LabelEntry(text));
      }
   }

   public void select(String name) {
      for (int i = 0; i < this.getEntryCount(); i++) {
         ShaderPackSelectionList.BaseEntry entry = (ShaderPackSelectionList.BaseEntry)this.getEntry(i);
         if (entry instanceof ShaderPackSelectionList.ShaderPackEntry && ((ShaderPackSelectionList.ShaderPackEntry)entry).packName.equals(name)) {
            this.setSelected(entry);
            return;
         }
      }
   }

   public ShaderPackSelectionList.ShaderPackEntry getApplied() {
      return this.applied;
   }

   public void setApplied(ShaderPackSelectionList.ShaderPackEntry entry) {
      this.applied = entry;
   }

   public ShaderPackSelectionList.TopButtonRowEntry getTopButtonRow() {
      return this.topButtonRow;
   }

   public abstract static class BaseEntry extends Entry<ShaderPackSelectionList.BaseEntry> {
      protected BaseEntry() {
      }
   }

   public static class LabelEntry extends ShaderPackSelectionList.BaseEntry {
      private final Text label;

      public LabelEntry(Text label) {
         this.label = label;
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer, this.label, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 12763842
         );
      }
   }

   private static class PinnedEntry extends ShaderPackSelectionList.BaseEntry {
      public final boolean allowPressButton = true;
      private final Text label;
      private final Runnable onClick;

      public PinnedEntry(Text label, Runnable onClick, ShaderPackSelectionList list) {
         this.label = label;
         this.onClick = onClick;
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         GuiUtil.bindIrisWidgetsTexture();
         GuiUtil.drawButton(context, x - 2, y - 2, entryWidth, entryHeight + 2, hovered, false);
         context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer, this.label, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 16777215
         );
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         Objects.requireNonNull(this);
         GuiUtil.playButtonClickSound();
         this.onClick.run();
         return false;
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (keyCode == 257) {
            Objects.requireNonNull(this);
            GuiUtil.playButtonClickSound();
            this.onClick.run();
            return false;
         } else {
            return false;
         }
      }
   }

   public class ShaderPackEntry extends ShaderPackSelectionList.BaseEntry {
      private final String packName;
      private final ShaderPackSelectionList list;
      private final int index;
      private ScreenRect bounds = ScreenRect.empty();
      private boolean focused;

      public ShaderPackEntry(int index, ShaderPackSelectionList list, String packName) {
         this.packName = packName;
         this.list = list;
         this.index = index;
      }

      public ScreenRect getNavigationFocus() {
         return this.bounds;
      }

      public boolean isApplied() {
         return this.list.getApplied() == this;
      }

      public boolean isSelected() {
         return this.list.getSelectedOrNull() == this;
      }

      public String getPackName() {
         return this.packName;
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         this.bounds = new ScreenRect(x, y, entryWidth, entryHeight);
         TextRenderer font = MinecraftClient.getInstance().textRenderer;
         int color = 16777215;
         String name = this.packName;
         if (hovered) {
            GuiUtil.bindIrisWidgetsTexture();
            GuiUtil.drawButton(context, x - 2, y - 2, entryWidth, entryHeight + 4, hovered, false);
         }

         boolean shadersEnabled = this.list.getTopButtonRow().shadersEnabled;
         if (font.getWidth(Text.literal(name).formatted(Formatting.BOLD)) > this.list.getRowWidth() - 3) {
            name = font.trimToWidth(name, this.list.getRowWidth() - 8) + "...";
         }

         MutableText text = Text.literal(name);
         if (this.isMouseOver(mouseX, mouseY)) {
            text = text.formatted(Formatting.BOLD);
         }

         if (shadersEnabled && this.isApplied()) {
            color = 16773731;
         }

         if (!shadersEnabled && !this.isMouseOver(mouseX, mouseY)) {
            color = 10658466;
         }

         context.drawCenteredTextWithShadow(font, text, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, color);
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         return button != 0 ? false : this.doThing();
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         return keyCode != 257 ? false : this.doThing();
      }

      private boolean doThing() {
         boolean didAnything = false;
         if (!this.list.getTopButtonRow().shadersEnabled) {
            this.list.getTopButtonRow().setShadersEnabled(true);
            didAnything = true;
         }

         if (!this.isSelected()) {
            this.list.select(this.index);
            didAnything = true;
         }

         ShaderPackSelectionList.this.screen.setFocused(ShaderPackSelectionList.this.screen.getBottomRowOption());
         return didAnything;
      }

      @Nullable
      public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
         return !this.isFocused() ? GuiNavigationPath.of(this) : null;
      }

      public boolean isFocused() {
         return this.list.getFocused() == this;
      }
   }

   public static class TopButtonRowEntry extends ShaderPackSelectionList.BaseEntry {
      private static final Text NONE_PRESENT_LABEL = Text.translatable("options.iris.shaders.nonePresent").formatted(Formatting.GRAY);
      private static final Text SHADERS_DISABLED_LABEL = Text.translatable("options.iris.shaders.disabled");
      private static final Text SHADERS_ENABLED_LABEL = Text.translatable("options.iris.shaders.enabled");
      private final ShaderPackSelectionList list;
      public boolean allowEnableShadersButton = true;
      public boolean shadersEnabled;

      public TopButtonRowEntry(ShaderPackSelectionList list, boolean shadersEnabled) {
         this.list = list;
         this.shadersEnabled = shadersEnabled;
      }

      public void setShadersEnabled(boolean shadersEnabled) {
         this.shadersEnabled = shadersEnabled;
         this.list.screen.refreshScreenSwitchButton();
      }

      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         GuiUtil.bindIrisWidgetsTexture();
         GuiUtil.drawButton(context, x - 2, y - 2, entryWidth, entryHeight + 2, hovered, !this.allowEnableShadersButton);
         context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer, this.getEnableDisableLabel(), x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 16777215
         );
      }

      private Text getEnableDisableLabel() {
         return this.allowEnableShadersButton ? (this.shadersEnabled ? SHADERS_ENABLED_LABEL : SHADERS_DISABLED_LABEL) : NONE_PRESENT_LABEL;
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.allowEnableShadersButton) {
            this.setShadersEnabled(!this.shadersEnabled);
            GuiUtil.playButtonClickSound();
            return true;
         } else {
            return false;
         }
      }

      public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
         if (keyCode == 257 && this.allowEnableShadersButton) {
            this.setShadersEnabled(!this.shadersEnabled);
            GuiUtil.playButtonClickSound();
            return true;
         } else {
            return false;
         }
      }

      @Nullable
      public GuiNavigationPath getNavigationPath(GuiNavigation navigation) {
         return !this.isFocused() ? GuiNavigationPath.of(this) : null;
      }

      public boolean isFocused() {
         return this.list.getFocused() == this;
      }

      public static class EnableShadersButtonElement extends IrisElementRow.TextButtonElement {
         private int centerX;

         public EnableShadersButtonElement(Text text, Function<IrisElementRow.TextButtonElement, Boolean> onClick) {
            super(text, onClick);
         }

         @Override
         public void renderLabel(DrawContext guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
            int textX = this.centerX - (int)(this.font.getWidth(this.text) * 0.5);
            int textY = y + (int)((height - 8) * 0.5);
            guiGraphics.drawTextWithShadow(this.font, this.text, textX, textY, 16777215);
         }
      }
   }
}
