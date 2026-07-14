package me.flashyreese.mods.sodiumextra.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import it.unimi.dsi.fastutil.objects.Object2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import net.caffeinemc.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Identifier.Serializer;
import org.lwjgl.glfw.GLFW;

public class SodiumExtraGameOptions {
   private static final Gson gson = new GsonBuilder()
      .registerTypeAdapter(Identifier.class, new Serializer())
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .setPrettyPrinting()
      .excludeFieldsWithModifiers(new int[]{2})
      .create();
   public final SodiumExtraGameOptions.AnimationSettings animationSettings = new SodiumExtraGameOptions.AnimationSettings();
   public final SodiumExtraGameOptions.ParticleSettings particleSettings = new SodiumExtraGameOptions.ParticleSettings();
   public final SodiumExtraGameOptions.DetailSettings detailSettings = new SodiumExtraGameOptions.DetailSettings();
   public final SodiumExtraGameOptions.RenderSettings renderSettings = new SodiumExtraGameOptions.RenderSettings();
   public final SodiumExtraGameOptions.ExtraSettings extraSettings = new SodiumExtraGameOptions.ExtraSettings();
   private File file;

   public static SodiumExtraGameOptions load(File file) {
      SodiumExtraGameOptions config;
      if (file.exists()) {
         try (FileReader reader = new FileReader(file)) {
            config = (SodiumExtraGameOptions)gson.fromJson(reader, SodiumExtraGameOptions.class);
         } catch (Exception e) {
            SodiumExtraClientMod.logger().error("Could not parse config, falling back to defaults!", e);
            config = new SodiumExtraGameOptions();
         }
      } else {
         config = new SodiumExtraGameOptions();
      }

      config.file = file;
      config.writeChanges();
      return config;
   }

   public void writeChanges() {
      File dir = this.file.getParentFile();
      if (!dir.exists()) {
         if (!dir.mkdirs()) {
            throw new RuntimeException("Could not create parent directories");
         }
      } else if (!dir.isDirectory()) {
         throw new RuntimeException("The parent file is not a directory");
      }

      try (FileWriter writer = new FileWriter(this.file)) {
         gson.toJson(this, writer);
      } catch (IOException e) {
         throw new RuntimeException("Could not save configuration file", e);
      }
   }

   public static class AnimationSettings {
      public boolean animation = true;
      public boolean water = true;
      public boolean lava = true;
      public boolean fire = true;
      public boolean portal = true;
      public boolean blockAnimations = true;
      public boolean sculkSensor = true;
   }

   public static class DetailSettings {
      public boolean sky = true;
      public boolean sun = true;
      public boolean moon = true;
      public boolean stars = true;
      public boolean rainSnow = true;
      public boolean biomeColors = true;
      public boolean skyColors = true;
   }

   public static class ExtraSettings {
      public SodiumExtraGameOptions.OverlayCorner overlayCorner = SodiumExtraGameOptions.OverlayCorner.TOP_LEFT;
      public SodiumExtraGameOptions.TextContrast textContrast = SodiumExtraGameOptions.TextContrast.NONE;
      public boolean showFps = false;
      public boolean showFPSExtended = true;
      public boolean showCoords = false;
      public boolean reduceResolutionOnMac = false;
      public boolean useAdaptiveSync = false;
      public int cloudHeight = 192;
      public int cloudDistance = 100;
      public boolean toasts = true;
      public boolean advancementToast = true;
      public boolean recipeToast = true;
      public boolean systemToast = true;
      public boolean tutorialToast = true;
      public boolean instantSneak = false;
      public boolean preventShaders = false;
      public boolean steadyDebugHud = true;
      public int steadyDebugHudRefreshInterval = 1;
   }

   public enum OverlayCorner implements TextProvider {
      TOP_LEFT("sodium-extra.option.overlay_corner.top_left"),
      TOP_RIGHT("sodium-extra.option.overlay_corner.top_right"),
      BOTTOM_LEFT("sodium-extra.option.overlay_corner.bottom_left"),
      BOTTOM_RIGHT("sodium-extra.option.overlay_corner.bottom_right");

      private final Text text;

      OverlayCorner(String text) {
         this.text = Text.translatable(text);
      }

      public Text getLocalizedName() {
         return this.text;
      }
   }

   public static class ParticleSettings {
      public boolean particles = true;
      public boolean rainSplash = true;
      public boolean blockBreak = true;
      public boolean blockBreaking = true;
      @SerializedName("other")
      public Map<Identifier, Boolean> otherMap = new Object2BooleanArrayMap();
   }

   public static class RenderSettings {
      public int fogDistance = 0;
      public int fogStart = 100;
      public boolean multiDimensionFogControl = false;
      @SerializedName("dimensionFogDistance")
      public Map<Identifier, Integer> dimensionFogDistanceMap = new Object2IntArrayMap();
      public boolean lightUpdates = true;
      public boolean itemFrame = true;
      public boolean armorStand = true;
      public boolean painting = true;
      public boolean piston = true;
      public boolean beaconBeam = true;
      public boolean limitBeaconBeamHeight = false;
      public boolean enchantingTableBook = true;
      public boolean itemFrameNameTag = true;
      public boolean playerNameTag = true;
   }

   public enum TextContrast implements TextProvider {
      NONE("sodium-extra.option.text_contrast.none"),
      BACKGROUND("sodium-extra.option.text_contrast.background"),
      SHADOW("sodium-extra.option.text_contrast.shadow");

      private final Text text;

      TextContrast(String text) {
         this.text = Text.translatable(text);
      }

      public Text getLocalizedName() {
         return this.text;
      }
   }

   public enum VerticalSyncOption implements TextProvider {
      OFF("options.off"),
      ON("options.on"),
      ADAPTIVE(
         "sodium-extra.option.use_adaptive_sync.name",
         GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear") || GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear")
      );

      private final Text name;
      private final boolean supported;

      VerticalSyncOption(String name) {
         this(name, true);
      }

      VerticalSyncOption(String name, boolean supported) {
         this.name = Text.translatable(name);
         this.supported = supported;
      }

      public static SodiumExtraGameOptions.VerticalSyncOption[] getAvailableOptions() {
         return Arrays.stream(values()).filter(o -> o.supported).toArray(SodiumExtraGameOptions.VerticalSyncOption[]::new);
      }

      public Text getLocalizedName() {
         return this.name;
      }
   }
}
