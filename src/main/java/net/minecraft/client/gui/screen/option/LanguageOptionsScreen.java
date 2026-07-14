package net.minecraft.client.gui.screen.option;

import io.github.reserveword.imblocker.client.IMBlockerConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.LanguageDefinition;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import java.util.LinkedList;
import jerozgen.languagereload.LanguageReload;
import jerozgen.languagereload.config.ConfigScreen;
import jerozgen.languagereload.config.Config;

public class LanguageOptionsScreen extends GameOptionsScreen {
   private static final Text LANGUAGE_WARNING_TEXT = Text.translatable("options.languageAccuracyWarning").withColor(-4539718);
   private static final int field_49497 = 53;
   private LanguageOptionsScreen.LanguageSelectionListWidget languageSelectionList;
   final LanguageManager languageManager;
   private final LinkedList<String> selectedLanguages = new LinkedList<>();
   private TextFieldWidget searchBox;

   public LanguageOptionsScreen(Screen parent, GameOptions options, LanguageManager languageManager) {
      super(parent, options, Text.translatable("options.language.title"));
      this.languageManager = languageManager;
      this.selectedLanguages.addAll(LanguageReload.getLanguages());
      this.layout.setFooterHeight(82);
      this.layout.setHeaderHeight(53);
   }

   @Override
   protected void initHeader() {
      DirectionalLayoutWidget header = this.layout.addHeader(DirectionalLayoutWidget.vertical().spacing(5));
      header.getMainPositioner().alignHorizontalCenter();
      header.add(new TextWidget(this.title, this.textRenderer));
      this.searchBox = new TextFieldWidget(this.textRenderer, 0, 0, 200, 20, Text.translatable("gui.recipebook.search_hint"));
      this.searchBox.setChangedListener(query -> {
         if (this.languageSelectionList != null) this.languageSelectionList.languageReload$rebuild();
      });
      header.add(this.searchBox);
   }

   @Override
   protected void initBody() {
      this.languageSelectionList = this.layout.addBody(new LanguageOptionsScreen.LanguageSelectionListWidget(this.client));
   }

   @Override
   protected void addOptions() {
   }

   @Override
   protected void initFooter() {
      DirectionalLayoutWidget directionalLayoutWidget = this.layout.addFooter(DirectionalLayoutWidget.vertical()).spacing(8);
      directionalLayoutWidget.getMainPositioner().alignHorizontalCenter();
      directionalLayoutWidget.add(new TextWidget(LANGUAGE_WARNING_TEXT, this.textRenderer));
      DirectionalLayoutWidget directionalLayoutWidget2 = directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
      directionalLayoutWidget2.add(
         ButtonWidget.builder(Text.translatable("options.font"), button -> this.client.setScreen(new FontOptionsScreen(this, this.gameOptions))).build()
      );
      directionalLayoutWidget2.add(
         ButtonWidget.builder(Text.translatable("imblocker.settings"), button -> this.client.setScreen(new IMBlockerConfigScreen(this))).build()
      );
      DirectionalLayoutWidget directionalLayoutWidget3 = directionalLayoutWidget.add(DirectionalLayoutWidget.horizontal().spacing(8));
      directionalLayoutWidget3.add(
         ButtonWidget.builder(Text.translatable("options.languagereload.title"), button -> this.client.setScreen(new ConfigScreen(this))).build()
      );
      directionalLayoutWidget3.add(ButtonWidget.builder(ScreenTexts.DONE, button -> this.onDone()).build());
   }

   @Override
   protected void refreshWidgetPositions() {
      super.refreshWidgetPositions();
      this.languageSelectionList.position(this.width, this.layout);
   }

   void onDone() {
      LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry languageEntry = this.languageSelectionList.getSelectedOrNull();
      if (languageEntry != null) {
         LinkedList<String> fallbacks = new LinkedList<>(this.selectedLanguages);
         fallbacks.remove(languageEntry.languageCode);
         LanguageReload.setLanguage(languageEntry.languageCode, fallbacks);
      }

      this.client.setScreen(this.parent);
   }

   class LanguageSelectionListWidget extends AlwaysSelectedEntryListWidget<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {
      public LanguageSelectionListWidget(final MinecraftClient client) {
         super(client, LanguageOptionsScreen.this.width, LanguageOptionsScreen.this.height - 53 - 82, 53, 18);
         this.languageReload$rebuild();
      }

