package mcp.client.gui.screen;

import com.viaversion.viafabricplus.screen.impl.settings.SettingsScreen;
import dev.isxander.zoomify.ZoomifyConfigScreen;
import dev.tr7zw.waveycapes.WaveyCapesConfigScreen;
import io.github.reserveword.imblocker.client.IMBlockerConfigScreen;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import jerozgen.languagereload.config.ConfigScreen;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.config.ContinuityConfigScreen;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

/** Central entry point for configuration screens provided by source-integrated mods. */
public final class BuiltinModsScreen extends Screen {
   private static final Text TITLE = Text.translatable("client.builtin_mods.title");
   private static final Text SEARCH = Text.translatable("client.builtin_mods.search");
   private static final Text CONFIGURE = Text.translatable("client.builtin_mods.configure");
   private static final Text CONFIGURABLE = Text.translatable("client.builtin_mods.configurable").formatted(Formatting.GREEN);
   private static final Text NOT_CONFIGURABLE = Text.translatable("client.builtin_mods.not_configurable").formatted(Formatting.GRAY);
   private static final List<BuiltinMod> MODS = List.of(
      configurable("sodium", "Sodium", SodiumOptionsGUI::createScreen),
      configurable("sodium_extra", "Sodium Extra", SodiumOptionsGUI::createSodiumExtraScreen),
      configurable("iris", "Iris", ShaderPackScreen::new),
      configurable("viafabricplus", "ViaFabricPlus", BuiltinModsScreen::createViaFabricPlusScreen),
      configurable("continuity", "Continuity", parent -> new ContinuityConfigScreen(parent, ContinuityConfig.INSTANCE)),
      configurable("zoomify", "Zoomify", ZoomifyConfigScreen::new),
      unavailable("accelerated_rendering", "AcceleratedRendering"),
      unavailable("entity_culling", "Entity Culling"),
      unavailable("ferrite_core", "FerriteCore"),
      configurable("wavey_capes", "Wavey Capes", WaveyCapesConfigScreen::new),
      unavailable("lithium", "Lithium"),
      unavailable("custom_skin_loader", "CustomSkinLoader"),
      configurable("imblocker", "IMBlocker", IMBlockerConfigScreen::new),
      configurable("language_reload", "Language Reload", ConfigScreen::new)
   );

