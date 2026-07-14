package io.github.reserveword.imblocker.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Config {
   public static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^([\\p{L}_][\\p{L}\\p{N}_]*:)?([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*$");
   public static final Predicate<Object> CHECK_CLASS_NAME = value -> value instanceof String name && CLASS_NAME_PATTERN.matcher(name).matches();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path PATH = Path.of("config", "imblocker.json");
   public static Config INSTANCE = load();

   public ArrayList<String> screenBlacklist = new ArrayList<>(List.of("com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap"));
   public ArrayList<String> screenWhitelist = new ArrayList<>(
      List.of(
         "net.minecraft.client.gui.screen.ingame.BookEditScreen",
         "net.minecraft.client.gui.screen.ingame.SignEditScreen",
         "net.minecraft.client.gui.screen.TitleScreen",
         "net.minecraft.client.gui.screen.ingame.HangingSignEditScreen",
         "journeymap.client.ui.waypoint.WaypointEditor",
         "com.ldtteam.blockout.BOScreen"
      )
   );
   public ArrayList<String> inputBlacklist = new ArrayList<>();
   public ArrayList<String> inputWhitelist = new ArrayList<>();
   public boolean enableScreenRecovering;
   public ArrayList<String> recoveredScreens = new ArrayList<>();

   public boolean inScreenBlacklist(Class<?> type) {
      return type != null && this.screenBlacklist.contains(type.getName());
   }

   public boolean inScreenWhitelist(Class<?> type) {
      return type != null && this.screenWhitelist.contains(type.getName());
   }

   public boolean inInputBlacklist(Class<?> type) {
      return type != null && this.inputBlacklist.contains(type.getName());
   }

   public boolean inInputWhitelist(Class<?> type) {
      return type != null && this.inputWhitelist.contains(type.getName());
   }

   public void recoverScreen(Class<?> type) {
      if (this.enableScreenRecovering && type != null && !this.recoveredScreens.contains(type.getName())) {
         this.recoveredScreens.add(type.getName());
         this.save();
      }
   }

   public void save() {
      try {
         Files.createDirectories(PATH.getParent());
         try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, writer);
         }
      } catch (IOException exception) {
         Common.LOGGER.error("Failed to save IMBlocker configuration", exception);
      }
   }

   private static Config load() {
      if (Files.isRegularFile(PATH)) {
         try (Reader reader = Files.newBufferedReader(PATH)) {
            Config config = GSON.fromJson(reader, Config.class);
            if (config != null) {
               config.normalize();
               return config;
            }
         } catch (IOException | RuntimeException exception) {
            Common.LOGGER.error("Failed to load IMBlocker configuration", exception);
         }
      }

      Config config = new Config();
      config.save();
      return config;
   }

   private void normalize() {
      this.screenBlacklist = normalize(this.screenBlacklist);
      this.screenWhitelist = normalize(this.screenWhitelist);
      this.inputBlacklist = normalize(this.inputBlacklist);
      this.inputWhitelist = normalize(this.inputWhitelist);
      this.recoveredScreens = normalize(this.recoveredScreens);
   }

   private static ArrayList<String> normalize(List<String> values) {
      ArrayList<String> result = new ArrayList<>();
      if (values != null) {
         for (String value : values) {
            if (value != null) {
               String className = value.contains(":") ? value.substring(value.lastIndexOf(':') + 1) : value;
               if (CLASS_NAME_PATTERN.matcher(className).matches() && !result.contains(className)) {
                  result.add(className);
               }
            }
         }
      }
      return result;
   }
}
