package dev.isxander.zoomify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;

public final class ZoomifyConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static ZoomifyConfig instance;
   public double zoomDivisor = 4.0;
   public double minimumDivisor = 1.0;
   public double maximumDivisor = 50.0;
   public double scrollStep = 1.0;
   public double transitionSeconds = 0.18;
   public double zoomedSensitivity = 0.35;
   public boolean useScrollWheel = true;
   public boolean resetZoomOnRelease;
   public boolean hideHand;
   public boolean affectHandFov;
   public boolean relativeViewBobbing = true;
   public boolean allowThirdPerson = true;
   public boolean cinematicCamera;
   public boolean toggleMode;
   public boolean spyglassUsesZoom = true;
   public double secondaryZoomDivisor = 2.0;
   public boolean secondaryHideHud;
   public SpyglassMode spyglassMode = SpyglassMode.OVERRIDE;
   public OverlayVisibility overlayVisibility = OverlayVisibility.HOLDING;
   public SoundMode soundMode = SoundMode.ALWAYS;

   public enum SpyglassMode { COMBINE, OVERRIDE, ONLY_WHILE_HOLDING }
   public enum OverlayVisibility { NEVER, HOLDING, CARRYING, ALWAYS }
   public enum SoundMode { NEVER, ALWAYS, WITH_OVERLAY }

   private static Path path() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("zoomify.json");
   }

   public static synchronized ZoomifyConfig get() {
      if (instance == null) {
         Path path = path();
         try {
            instance = Files.isRegularFile(path) ? GSON.fromJson(Files.readString(path), ZoomifyConfig.class) : new ZoomifyConfig();
         } catch (Exception ignored) {
            instance = new ZoomifyConfig();
         }
         if (instance == null) {
            instance = new ZoomifyConfig();
         }
         instance.sanitize();
         save();
      }
      return instance;
   }

   private void sanitize() {
      this.minimumDivisor = Math.max(1.0, this.minimumDivisor);
      this.maximumDivisor = Math.max(this.minimumDivisor, this.maximumDivisor);
      this.zoomDivisor = Math.clamp(this.zoomDivisor, this.minimumDivisor, this.maximumDivisor);
      this.scrollStep = Math.max(0.05, this.scrollStep);
      this.transitionSeconds = Math.max(0.0, this.transitionSeconds);
      this.zoomedSensitivity = Math.clamp(this.zoomedSensitivity, 0.01, 1.0);
      this.secondaryZoomDivisor = Math.clamp(this.secondaryZoomDivisor, 1.0, 50.0);
      if (this.spyglassMode == null) this.spyglassMode = SpyglassMode.OVERRIDE;
      if (this.overlayVisibility == null) this.overlayVisibility = OverlayVisibility.HOLDING;
      if (this.soundMode == null) this.soundMode = SoundMode.ALWAYS;
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
