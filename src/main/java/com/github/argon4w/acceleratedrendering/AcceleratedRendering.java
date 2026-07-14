package com.github.argon4w.acceleratedrendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderProgramLoader;
import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.backends.DebugOutput;
import com.github.argon4w.acceleratedrendering.core.meshes.ClientMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.utils.TextureUtils;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.client.render.model.BakedQuad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GLCapabilities;

/** Standalone lifecycle and hardware gate for the hard-merged renderer. */
public final class AcceleratedRendering {
   public static final String MOD_ID = "acceleratedrendering";
   public static final String VERSION = "1.21.1-forward-port";
   private static final Logger LOGGER = LogManager.getLogger("AcceleratedRendering");
   private static final AtomicLong RESOURCE_GENERATION = new AtomicLong();
   private static volatile boolean initialized;
   private static volatile boolean available;
   private static volatile boolean disabledAfterFailure;
   private static volatile String unavailableReason = "not initialized";

   private AcceleratedRendering() {
   }

   public static synchronized void initialize() {
      if (initialized) {
         return;
      }

      if (!RenderSystem.isOnRenderThreadOrInit()) {
         unavailableReason = "initialization was not called from the render thread";
         return;
      }

      initialized = true;
      if (!Boolean.parseBoolean(System.getProperty("acceleratedrendering.enabled", "true"))) {
         unavailableReason = "disabled by acceleratedrendering.enabled";
         LOGGER.info("AcceleratedRendering is disabled by configuration");
         return;
      }

      try {
         GLCapabilities capabilities = GL.getCapabilities();
         available = capabilities.GL_ARB_shader_image_load_store
            && capabilities.GL_ARB_sync
            && capabilities.GL_ARB_direct_state_access
            && capabilities.GL_ARB_compute_shader
            && capabilities.GL_ARB_buffer_storage
            && capabilities.GL_ARB_shader_atomic_counters;
         if (!available) {
            unavailableReason = "required OpenGL extensions are unavailable";
         }

         String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
         if (renderer != null) {
            String normalized = renderer.toLowerCase(Locale.ROOT);
            if (normalized.contains("mobileglues") || normalized.contains("gl4es") || normalized.contains("ltw")) {
               available = false;
               unavailableReason = "unsupported OpenGL translation layer: " + renderer;
            }
         }

         if (available) {
            if (CoreFeature.isDebugContextEnabled()) {
               if (shouldEnableSynchronousDebugOutput()) {
                  DebugOutput.enable();
                  LOGGER.warn(
                     "Synchronous OpenGL debug output is enabled by acceleratedrendering.core.debugContext; this can substantially reduce frame rates"
                  );
               } else {
                  LOGGER.warn(
                     "Ignoring acceleratedrendering.core.debugContext=ENABLED because Sodium's NVIDIA Driver Threaded Optimizations setting is enabled"
                  );
               }
            }
            LOGGER.info("AcceleratedRendering {} enabled on {}", VERSION, renderer);
         } else {
            LOGGER.warn("AcceleratedRendering is unavailable; using the vanilla/Iris paths ({})", unavailableReason);
         }
      } catch (Throwable throwable) {
         disableAfterFailure("OpenGL capability detection failed", throwable);
      }
   }

   private static boolean shouldEnableSynchronousDebugOutput() {
      return !NvidiaWorkarounds.isNvidiaGraphicsCardPresent()
         || !SodiumClientMod.options().advanced.allowNvidiaThreadedOptimizations;
   }

   public static boolean isAvailable() {
      return initialized && available && !disabledAfterFailure;
   }

   public static void registerReloaders(ReloadableResourceManagerImpl resourceManager) {
      if (isAvailable()) {
         resourceManager.registerReloader(ComputeShaderProgramLoader.INSTANCE);
         resourceManager.registerReloader(TextureUtils.getInstance());
      }
   }

   public static boolean isIrisPipelineActive() {
      return isAvailable() && Iris.isPackInUseQuick();
   }

   public static boolean isShadowPass() {
      return isIrisPipelineActive() && ShadowRenderingState.areShadowsCurrentlyBeingRendered();
   }

   public static String getUnavailableReason() {
      return unavailableReason;
   }

   public static long getResourceGeneration() {
      return RESOURCE_GENERATION.get();
   }

   public static void invalidateCaches() {
      RESOURCE_GENERATION.incrementAndGet();
      AcceleratedTextCache.clear();
      BakedQuad.clearAcceleratedMeshes();
      Runnable releaseMeshes = () -> {
         ServerMesh.Builder.INSTANCE.delete();
         ClientMesh.Builder.INSTANCE.delete();
      };
      if (RenderSystem.isOnRenderThreadOrInit()) {
         releaseMeshes.run();
      } else {
         RenderSystem.recordRenderCall(releaseMeshes::run);
      }
   }

   public static synchronized void disableAfterFailure(String operation, Throwable throwable) {
      if (!disabledAfterFailure) {
         disabledAfterFailure = true;
         available = false;
         unavailableReason = operation;
         LOGGER.error("{}; AcceleratedRendering has fallen back for this session", operation, throwable);
      }
   }

   public static void close() {
      if (ComputeShaderProgramLoader.isProgramsLoaded()) {
         CoreBuffers.deleteBuffers();
         IrisCompatBuffers.deleteBuffers();
      }
      ComputeShaderProgramLoader.delete();
      invalidateCaches();
      available = false;
      initialized = false;
   }
}
