package net.minecraft.client.resource.language;

import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.visuals.features.force_unicode_font.UnicodeFontFix1_12_2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.client.resource.metadata.LanguageResourceMetadata;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Language;
import jerozgen.languagereload.LanguageReload;
import jerozgen.languagereload.config.Config;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class LanguageManager implements SynchronousResourceReloader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LanguageDefinition ENGLISH_US = new LanguageDefinition("US", "English", false);
   private Map<String, LanguageDefinition> languageDefs = ImmutableMap.of("en_us", ENGLISH_US);
   private String currentLanguageCode;
   private final Consumer<TranslationStorage> reloadCallback;

   public LanguageManager(String languageCode, Consumer<TranslationStorage> reloadCallback) {
      this.currentLanguageCode = languageCode;
      this.reloadCallback = reloadCallback;
   }

   private static Map<String, LanguageDefinition> loadAvailableLanguages(Stream<ResourcePack> packs) {
      Map<String, LanguageDefinition> map = Maps.newHashMap();
      packs.forEach(pack -> {
         try {
            LanguageResourceMetadata languageResourceMetadata = (LanguageResourceMetadata)pack.parseMetadata(LanguageResourceMetadata.SERIALIZER);
            if (languageResourceMetadata != null) {
               languageResourceMetadata.definitions().forEach(map::putIfAbsent);
            }
         } catch (RuntimeException | IOException exception) {
            LOGGER.warn("Unable to parse language metadata section of resourcepack: {}", pack.getId(), exception);
         }
      });
      return ImmutableMap.copyOf(map);
   }

   public void reload(ResourceManager manager) {
      this.languageDefs = loadAvailableLanguages(manager.streamResourcePacks());
      List<String> list = new ArrayList<>();
      boolean bl = ENGLISH_US.rightToLeft();
      Config config = Config.getInstance();
      if (config.language.isEmpty() || !config.language.equals(this.currentLanguageCode)) {
         config.previousLanguage = config.language;
         config.previousFallbacks = new java.util.LinkedList<>(config.fallbacks);
         config.language = this.currentLanguageCode;
         config.fallbacks.clear();
         if (!"en_us".equals(this.currentLanguageCode) && !LanguageReload.NO_LANGUAGE.equals(this.currentLanguageCode)) {
            config.fallbacks.add("en_us");
         }
         Config.save();
      }
      if (!LanguageReload.NO_LANGUAGE.equals(this.currentLanguageCode)) {
         list.add("en_us");
      }
      for (String fallback : config.fallbacks.reversed()) {
         if (this.languageDefs.containsKey(fallback) && !list.contains(fallback)) {
            list.add(fallback);
         }
      }
      if (!this.currentLanguageCode.equals("en_us") && !LanguageReload.NO_LANGUAGE.equals(this.currentLanguageCode)) {
         LanguageDefinition languageDefinition = this.languageDefs.get(this.currentLanguageCode);
         if (languageDefinition != null) {
            list.add(this.currentLanguageCode);
            bl = languageDefinition.rightToLeft();
         }
      }

      TranslationStorage translationStorage = TranslationStorage.load(manager, list, bl);
      I18n.setLanguage(translationStorage);
      Language.setInstance(translationStorage);
      this.reloadCallback.accept(translationStorage);
      UnicodeFontFix1_12_2.updateUnicodeFontOverride(ViaFabricPlus.getImpl().getTargetVersion());
   }

   public void setLanguage(String languageCode) {
      this.currentLanguageCode = languageCode;
   }

   public String getLanguage() {
      return this.currentLanguageCode;
   }

   public SortedMap<String, LanguageDefinition> getAllLanguages() {
      return new TreeMap<>(this.languageDefs);
   }

   @Nullable
   public LanguageDefinition getLanguage(String code) {
      return this.languageDefs.get(code);
   }

   public static LanguageDefinition getEnglishUs() {
      return ENGLISH_US;
   }
}
