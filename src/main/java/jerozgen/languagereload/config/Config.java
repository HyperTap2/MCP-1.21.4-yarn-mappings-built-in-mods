package jerozgen.languagereload.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import jerozgen.languagereload.LanguageReload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Language;

public final class Config {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static Config instance;
   public int version = 1;
   public boolean multilingualItemSearch = true;
   public boolean removableDefaultLanguage;
   public LinkedList<String> fallbacks = new LinkedList<>();
   public LinkedList<String> previousFallbacks = new LinkedList<>();
   public String language = "";
   public String previousLanguage = "";

   private static Path path() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve(LanguageReload.MOD_ID + ".json");
   }

   public static synchronized Config getInstance() {
      if (instance == null) {
         load();
      }
      return instance;
   }

   public static synchronized void load() {
      Path path = path();
      try {
         instance = Files.isRegularFile(path) ? GSON.fromJson(Files.readString(path), Config.class) : new Config();
      } catch (Exception exception) {
         LanguageReload.LOGGER.error("Could not load {}", path, exception);
         instance = new Config();
      }
      if (instance == null) {
         instance = new Config();
      }
      if (instance.fallbacks == null) {
         instance.fallbacks = new LinkedList<>();
      }
      if (instance.previousFallbacks == null) {
         instance.previousFallbacks = new LinkedList<>();
      }
      if (instance.language == null) {
         instance.language = "";
      }
      if (instance.previousLanguage == null) {
         instance.previousLanguage = "";
      }
      migrateDefaultFallback(instance.language, instance.fallbacks);
      save();
   }

   private static void migrateDefaultFallback(String language, LinkedList<String> fallbacks) {
      if (!language.isEmpty() && !Language.DEFAULT_LANGUAGE.equals(language) && !fallbacks.contains(Language.DEFAULT_LANGUAGE)) {
         fallbacks.add(Language.DEFAULT_LANGUAGE);
      }
   }

   public static synchronized void save() {
      if (instance == null) {
         return;
      }
      Path path = path();
      try {
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(instance));
      } catch (IOException exception) {
         LanguageReload.LOGGER.error("Could not save {}", path, exception);
      }
   }
}