      void languageReload$rebuild() {
         String string = LanguageOptionsScreen.this.languageManager.getLanguage();
         String query = LanguageOptionsScreen.this.searchBox == null ? "" : LanguageOptionsScreen.this.searchBox.getText().toLowerCase(java.util.Locale.ROOT);
         LanguageEntry selected = this.getSelectedOrNull();
         this.clearEntries();
         LanguageOptionsScreen.this.languageManager
            .getAllLanguages()
            .forEach(
               (languageCode, languageDefinition) -> {
                  if (!query.isEmpty()
                     && !languageCode.toLowerCase(java.util.Locale.ROOT).contains(query)
                     && !languageDefinition.getDisplayText().getString().toLowerCase(java.util.Locale.ROOT).contains(query)) return;
                  LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry languageEntry = new LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry(
                     languageCode, languageDefinition
                  );
                  this.addEntry(languageEntry);
                  if (selected != null && selected.languageCode.equals(languageCode) || selected == null && string.equals(languageCode)) {
                     this.setSelected(languageEntry);
                  }
               }
            );
         if (this.getSelectedOrNull() != null) {
            this.centerScrollOn(this.getSelectedOrNull());
         }
      }

      @Override
      public int getRowWidth() {
         return super.getRowWidth() + 50;
      }

      public class LanguageEntry extends AlwaysSelectedEntryListWidget.Entry<LanguageOptionsScreen.LanguageSelectionListWidget.LanguageEntry> {
         final String languageCode;
         private final Text languageDefinition;
         private long clickTime;

         public LanguageEntry(final String languageCode, final LanguageDefinition languageDefinition) {
            this.languageCode = languageCode;
            this.languageDefinition = languageDefinition.getDisplayText();
         }

         @Override
         public void render(
            DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta
         ) {
            int priority = LanguageOptionsScreen.this.selectedLanguages.indexOf(this.languageCode);
            Text label = priority < 0 ? this.languageDefinition : Text.literal("[" + (priority + 1) + "] ").append(this.languageDefinition);
            context.drawCenteredTextWithShadow(LanguageOptionsScreen.this.textRenderer, label, LanguageSelectionListWidget.this.width / 2, y + entryHeight / 2 - 9 / 2, -1);
         }

         @Override
         public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (Screen.hasShiftDown() && LanguageOptionsScreen.this.selectedLanguages.contains(this.languageCode)) {
               int index = LanguageOptionsScreen.this.selectedLanguages.indexOf(this.languageCode);
               int target = keyCode == 265 ? index - 1 : keyCode == 264 ? index + 1 : index;
               if (target >= 0 && target < LanguageOptionsScreen.this.selectedLanguages.size() && target != index) {
                  LanguageOptionsScreen.this.selectedLanguages.add(target, LanguageOptionsScreen.this.selectedLanguages.remove(index));
                  return true;
               }
            }
            if (KeyCodes.isToggle(keyCode)) {
               this.onPressed();
               LanguageOptionsScreen.this.onDone();
               return true;
            } else {
               return super.keyPressed(keyCode, scanCode, modifiers);
            }
         }

         @Override
         public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (Screen.hasShiftDown() || button == 1) {
               if (LanguageOptionsScreen.this.selectedLanguages.contains(this.languageCode)) {
                  if ((!"en_us".equals(this.languageCode) || Config.getInstance().removableDefaultLanguage)
                     && !this.languageCode.equals(LanguageOptionsScreen.this.languageManager.getLanguage())) {
                     LanguageOptionsScreen.this.selectedLanguages.remove(this.languageCode);
                  }
               } else {
                  LanguageOptionsScreen.this.selectedLanguages.add(this.languageCode);
               }
               return true;
            }
            this.onPressed();
            if (Util.getMeasuringTimeMs() - this.clickTime < 250L) {
               LanguageOptionsScreen.this.onDone();
            }

            this.clickTime = Util.getMeasuringTimeMs();
            return super.mouseClicked(mouseX, mouseY, button);
         }

         private void onPressed() {
            LanguageSelectionListWidget.this.setSelected(this);
         }

         @Override
         public Text getNarration() {
            return Text.translatable("narrator.select", new Object[]{this.languageDefinition});
         }
      }
   }
}