   private final Screen parent;
   private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, 58, 36);
   private TextFieldWidget searchBox;
   private ModListWidget modList;
   private ButtonWidget configureButton;

   public BuiltinModsScreen(Screen parent) {
      super(TITLE);
      this.parent = parent;
   }

   @Override
   protected void init() {
      DirectionalLayoutWidget header = this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(5));
      header.getMainPositioner().alignHorizontalCenter();
      header.add(new TextWidget(this.title, this.textRenderer));
      this.searchBox = new TextFieldWidget(this.textRenderer, 0, 0, 240, 20, SEARCH);
      this.searchBox.setPlaceholder(SEARCH);
      this.searchBox.setChangedListener(query -> this.modList.rebuild(query));
      header.add(this.searchBox);

      this.modList = this.layout.addBody(new ModListWidget(this.client));
      this.modList.rebuild("");

      DirectionalLayoutWidget footer = this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
      this.configureButton = footer.add(ButtonWidget.builder(CONFIGURE, button -> this.openSelected()).width(150).build());
      footer.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close()).width(150).build());

      this.layout.forEachChild(this::addDrawableChild);
      this.refreshWidgetPositions();
      this.updateConfigureButton();
   }

   @Override
   protected void refreshWidgetPositions() {
      this.layout.refreshPositions();
      if (this.modList != null) {
         this.modList.position(this.width, this.layout);
      }
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 257 || keyCode == 335)
         && this.getFocused() == this.modList
         && this.configureButton != null
         && this.configureButton.active) {
         this.openSelected();
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public void close() {
      this.client.setScreen(this.parent);
   }

   private void openSelected() {
      ModEntry selected = this.modList.getSelectedOrNull();
      if (selected != null && selected.mod.screenFactory() != null) {
         this.client.setScreen(selected.mod.screenFactory().apply(this));
      }
   }

   private void updateConfigureButton() {
      if (this.configureButton != null) {
         ModEntry selected = this.modList.getSelectedOrNull();
         this.configureButton.active = selected != null && selected.mod.screenFactory() != null;
      }
   }

   private static BuiltinMod configurable(String id, String name, Function<Screen, Screen> screenFactory) {
      return new BuiltinMod(id, Text.literal(name), description(id), screenFactory);
   }

   private static BuiltinMod unavailable(String id, String name) {
      return new BuiltinMod(id, Text.literal(name), description(id), null);
   }

   private static Text description(String id) {
      return Text.translatable("client.builtin_mods.description." + id);
   }

   private static Screen createViaFabricPlusScreen(Screen parent) {
      SettingsScreen.INSTANCE.prevScreen = parent;
      return SettingsScreen.INSTANCE;
   }

   private record BuiltinMod(String id, Text name, Text description, @Nullable Function<Screen, Screen> screenFactory) {
      boolean matches(String query) {
         String normalized = query.toLowerCase(Locale.ROOT);
         return this.name.getString().toLowerCase(Locale.ROOT).contains(normalized)
            || this.description.getString().toLowerCase(Locale.ROOT).contains(normalized);
      }
   }

   private final class ModListWidget extends AlwaysSelectedEntryListWidget<ModEntry> {
      @Nullable
      private ModEntry lastClickedEntry;
      private long lastClickTime;

      ModListWidget(MinecraftClient client) {
         super(client, BuiltinModsScreen.this.width, BuiltinModsScreen.this.layout.getContentHeight(), BuiltinModsScreen.this.layout.getHeaderHeight(), 40);
      }

      void rebuild(String query) {
         BuiltinMod selectedMod = this.getSelectedOrNull() == null ? null : this.getSelectedOrNull().mod;
         this.clearEntries();
         for (BuiltinMod mod : MODS) {
            if (mod.matches(query)) {
               ModEntry entry = new ModEntry(mod);
               this.addEntry(entry);
               if (mod == selectedMod) {
                  this.setSelected(entry);
               }
            }
         }
         if (this.getSelectedOrNull() == null && !this.children().isEmpty()) {
            this.setSelected(this.children().get(0));
         }
         BuiltinModsScreen.this.updateConfigureButton();
      }

      @Override
      public void setSelected(@Nullable ModEntry entry) {
         super.setSelected(entry);
         BuiltinModsScreen.this.updateConfigureButton();
      }

      boolean registerClick(ModEntry entry) {
         long now = Util.getMeasuringTimeMs();
         boolean doubleClick = entry == this.lastClickedEntry && now - this.lastClickTime < 250L;
         this.lastClickedEntry = entry;
         this.lastClickTime = now;
         return doubleClick;
      }

      @Override
      public int getRowWidth() {
         return Math.min(440, this.width - 36);
      }
   }

   private final class ModEntry extends AlwaysSelectedEntryListWidget.Entry<ModEntry> {
      private final BuiltinMod mod;

      ModEntry(BuiltinMod mod) {
         this.mod = mod;
      }

      @Override
      public void render(
         DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
      ) {
         Text status = this.mod.screenFactory() == null ? NOT_CONFIGURABLE : CONFIGURABLE;
         int statusWidth = BuiltinModsScreen.this.textRenderer.getWidth(status);
         context.drawTextWithShadow(BuiltinModsScreen.this.textRenderer, this.mod.name(), x + 6, y + 5, 0xFFFFFF);
         context.drawTextWithShadow(BuiltinModsScreen.this.textRenderer, status, x + entryWidth - statusWidth - 8, y + 5, 0xFFFFFF);
         String description = BuiltinModsScreen.this.textRenderer.trimToWidth(this.mod.description().getString(), entryWidth - 16);
         context.drawTextWithShadow(BuiltinModsScreen.this.textRenderer, description, x + 6, y + 22, 0xA0A0A0);
      }

      @Override
      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         }
         BuiltinModsScreen.this.modList.setSelected(this);
         BuiltinModsScreen.this.updateConfigureButton();
         if (this.mod.screenFactory() != null && BuiltinModsScreen.this.modList.registerClick(this)) {
            BuiltinModsScreen.this.openSelected();
         }
         return true;
      }

      @Override
      public Text getNarration() {
         return Text.translatable(
            "client.builtin_mods.narration",
            this.mod.name(),
            this.mod.description(),
            this.mod.screenFactory() == null ? NOT_CONFIGURABLE : CONFIGURABLE
         );
      }
   }
}
