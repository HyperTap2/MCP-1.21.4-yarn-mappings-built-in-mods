package net.caffeinemc.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraGameOptionPages;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import me.pepperbell.continuity.client.config.ContinuityGameOptionPage;
import me.flashyreese.mods.sodiumextra.client.gui.scrollable_page.OptionPageScrollFrame;
import java.util.stream.Stream;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionFlag;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPromptable;
import net.caffeinemc.mods.sodium.client.gui.screen.ConfigCorruptedScreen;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class SodiumOptionsGUI extends Screen implements ScreenPromptable {
   private final List<OptionPage> pages = new ArrayList<>();
   private final List<ControlElement<?>> controls = new ArrayList<>();
   private final Screen prevScreen;
   private final OptionPage shaderPacks;
   private OptionPage currentPage;
   private FlatButtonWidget applyButton;
   private FlatButtonWidget closeButton;
   private FlatButtonWidget undoButton;
   private FlatButtonWidget donateButton;
   private FlatButtonWidget hideDonateButton;
   private boolean hasPendingChanges;
   private ControlElement<?> hoveredElement;
   @Nullable
   private ScreenPrompt prompt;
   private static final List<StringVisitable> DONATION_PROMPT_MESSAGE = List.of(
      StringVisitable.concat(Text.literal("Hello!")),
      StringVisitable.concat(
         Text.literal("It seems that you've been enjoying "),
         Text.literal("Sodium").withColor(2616210),
         Text.literal(", the powerful and open rendering optimization mod for Minecraft.")
      ),
      StringVisitable.concat(
         Text.literal("Mods like these are complex. They require "),
         Text.literal("thousands of hours").withColor(16739840),
         Text.literal(" of development, debugging, and tuning to create the experience that players have come to expect.")
      ),
      StringVisitable.concat(
         Text.literal("If you'd like to show your token of appreciation, and support the development of our mod in the process, then consider "),
         Text.literal("buying us a coffee").withColor(15550926),
         Text.literal(".")
      ),
      StringVisitable.concat(Text.literal("And thanks again for using our mod! We hope it helps you (and your computer.)"))
   );

   private SodiumOptionsGUI(Screen prevScreen) {
      this(prevScreen, false);
   }

   private SodiumOptionsGUI(Screen prevScreen, boolean openSodiumExtra) {
      super(Text.literal("Sodium Renderer Settings"));
      this.prevScreen = prevScreen;
      this.pages.add(SodiumGameOptionPages.general());
      this.pages.add(SodiumGameOptionPages.quality());
      this.pages.add(SodiumGameOptionPages.performance());
      this.pages.add(SodiumGameOptionPages.advanced());
      OptionPage firstSodiumExtraPage = null;
      if (SodiumExtraClientMod.isMixinEnabled("compat.MixinSodiumOptionsGUI")) {
         firstSodiumExtraPage = SodiumExtraGameOptionPages.animation();
         this.pages.add(firstSodiumExtraPage);
         this.pages.add(SodiumExtraGameOptionPages.particle());
         this.pages.add(SodiumExtraGameOptionPages.detail());
         this.pages.add(SodiumExtraGameOptionPages.render());
         this.pages.add(SodiumExtraGameOptionPages.extra());
      }
      this.pages.add(ContinuityGameOptionPage.create());
      this.shaderPacks = new OptionPage(Text.translatable("options.iris.shaderPackSelection"), ImmutableList.of());
      this.pages.add(this.shaderPacks);
      if (openSodiumExtra && firstSodiumExtraPage != null) {
         this.currentPage = firstSodiumExtraPage;
      }
      this.checkPromptTimers();
   }

   private void checkPromptTimers() {
      if (!PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
         SodiumGameOptions options = SodiumClientMod.options();
         if (!options.notifications.hasSeenDonationPrompt) {
            HashedFingerprint fingerprint = null;

            try {
               fingerprint = HashedFingerprint.loadFromDisk();
            } catch (Throwable var5) {
               SodiumClientMod.logger().error("Failed to read the fingerprint from disk", var5);
            }

            if (fingerprint != null) {
               Instant now = Instant.now();
               Instant threshold = Instant.ofEpochSecond(fingerprint.timestamp()).plus(3L, ChronoUnit.DAYS);
               if (now.isAfter(threshold)) {
                  this.openDonationPrompt(options);
               }
            }
         }
      }
   }

   private void openDonationPrompt(SodiumGameOptions options) {
      ScreenPrompt prompt = new ScreenPrompt(
         this, DONATION_PROMPT_MESSAGE, 320, 190, new ScreenPrompt.Action(Text.literal("Buy us a coffee"), this::openDonationPage)
      );
      prompt.setFocused(true);
      options.notifications.hasSeenDonationPrompt = true;

      try {
         SodiumGameOptions.writeToDisk(options);
      } catch (IOException var4) {
         SodiumClientMod.logger().error("Failed to update config file", var4);
      }
   }

   public static Screen createScreen(Screen currentScreen) {
      return (Screen)(SodiumClientMod.options().isReadOnly()
         ? new ConfigCorruptedScreen(currentScreen, SodiumOptionsGUI::new)
         : new SodiumOptionsGUI(currentScreen));
   }

   public static Screen createSodiumExtraScreen(Screen currentScreen) {
      return (Screen)(SodiumClientMod.options().isReadOnly()
         ? new ConfigCorruptedScreen(currentScreen, parent -> new SodiumOptionsGUI(parent, true))
         : new SodiumOptionsGUI(currentScreen, true));
   }

   public void setPage(OptionPage page) {
      if (page == this.shaderPacks) {
         this.client.setScreen(new ShaderPackScreen(this));
         return;
      }

      this.currentPage = page;
      this.rebuildGUI();
   }

   @Override
   protected void init() {
      super.init();
      this.rebuildGUI();
      if (this.prompt != null) {
         this.prompt.init();
      }
   }

   private void rebuildGUI() {
      this.controls.clear();
      this.clearChildren();
      if (this.currentPage == null) {
         if (this.pages.isEmpty()) {
            throw new IllegalStateException("No pages are available?!");
         }

         this.currentPage = this.pages.get(0);
      }

      this.rebuildGUIPages();
      this.rebuildGUIOptions();
      this.undoButton = new FlatButtonWidget(
         new Dim2i(this.width - 211, this.height - 30, 65, 20), Text.translatable("sodium.options.buttons.undo"), this::undoChanges
      );
      this.applyButton = new FlatButtonWidget(
         new Dim2i(this.width - 142, this.height - 30, 65, 20), Text.translatable("sodium.options.buttons.apply"), this::applyChanges
      );
      this.closeButton = new FlatButtonWidget(new Dim2i(this.width - 73, this.height - 30, 65, 20), Text.translatable("gui.done"), this::close);
      this.donateButton = new FlatButtonWidget(
         new Dim2i(this.width - 128, 6, 100, 20), Text.translatable("sodium.options.buttons.donate"), this::openDonationPage
      );
      this.hideDonateButton = new FlatButtonWidget(new Dim2i(this.width - 26, 6, 20, 20), Text.literal("x"), this::hideDonationButton);
      if (SodiumClientMod.options().notifications.hasClearedDonationButton) {
         this.setDonationButtonVisibility(false);
      }

      this.addDrawableChild(this.undoButton);
      this.addDrawableChild(this.applyButton);
      this.addDrawableChild(this.closeButton);
      this.addDrawableChild(this.donateButton);
      this.addDrawableChild(this.hideDonateButton);
   }

   private void setDonationButtonVisibility(boolean value) {
      this.donateButton.setVisible(value);
      this.hideDonateButton.setVisible(value);
   }

   private void hideDonationButton() {
      SodiumGameOptions options = SodiumClientMod.options();
      options.notifications.hasClearedDonationButton = true;

      try {
         SodiumGameOptions.writeToDisk(options);
      } catch (IOException var3) {
         throw new RuntimeException("Failed to save configuration", var3);
      }

      this.setDonationButtonVisibility(false);
   }

   private void rebuildGUIPages() {
      int x = 6;
      int y = 6;

      for (OptionPage page : this.pages) {
         int width = 12 + this.textRenderer.getWidth(page.getName());
         FlatButtonWidget button = new FlatButtonWidget(new Dim2i(x, y, width, 18), page.getName(), () -> this.setPage(page));
         button.setSelected(this.currentPage == page);
         x += width + 6;
         this.addDrawableChild(button);
      }
   }

   private void rebuildGUIOptions() {
      int x = 6;
      int y = 28;
      if (SodiumExtraClientMod.isMixinEnabled("sodium.scrollable_page.MixinSodiumOptionsGUI")) {
         this.addDrawableChild(new OptionPageScrollFrame(new Dim2i(x, y, 240, this.height - y - 10), this.currentPage));
         return;
      }

      for (OptionGroup group : this.currentPage.getGroups()) {
         for (Option<?> option : group.getOptions()) {
            Control<?> control = option.getControl();
            ControlElement<?> element = control.createElement(new Dim2i(x, y, 240, 18));
            this.addDrawableChild(element);
            this.controls.add(element);
            y += 18;
         }

         y += 4;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.updateControls();
      super.render(context, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);
      if (this.hoveredElement != null) {
         this.renderOptionTooltip(context, this.hoveredElement);
      }

      if (this.prompt != null) {
         this.prompt.render(context, mouseX, mouseY, delta);
      }
   }

   private void updateControls() {
      ControlElement<?> hovered = this.getActiveControls()
         .filter(AbstractWidget::isHovered)
         .findFirst()
         .orElse(this.getActiveControls().filter(AbstractWidget::isFocused).findFirst().orElse(null));
      boolean hasChanges = this.getAllOptions().anyMatch(Option::hasChanged);

      for (OptionPage page : this.pages) {
         UnmodifiableIterator var5 = page.getOptions().iterator();

         while (var5.hasNext()) {
            Option<?> option = (Option<?>)var5.next();
            if (option.hasChanged()) {
               hasChanges = true;
            }
         }
      }

      this.applyButton.setEnabled(hasChanges);
      this.undoButton.setVisible(hasChanges);
      this.closeButton.setEnabled(!hasChanges);
      this.hasPendingChanges = hasChanges;
      this.hoveredElement = hovered;
   }

   private Stream<Option<?>> getAllOptions() {
      return this.pages.stream().flatMap(s -> s.getOptions().stream());
   }

   private Stream<ControlElement<?>> getActiveControls() {
      return this.controls.stream();
   }

   private void renderOptionTooltip(DrawContext graphics, ControlElement<?> element) {
      if (SodiumExtraClientMod.isMixinEnabled("sodium.scrollable_page.MixinSodiumOptionsGUI")) {
         // The scroll frame renders tooltips after its scissor region is released.
         return;
      }

      Dim2i dim = element.getDimensions();
      int padding = 3;
      int spacing = 3;
      int y = dim.y();
      int x = dim.getLimitX() + spacing;
      int width = Math.min(200, this.width - x - spacing);
      Option<?> option = element.getOption();
      int textWidth = width - padding * 2;
      List<OrderedText> lines = new ArrayList<>(this.textRenderer.wrapLines(option.getTooltip(), textWidth));
      OptionImpact impact = option.getImpact();
      if (impact != null) {
         MutableText impactText = Text.translatable("sodium.options.performance_impact_string", impact.getLocalizedName());
         lines.addAll(this.textRenderer.wrapLines(impactText.formatted(Formatting.GRAY), textWidth));
      }

      int height = lines.size() * 12 + spacing;
      int bottom = y + height;
      int bottomLimit = this.height - 40;
      if (bottom > bottomLimit) {
         y -= bottom - bottomLimit;
      }

      graphics.fillGradient(x, y, x + width, y + height, -536870912, -536870912);
      for (int index = 0; index < lines.size(); index++) {
         graphics.drawText(this.textRenderer, lines.get(index), x + padding, y + padding + index * 12, -1, false);
      }
   }

   private void applyChanges() {
      HashSet<OptionStorage<?>> dirtyStorages = new HashSet<>();
      EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
      this.getAllOptions().forEach(option -> {
         if (option.hasChanged()) {
            option.applyChanges();
            flags.addAll(option.getFlags());
            dirtyStorages.add(option.getStorage());
         }
      });
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.world != null) {
         if (flags.contains(OptionFlag.REQUIRES_RENDERER_RELOAD)) {
            client.worldRenderer.reload();
         } else if (flags.contains(OptionFlag.REQUIRES_RENDERER_UPDATE)) {
            client.worldRenderer.scheduleTerrainUpdate();
         }
      }

      if (flags.contains(OptionFlag.REQUIRES_ASSET_RELOAD)) {
         client.setMipmapLevels(client.options.getMipmapLevels().getValue());
         client.reloadResourcesConcurrently();
      }

      if (flags.contains(OptionFlag.REQUIRES_VIDEOMODE_RELOAD)) {
         client.getWindow().applyFullscreenVideoMode();
      }

      if (flags.contains(OptionFlag.REQUIRES_GAME_RESTART)) {
         Console.instance().logMessage(MessageLevel.WARN, "sodium.console.game_restart", true, 10.0);
      }

      for (OptionStorage<?> storage : dirtyStorages) {
         storage.save();
      }
   }

   private void undoChanges() {
      this.getAllOptions().forEach(Option::reset);
   }

   private void openDonationPage() {
      Util.getOperatingSystem().open("https://caffeinemc.net/donate");
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.prompt != null && this.prompt.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else if (this.prompt == null && keyCode == 80 && (modifiers & 1) != 0) {
         MinecraftClient.getInstance().setScreen(new VideoOptionsScreen(this.prevScreen, MinecraftClient.getInstance(), MinecraftClient.getInstance().options));
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.prompt != null) {
         return this.prompt.mouseClicked(mouseX, mouseY, button);
      } else {
         boolean clicked = super.mouseClicked(mouseX, mouseY, button);
         if (!clicked) {
            this.setFocused(null);
            return true;
         } else {
            return clicked;
         }
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return !this.hasPendingChanges;
   }

   @Override
   public void close() {
      this.client.setScreen(this.prevScreen);
   }

   @Override
   public List<? extends Element> children() {
      return this.prompt == null ? super.children() : this.prompt.getWidgets();
   }

   @Override
   public void setPrompt(@Nullable ScreenPrompt prompt) {
      this.prompt = prompt;
   }

   @Nullable
   @Override
   public ScreenPrompt getPrompt() {
      return this.prompt;
   }

   @Override
   public Dim2i getDimensions() {
      return new Dim2i(0, 0, this.width, this.height);
   }
}
