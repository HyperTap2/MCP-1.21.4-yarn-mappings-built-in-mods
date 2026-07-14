package dev.tr7zw.waveycapes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

public final class WaveyCapesConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static WaveyCapesConfig instance;
   public boolean enabled = true;
   public CapeStyle capeStyle = CapeStyle.SMOOTH;
   public WindMode windMode = WindMode.WAVES;
   public float gravity = 22.0F;
   public float damping = 7.5F;
   public float stiffness = 48.0F;
   public float windStrength = 3.0F;

   public enum CapeStyle { SMOOTH, BLOCKY }
   public enum WindMode { NONE, WAVES }

   private static Path path() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("waveycapes.json");
   }

   public static synchronized WaveyCapesConfig get() {
      if (instance == null) {
         try {
            Path path = path();
            instance = Files.isRegularFile(path) ? GSON.fromJson(Files.readString(path), WaveyCapesConfig.class) : new WaveyCapesConfig();
         } catch (Exception ignored) {
            instance = new WaveyCapesConfig();
         }
         if (instance == null) instance = new WaveyCapesConfig();
         instance.gravity = Math.max(0.0F, instance.gravity);
         instance.damping = Math.max(0.0F, instance.damping);
         instance.stiffness = Math.max(1.0F, instance.stiffness);
         save();
      }
      return instance;
   }

   public static synchronized void save() {
      if (instance == null) return;
      try {
         Path path = path();
         Files.createDirectories(path.getParent());
         Files.writeString(path, GSON.toJson(instance));
      } catch (Exception ignored) {
      }
   }
}
