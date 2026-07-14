package net.caffeinemc.mods.sodium.client.gui;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraGameOptionPages;
import me.flashyreese.mods.sodiumextra.client.gui.SodiumExtraGameOptions;
import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gui.options.OptionFlag;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.binding.compat.VanillaBooleanOptionBinding;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.caffeinemc.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.SupportedGraphicsMode;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.InactivityFpsLimit;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public class SodiumGameOptionPages {
   private static final SodiumOptionsStorage sodiumOpts = new SodiumOptionsStorage();
   private static final MinecraftOptionsStorage vanillaOpts = new MinecraftOptionsStorage();
   private static final Window window = MinecraftClient.getInstance().getWindow();

   public static OptionPage general() {
      Monitor monitor = window.getMonitor();
      List<OptionGroup> groups = new ArrayList<>();
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.renderDistance"))
                  .setTooltip(Text.translatable("sodium.options.view_distance.tooltip"))
                  .setControl(option -> new SliderControl(option, 2, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                  .setBinding((options, value) -> options.getViewDistance().setValue(value), options -> options.getViewDistance().getValue())
                  .setImpact(OptionImpact.HIGH)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(createMaxShadowDistanceSlider())
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.simulationDistance"))
                  .setTooltip(Text.translatable("sodium.options.simulation_distance.tooltip"))
                  .setControl(option -> new SliderControl(option, 5, 32, 1, ControlValueFormatter.translateVariable("options.chunks")))
                  .setBinding((options, value) -> options.getSimulationDistance().setValue(value), options -> options.getSimulationDistance().getValue())
                  .setImpact(OptionImpact.HIGH)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.gamma"))
                  .setTooltip(Text.translatable("sodium.options.brightness.tooltip"))
                  .setControl(opt -> new SliderControl(opt, 0, 100, 1, ControlValueFormatter.brightness()))
                  .setBinding((opts, value) -> opts.getGamma().setValue(value.intValue() * 0.01), opts -> (int)(opts.getGamma().getValue() / 0.01))
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.guiScale"))
                  .setTooltip(Text.translatable("sodium.options.gui_scale.tooltip"))
                  .setControl(
                     option -> new SliderControl(
                        option,
                        0,
                        MinecraftClient.getInstance().getWindow().calculateScaleFactor(0, MinecraftClient.getInstance().forcesUnicodeFont()),
                        1,
                        ControlValueFormatter.guiScale()
                     )
                  )
                  .setBinding((opts, value) -> {
                     opts.getGuiScale().setValue(value);
                     MinecraftClient client = MinecraftClient.getInstance();
                     client.onResolutionChanged();
                  }, opts -> opts.getGuiScale().getValue())
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, vanillaOpts)
                  .setName(Text.translatable("options.fullscreen"))
                  .setTooltip(Text.translatable("sodium.options.fullscreen.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> {
                     opts.getFullscreen().setValue(value);
                     MinecraftClient client = MinecraftClient.getInstance();
                     Window window = client.getWindow();
                     if (window != null && window.isFullscreen() != opts.getFullscreen().getValue()) {
                        window.toggleFullscreen();
                        opts.getFullscreen().setValue(window.isFullscreen());
                     }
                  }, opts -> opts.getFullscreen().getValue())
                  .build()
            )
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.fullscreen.resolution"))
                  .setTooltip(Text.translatable("sodium.options.fullscreen_resolution.tooltip"))
                  .setControl(option -> new SliderControl(option, 0, null != monitor ? monitor.getVideoModeCount() : 0, 1, ControlValueFormatter.resolution()))
                  .setBinding((options, value) -> {
                     if (null != monitor) {
                        window.setFullscreenVideoMode(0 == value ? Optional.empty() : Optional.of(monitor.getVideoMode(value - 1)));
                     }
                  }, options -> {
                     if (null == monitor) {
                        return 0;
                     } else {
                        Optional<VideoMode> optional = window.getFullscreenVideoMode();
                        return optional.<Integer>map(videoMode -> monitor.findClosestVideoModeIndex(videoMode) + 1).orElse(0);
                     }
                  })
                  .setEnabled(() -> OsUtils.getOs() == OsUtils.OperatingSystem.WIN && MinecraftClient.getInstance().getWindow().getMonitor() != null)
                  .setFlags(OptionFlag.REQUIRES_VIDEOMODE_RELOAD)
                  .build()
            )
            .add(createVsyncOption())
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.framerateLimit"))
                  .setTooltip(Text.translatable("sodium.options.fps_limit.tooltip"))
                  .setControl(option -> new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit()))
                  .setBinding((opts, value) -> {
                     opts.getMaxFps().setValue(value);
                     MinecraftClient.getInstance().getInactivityFpsLimiter().setMaxFps(value);
                  }, opts -> opts.getMaxFps().getValue())
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(boolean.class, vanillaOpts)
                  .setName(Text.translatable("options.viewBobbing"))
                  .setTooltip(Text.translatable("sodium.options.view_bobbing.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding(new VanillaBooleanOptionBinding(MinecraftClient.getInstance().options.getBobView()))
                  .build()
            )
            .add(
               OptionImpl.createBuilder(AttackIndicator.class, vanillaOpts)
                  .setName(Text.translatable("options.attackIndicator"))
                  .setTooltip(Text.translatable("sodium.options.attack_indicator.tooltip"))
                  .setControl(
                     opts -> new CyclingControl<>(
                        opts,
                        AttackIndicator.class,
                        new Text[]{Text.translatable("options.off"), Text.translatable("options.attack.crosshair"), Text.translatable("options.attack.hotbar")}
                     )
                  )
                  .setBinding((opts, value) -> opts.getAttackIndicator().setValue(value), opts -> opts.getAttackIndicator().getValue())
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, vanillaOpts)
                  .setName(Text.translatable("options.autosaveIndicator"))
                  .setTooltip(Text.translatable("sodium.options.autosave_indicator.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> opts.getShowAutosaveIndicator().setValue(value), opts -> opts.getShowAutosaveIndicator().getValue())
                  .build()
            )
            .build()
      );
      return new OptionPage(Text.translatable("stat.generalButton"), ImmutableList.copyOf(groups));
   }

   public static OptionPage quality() {
      List<OptionGroup> groups = new ArrayList<>();
      groups.add(
         OptionGroup.createBuilder()
            .add(createGraphicsQualityOption())
            .add(createColorSpaceButton())
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(CloudRenderMode.class, vanillaOpts)
                  .setName(Text.translatable("options.renderClouds"))
                  .setTooltip(Text.translatable("sodium.options.clouds_quality.tooltip"))
                  .setControl(
                     option -> new CyclingControl<>(
                        option,
                        CloudRenderMode.class,
                        new Text[]{Text.translatable("options.off"), Text.translatable("options.graphics.fast"), Text.translatable("options.graphics.fancy")}
                     )
                  )
                  .setBinding((opts, value) -> {
                     opts.getCloudRenderMode().setValue(value);
                     if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                        Framebuffer framebuffer = MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer();
                        if (framebuffer != null) {
                           framebuffer.clear();
                        }
                     }
                  }, opts -> opts.getCloudRenderMode().getValue())
                  .setImpact(OptionImpact.LOW)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                  .setName(Text.translatable("soundCategory.weather"))
                  .setTooltip(Text.translatable("sodium.options.weather_quality.tooltip"))
                  .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                  .setBinding((opts, value) -> opts.quality.weatherQuality = value, opts -> opts.quality.weatherQuality)
                  .setImpact(OptionImpact.MEDIUM)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(SodiumGameOptions.GraphicsQuality.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.leaves_quality.name"))
                  .setTooltip(Text.translatable("sodium.options.leaves_quality.tooltip"))
                  .setControl(option -> new CyclingControl<>(option, SodiumGameOptions.GraphicsQuality.class))
                  .setBinding((opts, value) -> opts.quality.leavesQuality = value, opts -> opts.quality.leavesQuality)
                  .setImpact(OptionImpact.MEDIUM)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(ParticlesMode.class, vanillaOpts)
                  .setName(Text.translatable("options.particles"))
                  .setTooltip(Text.translatable("sodium.options.particle_quality.tooltip"))
                  .setControl(
                     option -> new CyclingControl<>(
                        option,
                        ParticlesMode.class,
                        new Text[]{
                           Text.translatable("options.particles.all"),
                           Text.translatable("options.particles.decreased"),
                           Text.translatable("options.particles.minimal")
                        }
                     )
                  )
                  .setBinding((opts, value) -> opts.getParticles().setValue(value), opts -> opts.getParticles().getValue())
                  .setImpact(OptionImpact.MEDIUM)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, vanillaOpts)
                  .setName(Text.translatable("options.ao"))
                  .setTooltip(Text.translatable("sodium.options.smooth_lighting.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> opts.getAo().setValue(value), opts -> opts.getAo().getValue())
                  .setImpact(OptionImpact.LOW)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.biomeBlendRadius"))
                  .setTooltip(Text.translatable("sodium.options.biome_blend.tooltip"))
                  .setControl(option -> new SliderControl(option, 1, 7, 1, ControlValueFormatter.biomeBlend()))
                  .setBinding((opts, value) -> opts.getBiomeBlendRadius().setValue(value), opts -> opts.getBiomeBlendRadius().getValue())
                  .setImpact(OptionImpact.LOW)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.entityDistanceScaling"))
                  .setTooltip(Text.translatable("sodium.options.entity_distance.tooltip"))
                  .setControl(option -> new SliderControl(option, 50, 500, 25, ControlValueFormatter.percentage()))
                  .setBinding(
                     (opts, value) -> opts.getEntityDistanceScaling().setValue(value.intValue() / 100.0),
                     opts -> Math.round(opts.getEntityDistanceScaling().getValue().floatValue() * 100.0F)
                  )
                  .setImpact(OptionImpact.HIGH)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, vanillaOpts)
                  .setName(Text.translatable("options.entityShadows"))
                  .setTooltip(Text.translatable("sodium.options.entity_shadows.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> opts.getEntityShadows().setValue(value), opts -> opts.getEntityShadows().getValue())
                  .setImpact(OptionImpact.MEDIUM)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.vignette.name"))
                  .setTooltip(Text.translatable("sodium.options.vignette.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> opts.quality.enableVignette = value, opts -> opts.quality.enableVignette)
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.mipmapLevels"))
                  .setTooltip(Text.translatable("sodium.options.mipmap_levels.tooltip"))
                  .setControl(option -> new SliderControl(option, 0, 4, 1, ControlValueFormatter.multiplier()))
                  .setBinding((opts, value) -> opts.getMipmapLevels().setValue(value), opts -> opts.getMipmapLevels().getValue())
                  .setImpact(OptionImpact.MEDIUM)
                  .setFlags(OptionFlag.REQUIRES_ASSET_RELOAD)
                  .build()
            )
            .build()
      );
      if (SodiumExtraClientMod.isMixinEnabled("sodium.accessibility.MixinSodiumGameOptionPages")) {
         groups.add(
            OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.screenEffectScale"))
                  .setTooltip(Text.translatable("options.screenEffectScale.tooltip"))
                  .setControl(option -> new SliderControl(option, 0, 100, 1, ControlValueFormatter.percentage()))
                  .setBinding(
                     (options, value) -> options.getDistortionEffectScale().setValue(value / 100.0),
                     options -> (int)Math.round(options.getDistortionEffectScale().getValue() * 100.0)
                  )
                  .setImpact(OptionImpact.LOW)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(int.class, vanillaOpts)
                  .setName(Text.translatable("options.fovEffectScale"))
                  .setTooltip(Text.translatable("options.fovEffectScale.tooltip"))
                  .setControl(option -> new SliderControl(option, 0, 100, 1, ControlValueFormatter.percentage()))
                  .setBinding(
                     (options, value) -> options.getFovEffectScale().setValue(Math.sqrt(value / 100.0F)),
                     options -> (int)Math.round(Math.pow(options.getFovEffectScale().getValue(), 2.0) * 100.0)
                  )
                  .setImpact(OptionImpact.LOW)
                  .build()
            )
               .build()
         );
      }
      return new OptionPage(Text.translatable("sodium.options.pages.quality"), ImmutableList.copyOf(groups));
   }

   private static Option<?> createVsyncOption() {
      if (!SodiumExtraClientMod.isMixinEnabled("sodium.vsync.MixinSodiumGameOptionsPages")) {
         return OptionImpl.createBuilder(boolean.class, vanillaOpts)
            .setName(Text.translatable("options.vsync"))
            .setTooltip(Text.translatable("sodium.options.v_sync.tooltip"))
            .setControl(TickBoxControl::new)
            .setBinding(new VanillaBooleanOptionBinding(MinecraftClient.getInstance().options.getEnableVsync()))
            .setImpact(OptionImpact.VARIES)
            .build();
      }

      return OptionImpl.<SodiumExtraGameOptions, SodiumExtraGameOptions.VerticalSyncOption>createBuilder(
            SodiumExtraGameOptions.VerticalSyncOption.class, SodiumExtraGameOptionPages.sodiumExtraOpts
         )
         .setName(Text.translatable("options.vsync"))
         .setTooltip(
            Text.literal(
               Text.translatable("sodium.options.v_sync.tooltip").getString()
                  + "\n- "
                  + Text.translatable("sodium-extra.option.use_adaptive_sync.name").getString()
                  + ": "
                  + Text.translatable("sodium-extra.option.use_adaptive_sync.tooltip").getString()
            )
         )
         .setControl(
            option -> new CyclingControl<>(
               option,
               SodiumExtraGameOptions.VerticalSyncOption.class,
               SodiumExtraGameOptions.VerticalSyncOption.getAvailableOptions()
            )
         )
         .setBinding((options, value) -> {
            switch (value) {
               case OFF -> {
                  options.extraSettings.useAdaptiveSync = false;
                  vanillaOpts.getData().getEnableVsync().setValue(false);
               }
               case ON -> {
                  options.extraSettings.useAdaptiveSync = false;
                  vanillaOpts.getData().getEnableVsync().setValue(true);
               }
               case ADAPTIVE -> {
                  options.extraSettings.useAdaptiveSync = true;
                  vanillaOpts.getData().getEnableVsync().setValue(true);
               }
            }

            vanillaOpts.save();
         }, options -> {
            boolean vsync = vanillaOpts.getData().getEnableVsync().getValue();
            if (options.extraSettings.useAdaptiveSync) {
               return SodiumExtraGameOptions.VerticalSyncOption.ADAPTIVE;
            }

            return vsync ? SodiumExtraGameOptions.VerticalSyncOption.ON : SodiumExtraGameOptions.VerticalSyncOption.OFF;
         })
         .setImpact(OptionImpact.VARIES)
         .build();
   }

   public static OptionPage performance() {
      List<OptionGroup> groups = new ArrayList<>();
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.chunk_update_threads.name"))
                  .setTooltip(Text.translatable("sodium.options.chunk_update_threads.tooltip"))
                  .setControl(
                     o -> new SliderControl(o, 0, Runtime.getRuntime().availableProcessors(), 1, ControlValueFormatter.quantityOrDisabled("threads", "Default"))
                  )
                  .setImpact(OptionImpact.HIGH)
                  .setBinding((opts, value) -> opts.performance.chunkBuilderThreads = value, opts -> opts.performance.chunkBuilderThreads)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.always_defer_chunk_updates.name"))
                  .setTooltip(Text.translatable("sodium.options.always_defer_chunk_updates.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.HIGH)
                  .setBinding((opts, value) -> opts.performance.alwaysDeferChunkUpdates = value, opts -> opts.performance.alwaysDeferChunkUpdates)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.use_block_face_culling.name"))
                  .setTooltip(Text.translatable("sodium.options.use_block_face_culling.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.MEDIUM)
                  .setBinding((opts, value) -> opts.performance.useBlockFaceCulling = value, opts -> opts.performance.useBlockFaceCulling)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.use_fog_occlusion.name"))
                  .setTooltip(Text.translatable("sodium.options.use_fog_occlusion.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setBinding((opts, value) -> opts.performance.useFogOcclusion = value, opts -> opts.performance.useFogOcclusion)
                  .setImpact(OptionImpact.MEDIUM)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.use_entity_culling.name"))
                  .setTooltip(Text.translatable("sodium.options.use_entity_culling.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.MEDIUM)
                  .setBinding((opts, value) -> opts.performance.useEntityCulling = value, opts -> opts.performance.useEntityCulling)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.animate_only_visible_textures.name"))
                  .setTooltip(Text.translatable("sodium.options.animate_only_visible_textures.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.HIGH)
                  .setBinding((opts, value) -> opts.performance.animateOnlyVisibleTextures = value, opts -> opts.performance.animateOnlyVisibleTextures)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_UPDATE)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.use_no_error_context.name"))
                  .setTooltip(Text.translatable("sodium.options.use_no_error_context.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.LOW)
                  .setBinding((opts, value) -> opts.performance.useNoErrorGLContext = value, opts -> opts.performance.useNoErrorGLContext)
                  .setEnabled(SodiumGameOptionPages::supportsNoErrorContext)
                  .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                  .build()
            )
            .add(
               OptionImpl.createBuilder(InactivityFpsLimit.class, vanillaOpts)
                  .setName(Text.translatable("options.inactivityFpsLimit"))
                  .setTooltip(
                     v -> Text.translatable(v.getId() == 0 ? "options.inactivityFpsLimit.minimized.tooltip" : "options.inactivityFpsLimit.afk.tooltip")
                  )
                  .setControl(
                     option -> new CyclingControl<>(
                        option,
                        InactivityFpsLimit.class,
                        new Text[]{Text.translatable("options.inactivityFpsLimit.minimized"), Text.translatable("options.inactivityFpsLimit.afk")}
                     )
                  )
                  .setBinding((opts, value) -> opts.getInactivityFpsLimit().setValue(value), opts -> opts.getInactivityFpsLimit().getValue())
                  .build()
            )
            .build()
      );
      return new OptionPage(Text.translatable("sodium.options.pages.performance"), ImmutableList.copyOf(groups));
   }

   private static boolean supportsNoErrorContext() {
      GLCapabilities capabilities = GL.getCapabilities();
      return (capabilities.OpenGL46 || capabilities.GL_KHR_no_error) && !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
   }

   private static OptionImpl<net.minecraft.client.option.GameOptions, Integer> createMaxShadowDistanceSlider() {
      return OptionImpl.createBuilder(int.class, vanillaOpts)
         .setName(Text.translatable("options.iris.shadowDistance"))
         .setTooltip(Text.translatable("options.iris.shadowDistance.sodium_tooltip"))
         .setControl(option -> new SliderControl(option, 0, 32, 1, value -> value == 0 ? Text.literal("Disabled") : Text.translatable("options.chunks", value)))
         .setBinding((options, value) -> {
            IrisVideoSettings.shadowDistance = value;
            saveIrisConfig();
         }, options -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance))
         .setImpact(OptionImpact.HIGH)
         .setEnabled(IrisVideoSettings::isShadowDistanceSliderEnabled)
         .build();
   }

   private static net.caffeinemc.mods.sodium.client.gui.options.Option<?> createGraphicsQualityOption() {
      if (Iris.getIrisConfig().areShadersEnabled()) {
         return OptionImpl.createBuilder(SupportedGraphicsMode.class, vanillaOpts)
            .setName(Text.translatable("options.graphics"))
            .setTooltip(Text.translatable("sodium.options.graphics_quality.tooltip"))
            .setControl(
               option -> new CyclingControl<>(
                  option,
                  SupportedGraphicsMode.class,
                  new Text[]{Text.translatable("options.graphics.fast"), Text.translatable("options.graphics.fancy")}
               )
            )
            .setBinding((opts, value) -> opts.getGraphicsMode().setValue(value.toVanilla()), opts -> SupportedGraphicsMode.fromVanilla(opts.getGraphicsMode()))
            .setImpact(OptionImpact.HIGH)
            .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
            .build();
      }

      return OptionImpl.createBuilder(GraphicsMode.class, vanillaOpts)
         .setName(Text.translatable("options.graphics"))
         .setTooltip(Text.translatable("sodium.options.graphics_quality.tooltip"))
         .setControl(
            option -> new CyclingControl<>(
               option,
               GraphicsMode.class,
               new Text[]{
                  Text.translatable("options.graphics.fast"),
                  Text.translatable("options.graphics.fancy"),
                  Text.translatable("options.graphics.fabulous")
               }
            )
         )
         .setBinding((opts, value) -> opts.getGraphicsMode().setValue(value), opts -> opts.getGraphicsMode().getValue())
         .setImpact(OptionImpact.HIGH)
         .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
         .build();
   }

   private static OptionImpl<net.minecraft.client.option.GameOptions, ColorSpace> createColorSpaceButton() {
      return OptionImpl.createBuilder(ColorSpace.class, vanillaOpts)
         .setName(Text.translatable("options.iris.colorSpace"))
         .setTooltip(Text.translatable("options.iris.colorSpace.sodium_tooltip"))
         .setControl(
            option -> new CyclingControl<>(
               option,
               ColorSpace.class,
               new Text[]{Text.literal("sRGB"), Text.literal("DCI_P3"), Text.literal("Display P3"), Text.literal("REC2020"), Text.literal("Adobe RGB")}
            )
         )
         .setBinding((options, value) -> {
            IrisVideoSettings.colorSpace = value;
            saveIrisConfig();
         }, options -> IrisVideoSettings.colorSpace)
         .setImpact(OptionImpact.LOW)
         .setEnabled(() -> true)
         .build();
   }

   private static void saveIrisConfig() {
      try {
         Iris.getIrisConfig().save();
      } catch (IOException e) {
         Iris.logger.error("Failed to save Iris config!", e);
      }
   }

   public static OptionPage advanced() {
      List<OptionGroup> groups = new ArrayList<>();
      boolean isPersistentMappingSupported = MappedStagingBuffer.isSupported(RenderDevice.INSTANCE);
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.allow_nvidia_threaded_optimizations.name"))
                  .setTooltip(Text.translatable("sodium.options.allow_nvidia_threaded_optimizations.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.HIGH)
                  .setEnabled(NvidiaWorkarounds::isNvidiaGraphicsCardPresent)
                  .setBinding(
                     (opts, value) -> opts.advanced.allowNvidiaThreadedOptimizations = value,
                     opts -> opts.advanced.allowNvidiaThreadedOptimizations
                  )
                  .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(boolean.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.use_persistent_mapping.name"))
                  .setTooltip(Text.translatable("sodium.options.use_persistent_mapping.tooltip"))
                  .setControl(TickBoxControl::new)
                  .setImpact(OptionImpact.MEDIUM)
                  .setEnabled(() -> isPersistentMappingSupported)
                  .setBinding((opts, value) -> opts.advanced.useAdvancedStagingBuffers = value, opts -> opts.advanced.useAdvancedStagingBuffers)
                  .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                  .build()
            )
            .build()
      );
      groups.add(
         OptionGroup.createBuilder()
            .add(
               OptionImpl.createBuilder(int.class, sodiumOpts)
                  .setName(Text.translatable("sodium.options.cpu_render_ahead_limit.name"))
                  .setTooltip(Text.translatable("sodium.options.cpu_render_ahead_limit.tooltip"))
                  .setControl(opt -> new SliderControl(opt, 0, 9, 1, ControlValueFormatter.translateVariable("sodium.options.cpu_render_ahead_limit.value")))
                  .setBinding((opts, value) -> opts.advanced.cpuRenderAheadLimit = value, opts -> opts.advanced.cpuRenderAheadLimit)
                  .build()
            )
            .build()
      );
      return new OptionPage(Text.translatable("sodium.options.pages.advanced"), ImmutableList.copyOf(groups));
   }
}
