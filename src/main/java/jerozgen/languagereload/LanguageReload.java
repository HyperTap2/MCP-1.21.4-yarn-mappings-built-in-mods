package jerozgen.languagereload;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import jerozgen.languagereload.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.client.resource.language.LanguageManager;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class LanguageReload {
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final String MOD_ID = "languagereload";
   public static final String NO_LANGUAGE = "*";

   private LanguageReload() {
   }

   public static List<String> getLanguages() {
      MinecraftClient client = MinecraftClient.getInstance();
      Config config = Config.getInstance();
      Set<String> languages = new LinkedHashSet<>();
      String current = client.getLanguageManager().getLanguage();
      if (!NO_LANGUAGE.equals(current)) {
         languages.add(current);
      }
      languages.addAll(config.fallbacks);
      return List.copyOf(languages);
   }

   public static void reloadLanguages() {
      MinecraftClient client = MinecraftClient.getInstance();
      client.getLanguageManager().reload(client.getResourceManager());
      client.updateWindowTitle();
      client.inGameHud.getChatHud().reset();
      if (client.getNetworkHandler() != null) {
         client.getNetworkHandler().refreshSearchManager();
      }
      if (client.currentScreen instanceof AdvancementsScreen screen) {
         screen.languageReload$recreateWidgets();
      } else if (client.currentScreen instanceof BookScreen screen) {
         screen.languageReload$invalidateCache();
      }
      if (client.world != null) {
         for (var chunk : client.world.getChunkManager().languageReload$getLoadedChunks()) {
            for (var blockEntity : chunk.getBlockEntities().values()) {
               if (blockEntity instanceof SignBlockEntity sign) {
                  sign.getFrontText().languageReload$invalidateCache();
                  sign.getBackText().languageReload$invalidateCache();
               }
            }
         }
         for (var entity : client.world.getEntities()) {
            if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
               textDisplay.languageReload$invalidateTextLines();
            }
         }
      }
   }

   public static void setLanguage(@Nullable String language) {
      LinkedList<String> fallbacks = new LinkedList<>();
      if (language != null && !NO_LANGUAGE.equals(language) && !Language.DEFAULT_LANGUAGE.equals(language)) {
         fallbacks.add(Language.DEFAULT_LANGUAGE);
      }
      setLanguage(language, fallbacks);
   }

   public static void setLanguage(@Nullable String language, @Nullable List<String> fallbacks) {
      String selected = language == null ? NO_LANGUAGE : language;
      LinkedList<String> selectedFallbacks = new LinkedList<>(fallbacks == null ? List.of() : fallbacks);
      selectedFallbacks.remove(selected);

      MinecraftClient client = MinecraftClient.getInstance();
      LanguageManager manager = client.getLanguageManager();
      Config config = Config.getInstance();
      if (selected.equals(manager.getLanguage()) && selectedFallbacks.equals(config.fallbacks)) {
         return;
      }

      config.previousLanguage = manager.getLanguage();
      config.previousFallbacks = new LinkedList<>(config.fallbacks);
      config.language = selected;
      config.fallbacks = selectedFallbacks;
      Config.save();
      manager.setLanguage(selected);
      client.options.language = selected;
      client.options.write();
      reloadLanguages();
   }
}
