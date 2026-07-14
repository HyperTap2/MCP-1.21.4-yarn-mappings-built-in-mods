package net.minecraft.client.option;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.platform.StandaloneIrisPlatformHelpers;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OperatingSystem;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class GameOptions {
   static final Logger LOGGER = LogUtils.getLogger();
   static final Gson GSON = new Gson();
   private static final TypeToken<List<String>> STRING_LIST_TYPE = new TypeToken<List<String>>() {};
   private static boolean iris$initialized;
   public static final int field_32149 = 2;
   public static final int field_32150 = 4;
   public static final int field_32152 = 8;
   public static final int field_32153 = 12;
   public static final int field_32154 = 16;
   public static final int field_32155 = 32;
   private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
   public static final String EMPTY_STRING = "";
   private static final Text DARK_MOJANG_STUDIOS_BACKGROUND_COLOR_TOOLTIP = Text.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
   private final SimpleOption<Boolean> monochromeLogo = SimpleOption.ofBoolean(
      "options.darkMojangStudiosBackgroundColor", SimpleOption.constantTooltip(DARK_MOJANG_STUDIOS_BACKGROUND_COLOR_TOOLTIP), false
   );
   private static final Text HIDE_LIGHTNING_FLASHES_TOOLTIP = Text.translatable("options.hideLightningFlashes.tooltip");
   private final SimpleOption<Boolean> hideLightningFlashes = SimpleOption.ofBoolean(
      "options.hideLightningFlashes", SimpleOption.constantTooltip(HIDE_LIGHTNING_FLASHES_TOOLTIP), false
   );
   private static final Text HIDE_SPLASH_TEXTS_TOOLTIP = Text.translatable("options.hideSplashTexts.tooltip");
   private final SimpleOption<Boolean> hideSplashTexts = SimpleOption.ofBoolean(
      "options.hideSplashTexts", SimpleOption.constantTooltip(HIDE_SPLASH_TEXTS_TOOLTIP), false
   );
   private final SimpleOption<Double> mouseSensitivity = new SimpleOption<>("options.sensitivity", SimpleOption.emptyTooltip(), (optionText, value) -> {
      if (value == 0.0) {
         return getGenericValueText(optionText, Text.translatable("options.sensitivity.min"));
      } else {
         return value == 1.0 ? getGenericValueText(optionText, Text.translatable("options.sensitivity.max")) : getPercentValueText(optionText, 2.0 * value);
      }
   }, SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.5, value -> {});
   private final SimpleOption<Integer> viewDistance;
   private final SimpleOption<Integer> simulationDistance;
   private int serverViewDistance = 0;
   private final SimpleOption<Double> entityDistanceScaling = new SimpleOption<>(
      "options.entityDistanceScaling",
      SimpleOption.emptyTooltip(),
      GameOptions::getPercentValueText,
      new SimpleOption.ValidatingIntSliderCallbacks(2, 20).withModifier(sliderProgressValue -> sliderProgressValue / 4.0, value -> (int)(value * 4.0)),
      Codec.doubleRange(0.5, 5.0),
      1.0,
      value -> {}
   );
   public static final int MAX_FPS_LIMIT = 260;
   private final SimpleOption<Integer> maxFps = new SimpleOption<>(
      "options.framerateLimit",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> value == 260
         ? getGenericValueText(optionText, Text.translatable("options.framerateLimit.max"))
         : getGenericValueText(optionText, Text.translatable("options.framerate", new Object[]{value})),
      new SimpleOption.ValidatingIntSliderCallbacks(1, 26).withModifier(value -> value * 10, value -> value / 10),
      Codec.intRange(10, 260),
      120,
      value -> MinecraftClient.getInstance().getInactivityFpsLimiter().setMaxFps(value)
   );
   private static final Text INACTIVITY_FPS_LIMIT_MINIMIZED_TOOLTIP = Text.translatable("options.inactivityFpsLimit.minimized.tooltip");
   private static final Text INACTIVITY_FPS_LIMIT_AFK_TOOLTIP = Text.translatable("options.inactivityFpsLimit.afk.tooltip");
   private final SimpleOption<InactivityFpsLimit> inactivityFpsLimit = new SimpleOption<>(
      "options.inactivityFpsLimit",
      option -> {
         return switch (option) {
            case MINIMIZED -> Tooltip.of(INACTIVITY_FPS_LIMIT_MINIMIZED_TOOLTIP);
            case AFK -> Tooltip.of(INACTIVITY_FPS_LIMIT_AFK_TOOLTIP);
         };
      },
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(InactivityFpsLimit.values()), InactivityFpsLimit.Codec),
      InactivityFpsLimit.AFK,
      inactivityFpsLimit -> {}
   );
   private final SimpleOption<CloudRenderMode> cloudRenderMode = new SimpleOption<>(
      "options.renderClouds",
      SimpleOption.emptyTooltip(),
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(
         Arrays.asList(CloudRenderMode.values()),
         Codec.withAlternative(CloudRenderMode.CODEC, Codec.BOOL, value -> value ? CloudRenderMode.FANCY : CloudRenderMode.OFF)
      ),
      CloudRenderMode.FANCY,
      cloudRenderMode -> {}
   );
   private static final Text FAST_GRAPHICS_TOOLTIP = Text.translatable("options.graphics.fast.tooltip");
   private static final Text FABULOUS_GRAPHICS_TOOLTIP = Text.translatable(
      "options.graphics.fabulous.tooltip", new Object[]{Text.translatable("options.graphics.fabulous").formatted(Formatting.ITALIC)}
   );
   private static final Text FANCY_GRAPHICS_TOOLTIP = Text.translatable("options.graphics.fancy.tooltip");
   private final SimpleOption<GraphicsMode> graphicsMode = new SimpleOption<>(
      "options.graphics",
      value -> {
         return switch (value) {
            case FANCY -> Tooltip.of(FANCY_GRAPHICS_TOOLTIP);
            case FAST -> Tooltip.of(FAST_GRAPHICS_TOOLTIP);
            case FABULOUS -> Tooltip.of(FABULOUS_GRAPHICS_TOOLTIP);
         };
      },
      (optionText, value) -> {
         MutableText mutableText = Text.translatable(value.getTranslationKey());
         return value == GraphicsMode.FABULOUS ? mutableText.formatted(Formatting.ITALIC) : mutableText;
      },
      new SimpleOption.AlternateValuesSupportingCyclingCallbacks<>(
         Arrays.asList(GraphicsMode.values()),
         Stream.of(GraphicsMode.values()).filter(graphicsMode -> graphicsMode != GraphicsMode.FABULOUS).collect(Collectors.toList()),
         () -> MinecraftClient.getInstance().isRunning() && MinecraftClient.getInstance().getVideoWarningManager().hasCancelledAfterWarning(),
         (option, graphicsMode) -> {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            VideoWarningManager videoWarningManager = minecraftClient.getVideoWarningManager();
            if (graphicsMode == GraphicsMode.FABULOUS && videoWarningManager.canWarn()) {
               videoWarningManager.scheduleWarning();
            } else {
               option.setValue(graphicsMode);
               minecraftClient.worldRenderer.reload();
            }
         },
         Codec.INT.xmap(GraphicsMode::byId, GraphicsMode::getId)
      ),
      GraphicsMode.FANCY,
      value -> {}
   );
   private final SimpleOption<Boolean> ao = SimpleOption.ofBoolean("options.ao", true, value -> MinecraftClient.getInstance().worldRenderer.reload());
   private static final Text NONE_CHUNK_BUILDER_MODE_TOOLTIP = Text.translatable("options.prioritizeChunkUpdates.none.tooltip");
   private static final Text BY_PLAYER_CHUNK_BUILDER_MODE_TOOLTIP = Text.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
   private static final Text NEARBY_CHUNK_BUILDER_MODE_TOOLTIP = Text.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
   private final SimpleOption<ChunkBuilderMode> chunkBuilderMode = new SimpleOption<>(
      "options.prioritizeChunkUpdates",
      value -> {
         return switch (value) {
            case NONE -> Tooltip.of(NONE_CHUNK_BUILDER_MODE_TOOLTIP);
            case PLAYER_AFFECTED -> Tooltip.of(BY_PLAYER_CHUNK_BUILDER_MODE_TOOLTIP);
            case NEARBY -> Tooltip.of(NEARBY_CHUNK_BUILDER_MODE_TOOLTIP);
         };
      },
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(ChunkBuilderMode.values()), Codec.INT.xmap(ChunkBuilderMode::get, ChunkBuilderMode::getId)),
      ChunkBuilderMode.NONE,
      value -> {}
   );
   public List<String> resourcePacks = Lists.newArrayList();
   public List<String> incompatibleResourcePacks = Lists.newArrayList();
   private final SimpleOption<ChatVisibility> chatVisibility = new SimpleOption<>(
      "options.chat.visibility",
      SimpleOption.emptyTooltip(),
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(ChatVisibility.values()), Codec.INT.xmap(ChatVisibility::byId, ChatVisibility::getId)),
      ChatVisibility.FULL,
      value -> {}
   );
   private final SimpleOption<Double> chatOpacity = new SimpleOption<>(
      "options.chat.opacity",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> getPercentValueText(optionText, value * 0.9 + 0.1),
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> chatLineSpacing = new SimpleOption<>(
      "options.chat.line_spacing", SimpleOption.emptyTooltip(), GameOptions::getPercentValueText, SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.0, value -> {}
   );
   private static final Text MENU_BACKGROUND_BLURRINESS_TOOLTIP = Text.translatable("options.accessibility.menu_background_blurriness.tooltip");
   private static final int DEFAULT_MENU_BACKGROUND_BLURRINESS = 5;
   private final SimpleOption<Integer> menuBackgroundBlurriness = new SimpleOption<>(
      "options.accessibility.menu_background_blurriness",
      SimpleOption.constantTooltip(MENU_BACKGROUND_BLURRINESS_TOOLTIP),
      GameOptions::getGenericValueOrOffText,
      new SimpleOption.ValidatingIntSliderCallbacks(0, 10),
      5,
      value -> {}
   );
   private final SimpleOption<Double> textBackgroundOpacity = new SimpleOption<>(
      "options.accessibility.text_background_opacity",
      SimpleOption.emptyTooltip(),
      GameOptions::getPercentValueText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      0.5,
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> panoramaSpeed = new SimpleOption<>(
      "options.accessibility.panorama_speed",
      SimpleOption.emptyTooltip(),
      GameOptions::getPercentValueText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> {}
   );
   private static final Text HIGH_CONTRAST_TOOLTIP = Text.translatable("options.accessibility.high_contrast.tooltip");
   private final SimpleOption<Boolean> highContrast = SimpleOption.ofBoolean(
      "options.accessibility.high_contrast", SimpleOption.constantTooltip(HIGH_CONTRAST_TOOLTIP), false, value -> {
         ResourcePackManager resourcePackManager = MinecraftClient.getInstance().getResourcePackManager();
         boolean blx = resourcePackManager.getEnabledIds().contains("high_contrast");
         if (!blx && value) {
            if (resourcePackManager.enable("high_contrast")) {
               this.refreshResourcePacks(resourcePackManager);
            }
         } else if (blx && !value && resourcePackManager.disable("high_contrast")) {
            this.refreshResourcePacks(resourcePackManager);
         }
      }
   );
   private static final Text HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP = Text.translatable("options.accessibility.high_contrast_block_outline.tooltip");
   private final SimpleOption<Boolean> highContrastBlockOutline = SimpleOption.ofBoolean(
      "options.accessibility.high_contrast_block_outline", SimpleOption.constantTooltip(HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP), false
   );
   private final SimpleOption<Boolean> narratorHotkey = SimpleOption.ofBoolean(
      "options.accessibility.narrator_hotkey",
      SimpleOption.constantTooltip(
         MinecraftClient.IS_SYSTEM_MAC
            ? Text.translatable("options.accessibility.narrator_hotkey.mac.tooltip")
            : Text.translatable("options.accessibility.narrator_hotkey.tooltip")
      ),
      true
   );
   @Nullable
   public String fullscreenResolution;
   public boolean hideServerAddress;
   public boolean advancedItemTooltips;
   public boolean pauseOnLostFocus = true;
   private final Set<PlayerModelPart> enabledPlayerModelParts = EnumSet.allOf(PlayerModelPart.class);
   private final SimpleOption<Arm> mainArm = new SimpleOption<>(
      "options.mainHand",
      SimpleOption.emptyTooltip(),
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(Arm.values()), Arm.CODEC),
      Arm.RIGHT,
      arm -> {}
   );
   public int overrideWidth;
   public int overrideHeight;
   private final SimpleOption<Double> chatScale = new SimpleOption<>(
      "options.chat.scale",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> (Text)(value == 0.0 ? ScreenTexts.composeToggleText(optionText, false) : getPercentValueText(optionText, value)),
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> chatWidth = new SimpleOption<>(
      "options.chat.width",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> getPixelValueText(optionText, ChatHud.getWidth(value)),
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> chatHeightUnfocused = new SimpleOption<>(
      "options.chat.height.unfocused",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> getPixelValueText(optionText, ChatHud.getHeight(value)),
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      ChatHud.getDefaultUnfocusedHeight(),
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> chatHeightFocused = new SimpleOption<>(
      "options.chat.height.focused",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> getPixelValueText(optionText, ChatHud.getHeight(value)),
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
   );
   private final SimpleOption<Double> chatDelay = new SimpleOption<>(
      "options.chat.delay_instant",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> value <= 0.0
         ? Text.translatable("options.chat.delay_none")
         : Text.translatable("options.chat.delay", new Object[]{String.format(Locale.ROOT, "%.1f", value)}),
      new SimpleOption.ValidatingIntSliderCallbacks(0, 60).withModifier(value -> value / 10.0, value -> (int)(value * 10.0)),
      Codec.doubleRange(0.0, 6.0),
      0.0,
      value -> MinecraftClient.getInstance().getMessageHandler().setChatDelay(value)
   );
   private static final Text NOTIFICATION_DISPLAY_TIME_TOOLTIP = Text.translatable("options.notifications.display_time.tooltip");
   private final SimpleOption<Double> notificationDisplayTime = new SimpleOption<>(
      "options.notifications.display_time",
      SimpleOption.constantTooltip(NOTIFICATION_DISPLAY_TIME_TOOLTIP),
      (optionText, value) -> getGenericValueText(optionText, Text.translatable("options.multiplier", new Object[]{value})),
      new SimpleOption.ValidatingIntSliderCallbacks(5, 100).withModifier(sliderProgressValue -> sliderProgressValue / 10.0, value -> (int)(value * 10.0)),
      Codec.doubleRange(0.5, 10.0),
      1.0,
      value -> {}
   );
   private final SimpleOption<Integer> mipmapLevels = new SimpleOption<>(
      "options.mipmapLevels",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> (Text)(value == 0 ? ScreenTexts.composeToggleText(optionText, false) : getGenericValueText(optionText, value)),
      new SimpleOption.ValidatingIntSliderCallbacks(0, 4),
      4,
      value -> {}
   );
   public boolean useNativeTransport = true;
   private final SimpleOption<AttackIndicator> attackIndicator = new SimpleOption<>(
      "options.attackIndicator",
      SimpleOption.emptyTooltip(),
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(AttackIndicator.values()), Codec.INT.xmap(AttackIndicator::byId, AttackIndicator::getId)),
      AttackIndicator.CROSSHAIR,
      value -> {}
   );
   public TutorialStep tutorialStep = TutorialStep.MOVEMENT;
   public boolean joinedFirstServer = false;
   private final SimpleOption<Integer> biomeBlendRadius = new SimpleOption<>("options.biomeBlendRadius", SimpleOption.emptyTooltip(), (optionText, value) -> {
      int i = value * 2 + 1;
      return getGenericValueText(optionText, Text.translatable("options.biomeBlendRadius." + i));
   }, new SimpleOption.ValidatingIntSliderCallbacks(0, 7, false), 2, value -> MinecraftClient.getInstance().worldRenderer.reload());
   private final SimpleOption<Double> mouseWheelSensitivity = new SimpleOption<>(
      "options.mouseWheelSensitivity",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> getGenericValueText(optionText, Text.literal(String.format(Locale.ROOT, "%.2f", value))),
      new SimpleOption.ValidatingIntSliderCallbacks(-200, 100)
         .withModifier(GameOptions::toMouseWheelSensitivityValue, GameOptions::toMouseWheelSensitivitySliderProgressValue),
      Codec.doubleRange(toMouseWheelSensitivityValue(-200), toMouseWheelSensitivityValue(100)),
      toMouseWheelSensitivityValue(0),
      value -> {}
   );
   private final SimpleOption<Boolean> rawMouseInput = SimpleOption.ofBoolean("options.rawMouseInput", true, value -> {
      Window window = MinecraftClient.getInstance().getWindow();
      if (window != null) {
         window.setRawMouseMotion(value);
      }
   });
   public int glDebugVerbosity = 1;
   private final SimpleOption<Boolean> autoJump = SimpleOption.ofBoolean("options.autoJump", false);
   private static final Text ROTATE_WITH_MINECART_TOOLTIP = Text.translatable("options.rotateWithMinecart.tooltip");
   private final SimpleOption<Boolean> rotateWithMinecart = SimpleOption.ofBoolean(
      "options.rotateWithMinecart", SimpleOption.constantTooltip(ROTATE_WITH_MINECART_TOOLTIP), false
   );
   private final SimpleOption<Boolean> operatorItemsTab = SimpleOption.ofBoolean("options.operatorItemsTab", false);
   private final SimpleOption<Boolean> autoSuggestions = SimpleOption.ofBoolean("options.autoSuggestCommands", true);
   private final SimpleOption<Boolean> chatColors = SimpleOption.ofBoolean("options.chat.color", true);
   private final SimpleOption<Boolean> chatLinks = SimpleOption.ofBoolean("options.chat.links", true);
   private final SimpleOption<Boolean> chatLinksPrompt = SimpleOption.ofBoolean("options.chat.links.prompt", true);
   private final SimpleOption<Boolean> enableVsync = SimpleOption.ofBoolean("options.vsync", true, value -> {
      if (MinecraftClient.getInstance().getWindow() != null) {
         MinecraftClient.getInstance().getWindow().setVsync(value);
      }
   });
   private final SimpleOption<Boolean> entityShadows = SimpleOption.ofBoolean("options.entityShadows", true);
   private final SimpleOption<Boolean> forceUnicodeFont = SimpleOption.ofBoolean("options.forceUnicodeFont", false, value -> onFontOptionsChanged());
   private final SimpleOption<Boolean> japaneseGlyphVariants = SimpleOption.ofBoolean(
      "options.japaneseGlyphVariants",
      SimpleOption.constantTooltip(Text.translatable("options.japaneseGlyphVariants.tooltip")),
      shouldUseJapaneseGlyphsByDefault(),
      value -> onFontOptionsChanged()
   );
   private final SimpleOption<Boolean> invertYMouse = SimpleOption.ofBoolean("options.invertMouse", false);
   private final SimpleOption<Boolean> discreteMouseScroll = SimpleOption.ofBoolean("options.discrete_mouse_scroll", false);
   private static final Text REALMS_NOTIFICATIONS_TOOLTIP = Text.translatable("options.realmsNotifications.tooltip");
   private final SimpleOption<Boolean> realmsNotifications = SimpleOption.ofBoolean(
      "options.realmsNotifications", SimpleOption.constantTooltip(REALMS_NOTIFICATIONS_TOOLTIP), true
   );
   private static final Text ALLOW_SERVER_LISTING_TOOLTIP = Text.translatable("options.allowServerListing.tooltip");
   private final SimpleOption<Boolean> allowServerListing = SimpleOption.ofBoolean(
      "options.allowServerListing", SimpleOption.constantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, boolean_ -> {}
   );
   private final SimpleOption<Boolean> reducedDebugInfo = SimpleOption.ofBoolean("options.reducedDebugInfo", false);
   private final Map<SoundCategory, SimpleOption<Double>> soundVolumeLevels = (Map<SoundCategory, SimpleOption<Double>>)Util.make(
      new EnumMap(SoundCategory.class), soundVolumeLevels -> {
         for (SoundCategory soundCategory : SoundCategory.values()) {
            soundVolumeLevels.put(soundCategory, this.createSoundVolumeOption("soundCategory." + soundCategory.getName(), soundCategory));
         }
      }
   );
   private final SimpleOption<Boolean> showSubtitles = SimpleOption.ofBoolean("options.showSubtitles", false);
   private static final Text DIRECTIONAL_AUDIO_ON_TOOLTIP = Text.translatable("options.directionalAudio.on.tooltip");
   private static final Text DIRECTIONAL_AUDIO_OFF_TOOLTIP = Text.translatable("options.directionalAudio.off.tooltip");
   private final SimpleOption<Boolean> directionalAudio = SimpleOption.ofBoolean(
      "options.directionalAudio", value -> value ? Tooltip.of(DIRECTIONAL_AUDIO_ON_TOOLTIP) : Tooltip.of(DIRECTIONAL_AUDIO_OFF_TOOLTIP), false, value -> {
         SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
         soundManager.reloadSounds();
         soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   );
   private final SimpleOption<Boolean> backgroundForChatOnly = new SimpleOption<>(
      "options.accessibility.text_background",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> value
         ? Text.translatable("options.accessibility.text_background.chat")
         : Text.translatable("options.accessibility.text_background.everywhere"),
      SimpleOption.BOOLEAN,
      true,
      value -> {}
   );
   private final SimpleOption<Boolean> touchscreen = SimpleOption.ofBoolean("options.touchscreen", false);
   private final SimpleOption<Boolean> fullscreen = SimpleOption.ofBoolean("options.fullscreen", false, value -> {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.getWindow() != null && minecraftClient.getWindow().isFullscreen() != value) {
         minecraftClient.getWindow().toggleFullscreen();
         this.getFullscreen().setValue(minecraftClient.getWindow().isFullscreen());
      }
   });
   private final SimpleOption<Boolean> bobView = SimpleOption.ofBoolean("options.viewBobbing", true);
   private static final Text TOGGLE_KEY_TEXT = Text.translatable("options.key.toggle");
   private static final Text HOLD_KEY_TEXT = Text.translatable("options.key.hold");
   private final SimpleOption<Boolean> sneakToggled = new SimpleOption<>(
      "key.sneak", SimpleOption.emptyTooltip(), (optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT, SimpleOption.BOOLEAN, false, value -> {}
   );
   private final SimpleOption<Boolean> sprintToggled = new SimpleOption<>(
      "key.sprint", SimpleOption.emptyTooltip(), (optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT, SimpleOption.BOOLEAN, false, value -> {}
   );
   public boolean skipMultiplayerWarning;
   private static final Text HIDE_MATCHED_NAMES_TOOLTIP = Text.translatable("options.hideMatchedNames.tooltip");
   private final SimpleOption<Boolean> hideMatchedNames = SimpleOption.ofBoolean(
      "options.hideMatchedNames", SimpleOption.constantTooltip(HIDE_MATCHED_NAMES_TOOLTIP), true
   );
   private final SimpleOption<Boolean> showAutosaveIndicator = SimpleOption.ofBoolean("options.autosaveIndicator", true);
   private static final Text ONLY_SHOW_SECURE_CHAT_TOOLTIP = Text.translatable("options.onlyShowSecureChat.tooltip");
   private final SimpleOption<Boolean> onlyShowSecureChat = SimpleOption.ofBoolean(
      "options.onlyShowSecureChat", SimpleOption.constantTooltip(ONLY_SHOW_SECURE_CHAT_TOOLTIP), false
   );
   public final KeyBinding forwardKey = new KeyBinding("key.forward", 87, "key.categories.movement");
   public final KeyBinding leftKey = new KeyBinding("key.left", 65, "key.categories.movement");
   public final KeyBinding backKey = new KeyBinding("key.back", 83, "key.categories.movement");
   public final KeyBinding rightKey = new KeyBinding("key.right", 68, "key.categories.movement");
   public final KeyBinding jumpKey = new KeyBinding("key.jump", 32, "key.categories.movement");
   public final KeyBinding sneakKey = new StickyKeyBinding("key.sneak", 340, "key.categories.movement", this.sneakToggled::getValue);
   public final KeyBinding sprintKey = new StickyKeyBinding("key.sprint", 341, "key.categories.movement", this.sprintToggled::getValue);
   public final KeyBinding inventoryKey = new KeyBinding("key.inventory", 69, "key.categories.inventory");
   public final KeyBinding swapHandsKey = new KeyBinding("key.swapOffhand", 70, "key.categories.inventory");
   public final KeyBinding dropKey = new KeyBinding("key.drop", 81, "key.categories.inventory");
   public final KeyBinding useKey = new KeyBinding("key.use", InputUtil.Type.MOUSE, 1, "key.categories.gameplay");
   public final KeyBinding attackKey = new KeyBinding("key.attack", InputUtil.Type.MOUSE, 0, "key.categories.gameplay");
   public final KeyBinding pickItemKey = new KeyBinding("key.pickItem", InputUtil.Type.MOUSE, 2, "key.categories.gameplay");
   public final KeyBinding chatKey = new KeyBinding("key.chat", 84, "key.categories.multiplayer");
   public final KeyBinding playerListKey = new KeyBinding("key.playerlist", 258, "key.categories.multiplayer");
   public final KeyBinding commandKey = new KeyBinding("key.command", 47, "key.categories.multiplayer");
   public final KeyBinding socialInteractionsKey = new KeyBinding("key.socialInteractions", 80, "key.categories.multiplayer");
   public final KeyBinding screenshotKey = new KeyBinding("key.screenshot", 291, "key.categories.misc");
   public final KeyBinding togglePerspectiveKey = new KeyBinding("key.togglePerspective", 294, "key.categories.misc");
   public final KeyBinding smoothCameraKey = new KeyBinding("key.smoothCamera", InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc");
   public final KeyBinding fullscreenKey = new KeyBinding("key.fullscreen", 300, "key.categories.misc");
   public final KeyBinding spectatorOutlinesKey = new KeyBinding("key.spectatorOutlines", InputUtil.UNKNOWN_KEY.getCode(), "key.categories.misc");
   public final KeyBinding advancementsKey = new KeyBinding("key.advancements", 76, "key.categories.misc");
   public final KeyBinding zoomKey = new KeyBinding("zoomify.key.zoom", 67, "zoomify.key.category");
   public final KeyBinding secondaryZoomKey = new KeyBinding("zoomify.key.zoom.secondary", InputUtil.UNKNOWN_KEY.getCode(), "zoomify.key.category");
   public final KeyBinding[] hotbarKeys = new KeyBinding[]{
      new KeyBinding("key.hotbar.1", 49, "key.categories.inventory"),
      new KeyBinding("key.hotbar.2", 50, "key.categories.inventory"),
      new KeyBinding("key.hotbar.3", 51, "key.categories.inventory"),
      new KeyBinding("key.hotbar.4", 52, "key.categories.inventory"),
      new KeyBinding("key.hotbar.5", 53, "key.categories.inventory"),
      new KeyBinding("key.hotbar.6", 54, "key.categories.inventory"),
      new KeyBinding("key.hotbar.7", 55, "key.categories.inventory"),
      new KeyBinding("key.hotbar.8", 56, "key.categories.inventory"),
      new KeyBinding("key.hotbar.9", 57, "key.categories.inventory")
   };
   public final KeyBinding saveToolbarActivatorKey = new KeyBinding("key.saveToolbarActivator", 67, "key.categories.creative");
   public final KeyBinding loadToolbarActivatorKey = new KeyBinding("key.loadToolbarActivator", 88, "key.categories.creative");
   public KeyBinding[] allKeys = (KeyBinding[])ArrayUtils.addAll(
      new KeyBinding[]{
         this.attackKey,
         this.useKey,
         this.forwardKey,
         this.leftKey,
         this.backKey,
         this.rightKey,
         this.jumpKey,
         this.sneakKey,
         this.sprintKey,
         this.dropKey,
         this.inventoryKey,
         this.chatKey,
         this.playerListKey,
         this.pickItemKey,
         this.commandKey,
         this.socialInteractionsKey,
         this.screenshotKey,
         this.togglePerspectiveKey,
         this.smoothCameraKey,
         this.fullscreenKey,
         this.spectatorOutlinesKey,
         this.swapHandsKey,
         this.saveToolbarActivatorKey,
         this.loadToolbarActivatorKey,
         this.advancementsKey,
         this.zoomKey,
         this.secondaryZoomKey
      },
      this.hotbarKeys
   );
   protected MinecraftClient client;
   private final File optionsFile;
   public boolean hudHidden;
   private Perspective perspective = Perspective.FIRST_PERSON;
   public String lastServer = "";
   public boolean smoothCameraEnabled;
   private final SimpleOption<Integer> fov = new SimpleOption<>(
      "options.fov",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> {
         return switch (value) {
            case 70 -> getGenericValueText(optionText, Text.translatable("options.fov.min"));
            case 110 -> getGenericValueText(optionText, Text.translatable("options.fov.max"));
            default -> getGenericValueText(optionText, value);
         };
      },
      new SimpleOption.ValidatingIntSliderCallbacks(30, 110),
      Codec.DOUBLE.xmap(value -> (int)(value * 40.0 + 70.0), value -> (value.intValue() - 70.0) / 40.0),
      70,
      value -> MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate()
   );
   private static final Text TELEMETRY_TOOLTIP = Text.translatable(
      "options.telemetry.button.tooltip", new Object[]{Text.translatable("options.telemetry.state.minimal"), Text.translatable("options.telemetry.state.all")}
   );
   private final SimpleOption<Boolean> telemetryOptInExtra = SimpleOption.ofBoolean(
      "options.telemetry.button",
      SimpleOption.constantTooltip(TELEMETRY_TOOLTIP),
      (optionText, value) -> {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (!minecraftClient.isTelemetryEnabledByApi()) {
            return Text.translatable("options.telemetry.state.none");
         } else {
            return value && minecraftClient.isOptionalTelemetryEnabledByApi()
               ? Text.translatable("options.telemetry.state.all")
               : Text.translatable("options.telemetry.state.minimal");
         }
      },
      false,
      value -> {}
   );
   private static final Text SCREEN_EFFECT_SCALE_TOOLTIP = Text.translatable("options.screenEffectScale.tooltip");
   private final SimpleOption<Double> distortionEffectScale = new SimpleOption<>(
      "options.screenEffectScale",
      SimpleOption.constantTooltip(SCREEN_EFFECT_SCALE_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> {}
   );
   private static final Text FOV_EFFECT_SCALE_TOOLTIP = Text.translatable("options.fovEffectScale.tooltip");
   private final SimpleOption<Double> fovEffectScale = new SimpleOption<>(
      "options.fovEffectScale",
      SimpleOption.constantTooltip(FOV_EFFECT_SCALE_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(MathHelper::square, Math::sqrt),
      Codec.doubleRange(0.0, 1.0),
      1.0,
      value -> {}
   );
   private static final Text DARKNESS_EFFECT_SCALE_TOOLTIP = Text.translatable("options.darknessEffectScale.tooltip");
   private final SimpleOption<Double> darknessEffectScale = new SimpleOption<>(
      "options.darknessEffectScale",
      SimpleOption.constantTooltip(DARKNESS_EFFECT_SCALE_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(MathHelper::square, Math::sqrt),
      1.0,
      value -> {}
   );
   private static final Text GLINT_SPEED_TOOLTIP = Text.translatable("options.glintSpeed.tooltip");
   private final SimpleOption<Double> glintSpeed = new SimpleOption<>(
      "options.glintSpeed",
      SimpleOption.constantTooltip(GLINT_SPEED_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      0.5,
      value -> {}
   );
   private static final Text GLINT_STRENGTH_TOOLTIP = Text.translatable("options.glintStrength.tooltip");
   private final SimpleOption<Double> glintStrength = new SimpleOption<>(
      "options.glintStrength",
      SimpleOption.constantTooltip(GLINT_STRENGTH_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      0.75,
      RenderSystem::setShaderGlintAlpha
   );
   private static final Text DAMAGE_TILT_STRENGTH_TOOLTIP = Text.translatable("options.damageTiltStrength.tooltip");
   private final SimpleOption<Double> damageTiltStrength = new SimpleOption<>(
      "options.damageTiltStrength",
      SimpleOption.constantTooltip(DAMAGE_TILT_STRENGTH_TOOLTIP),
      GameOptions::getPercentValueOrOffText,
      SimpleOption.DoubleSliderCallbacks.INSTANCE,
      1.0,
      value -> {}
   );
   public SimpleOption<Double> gamma = new SimpleOption<>("options.gamma", SimpleOption.emptyTooltip(), (optionText, value) -> {
      int i = (int)(value * 100.0);
      if (i == 0) {
         return getGenericValueText(optionText, Text.translatable("options.gamma.min"));
      } else if (i == 50) {
         return getGenericValueText(optionText, Text.translatable("options.gamma.default"));
      } else {
         return i == 100 ? getGenericValueText(optionText, Text.translatable("options.gamma.max")) : getGenericValueText(optionText, i);
      }
   }, SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.5, value -> {});
   public static final int field_43405 = 0;
   private static final int MAX_SERIALIZABLE_GUI_SCALE = 2147483646;
   private final SimpleOption<Integer> guiScale = new SimpleOption<>(
      "options.guiScale",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> value == 0 ? Text.translatable("options.guiScale.auto") : Text.literal(Integer.toString(value)),
      new SimpleOption.MaxSuppliableIntCallbacks(0, () -> {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         return !minecraftClient.isRunning() ? 2147483646 : minecraftClient.getWindow().calculateScaleFactor(0, minecraftClient.forcesUnicodeFont());
      }, 2147483646),
      0,
      value -> this.client.onResolutionChanged()
   );
   private final SimpleOption<ParticlesMode> particles = new SimpleOption<>(
      "options.particles",
      SimpleOption.emptyTooltip(),
      SimpleOption.enumValueText(),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(ParticlesMode.values()), Codec.INT.xmap(ParticlesMode::byId, ParticlesMode::getId)),
      ParticlesMode.ALL,
      value -> {}
   );
   private final SimpleOption<NarratorMode> narrator = new SimpleOption<>(
      "options.narrator",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> (Text)(this.client.getNarratorManager().isActive() ? value.getName() : Text.translatable("options.narrator.notavailable")),
      new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(NarratorMode.values()), Codec.INT.xmap(NarratorMode::byId, NarratorMode::getId)),
      NarratorMode.OFF,
      value -> this.client.getNarratorManager().onModeChange(value)
   );
   public String language = "en_us";
   private final SimpleOption<String> soundDevice = new SimpleOption<>(
      "options.audioDevice",
      SimpleOption.emptyTooltip(),
      (optionText, value) -> {
         if ("".equals(value)) {
            return Text.translatable("options.audioDevice.default");
         } else {
            return value.startsWith("OpenAL Soft on ") ? Text.literal(value.substring(SoundSystem.OPENAL_SOFT_ON_LENGTH)) : Text.literal(value);
         }
      },
      new SimpleOption.LazyCyclingCallbacks<>(
         () -> Stream.concat(Stream.of(""), MinecraftClient.getInstance().getSoundManager().getSoundDevices().stream()).toList(),
         value -> MinecraftClient.getInstance().isRunning()
               && value != ""
               && !MinecraftClient.getInstance().getSoundManager().getSoundDevices().contains(value)
            ? Optional.empty()
            : Optional.of(value),
         Codec.STRING
      ),
      "",
      value -> {
         SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
         soundManager.reloadSounds();
         soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
      }
   );
   public boolean onboardAccessibility = true;
   public boolean syncChunkWrites;

   public SimpleOption<Boolean> getMonochromeLogo() {
      return this.monochromeLogo;
   }

   public SimpleOption<Boolean> getHideLightningFlashes() {
      return this.hideLightningFlashes;
   }

   public SimpleOption<Boolean> getHideSplashTexts() {
      return this.hideSplashTexts;
   }

   public SimpleOption<Double> getMouseSensitivity() {
      return this.mouseSensitivity;
   }

   public SimpleOption<Integer> getViewDistance() {
      return this.viewDistance;
   }

   public SimpleOption<Integer> getSimulationDistance() {
      return this.simulationDistance;
   }

   public SimpleOption<Double> getEntityDistanceScaling() {
      return this.entityDistanceScaling;
   }

   public SimpleOption<Integer> getMaxFps() {
      return this.maxFps;
   }

   public SimpleOption<InactivityFpsLimit> getInactivityFpsLimit() {
      return this.inactivityFpsLimit;
   }

   public SimpleOption<CloudRenderMode> getCloudRenderMode() {
      return this.cloudRenderMode;
   }

   public SimpleOption<GraphicsMode> getGraphicsMode() {
      return this.graphicsMode;
   }

   public SimpleOption<Boolean> getAo() {
      return this.ao;
   }

   public SimpleOption<ChunkBuilderMode> getChunkBuilderMode() {
      return this.chunkBuilderMode;
   }

   public void refreshResourcePacks(ResourcePackManager resourcePackManager) {
      List<String> list = ImmutableList.copyOf(this.resourcePacks);
      this.resourcePacks.clear();
      this.incompatibleResourcePacks.clear();

      for (ResourcePackProfile resourcePackProfile : resourcePackManager.getEnabledProfiles()) {
         if (!resourcePackProfile.isPinned()) {
            this.resourcePacks.add(resourcePackProfile.getId());
            if (!resourcePackProfile.getCompatibility().isCompatible()) {
               this.incompatibleResourcePacks.add(resourcePackProfile.getId());
            }
         }
      }

      this.write();
      List<String> list2 = ImmutableList.copyOf(this.resourcePacks);
      if (!list2.equals(list)) {
         this.client.reloadResources();
      }
   }

   public SimpleOption<ChatVisibility> getChatVisibility() {
      return this.chatVisibility;
   }

   public SimpleOption<Double> getChatOpacity() {
      return this.chatOpacity;
   }

   public SimpleOption<Double> getChatLineSpacing() {
      return this.chatLineSpacing;
   }

   public SimpleOption<Integer> getMenuBackgroundBlurriness() {
      return this.menuBackgroundBlurriness;
   }

   public int getMenuBackgroundBlurrinessValue() {
      return this.getMenuBackgroundBlurriness().getValue();
   }

   public SimpleOption<Double> getTextBackgroundOpacity() {
      return this.textBackgroundOpacity;
   }

   public SimpleOption<Double> getPanoramaSpeed() {
      return this.panoramaSpeed;
   }

   public SimpleOption<Boolean> getHighContrast() {
      return this.highContrast;
   }

   public SimpleOption<Boolean> getHighContrastBlockOutline() {
      return this.highContrastBlockOutline;
   }

   public SimpleOption<Boolean> getNarratorHotkey() {
      return this.narratorHotkey;
   }

   public SimpleOption<Arm> getMainArm() {
      return this.mainArm;
   }

   public SimpleOption<Double> getChatScale() {
      return this.chatScale;
   }

   public SimpleOption<Double> getChatWidth() {
      return this.chatWidth;
   }

   public SimpleOption<Double> getChatHeightUnfocused() {
      return this.chatHeightUnfocused;
   }

   public SimpleOption<Double> getChatHeightFocused() {
      return this.chatHeightFocused;
   }

   public SimpleOption<Double> getChatDelay() {
      return this.chatDelay;
   }

   public SimpleOption<Double> getNotificationDisplayTime() {
      return this.notificationDisplayTime;
   }

   public SimpleOption<Integer> getMipmapLevels() {
      return this.mipmapLevels;
   }

   public SimpleOption<AttackIndicator> getAttackIndicator() {
      return this.attackIndicator;
   }

   public SimpleOption<Integer> getBiomeBlendRadius() {
      return this.biomeBlendRadius;
   }

   private static double toMouseWheelSensitivityValue(int value) {
      return Math.pow(10.0, value / 100.0);
   }

   private static int toMouseWheelSensitivitySliderProgressValue(double value) {
      return MathHelper.floor(Math.log10(value) * 100.0);
   }

   public SimpleOption<Double> getMouseWheelSensitivity() {
      return this.mouseWheelSensitivity;
   }

   public SimpleOption<Boolean> getRawMouseInput() {
      return this.rawMouseInput;
   }

   public SimpleOption<Boolean> getAutoJump() {
      return this.autoJump;
   }

   public SimpleOption<Boolean> getRotateWithMinecart() {
      return this.rotateWithMinecart;
   }

   public SimpleOption<Boolean> getOperatorItemsTab() {
      return this.operatorItemsTab;
   }

   public SimpleOption<Boolean> getAutoSuggestions() {
      return this.autoSuggestions;
   }

   public SimpleOption<Boolean> getChatColors() {
      return this.chatColors;
   }

   public SimpleOption<Boolean> getChatLinks() {
      return this.chatLinks;
   }

   public SimpleOption<Boolean> getChatLinksPrompt() {
      return this.chatLinksPrompt;
   }

   public SimpleOption<Boolean> getEnableVsync() {
      return this.enableVsync;
   }

   public SimpleOption<Boolean> getEntityShadows() {
      return this.entityShadows;
   }

   private static void onFontOptionsChanged() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.getWindow() != null) {
         minecraftClient.onFontOptionsChanged();
         minecraftClient.onResolutionChanged();
      }
   }

   public SimpleOption<Boolean> getForceUnicodeFont() {
      return this.forceUnicodeFont;
   }

   private static boolean shouldUseJapaneseGlyphsByDefault() {
      return Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
   }

   public SimpleOption<Boolean> getJapaneseGlyphVariants() {
      return this.japaneseGlyphVariants;
   }

   public SimpleOption<Boolean> getInvertYMouse() {
      return this.invertYMouse;
   }

   public SimpleOption<Boolean> getDiscreteMouseScroll() {
      return this.discreteMouseScroll;
   }

   public SimpleOption<Boolean> getRealmsNotifications() {
      return this.realmsNotifications;
   }

   public SimpleOption<Boolean> getAllowServerListing() {
      return this.allowServerListing;
   }

   public SimpleOption<Boolean> getReducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public final float getSoundVolume(SoundCategory category) {
      return this.getSoundVolumeOption(category).getValue().floatValue();
   }

   public final SimpleOption<Double> getSoundVolumeOption(SoundCategory category) {
      return Objects.requireNonNull(this.soundVolumeLevels.get(category));
   }

   private SimpleOption<Double> createSoundVolumeOption(String key, SoundCategory category) {
      return new SimpleOption<>(
         key,
         SimpleOption.emptyTooltip(),
         GameOptions::getPercentValueOrOffText,
         SimpleOption.DoubleSliderCallbacks.INSTANCE,
         1.0,
         value -> MinecraftClient.getInstance().getSoundManager().updateSoundVolume(category, value.floatValue())
      );
   }

   public SimpleOption<Boolean> getShowSubtitles() {
      return this.showSubtitles;
   }

   public SimpleOption<Boolean> getDirectionalAudio() {
      return this.directionalAudio;
   }

   public SimpleOption<Boolean> getBackgroundForChatOnly() {
      return this.backgroundForChatOnly;
   }

   public SimpleOption<Boolean> getTouchscreen() {
      return this.touchscreen;
   }

   public SimpleOption<Boolean> getFullscreen() {
      return this.fullscreen;
   }

   public SimpleOption<Boolean> getBobView() {
      return this.bobView;
   }

   public SimpleOption<Boolean> getSneakToggled() {
      return this.sneakToggled;
   }

   public SimpleOption<Boolean> getSprintToggled() {
      return this.sprintToggled;
   }

   public SimpleOption<Boolean> getHideMatchedNames() {
      return this.hideMatchedNames;
   }

   public SimpleOption<Boolean> getShowAutosaveIndicator() {
      return this.showAutosaveIndicator;
   }

   public SimpleOption<Boolean> getOnlyShowSecureChat() {
      return this.onlyShowSecureChat;
   }

   public SimpleOption<Integer> getFov() {
      return this.fov;
   }

   public SimpleOption<Boolean> getTelemetryOptInExtra() {
      return this.telemetryOptInExtra;
   }

   public SimpleOption<Double> getDistortionEffectScale() {
      return this.distortionEffectScale;
   }

   public SimpleOption<Double> getFovEffectScale() {
      return this.fovEffectScale;
   }

   public SimpleOption<Double> getDarknessEffectScale() {
      return this.darknessEffectScale;
   }

   public SimpleOption<Double> getGlintSpeed() {
      return this.glintSpeed;
   }

   public SimpleOption<Double> getGlintStrength() {
      return this.glintStrength;
   }

   public SimpleOption<Double> getDamageTiltStrength() {
      return this.damageTiltStrength;
   }

   public SimpleOption<Double> getGamma() {
      return this.gamma;
   }

   public SimpleOption<Integer> getGuiScale() {
      return this.guiScale;
   }

   public SimpleOption<ParticlesMode> getParticles() {
      return this.particles;
   }

   public SimpleOption<NarratorMode> getNarrator() {
      return this.narrator;
   }

   public SimpleOption<String> getSoundDevice() {
      return this.soundDevice;
   }

   public void setAccessibilityOnboarded() {
      this.onboardAccessibility = false;
      this.write();
   }

   public GameOptions(MinecraftClient client, File optionsFile) {
      this.client = client;
      this.optionsFile = new File(optionsFile, "options.txt");
      boolean bl = Runtime.getRuntime().maxMemory() >= 1000000000L;
      this.viewDistance = new SimpleOption<>(
         "options.renderDistance",
         SimpleOption.emptyTooltip(),
         (optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", new Object[]{value})),
         new SimpleOption.ValidatingIntSliderCallbacks(2, bl ? 32 : 16, false),
         12,
         value -> MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate()
      );
      this.simulationDistance = new SimpleOption<>(
         "options.simulationDistance",
         SimpleOption.emptyTooltip(),
         (optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", new Object[]{value})),
         new SimpleOption.ValidatingIntSliderCallbacks(5, bl ? 32 : 16, false),
         12,
         value -> {}
      );
      this.syncChunkWrites = Util.getOperatingSystem() == OperatingSystem.WINDOWS;
      synchronized (GameOptions.class) {
         if (!iris$initialized) {
            iris$initialized = true;
            new Iris().onEarlyInitialize();
         }
      }

      this.allKeys = (KeyBinding[])ArrayUtils.addAll(this.allKeys, StandaloneIrisPlatformHelpers.getRegisteredKeyBindings());
      this.load();
   }

   public float getTextBackgroundOpacity(float fallback) {
      return this.backgroundForChatOnly.getValue() ? fallback : this.getTextBackgroundOpacity().getValue().floatValue();
   }

   public int getTextBackgroundColor(float fallbackOpacity) {
      return ColorHelper.fromFloats(this.getTextBackgroundOpacity(fallbackOpacity), 0.0F, 0.0F, 0.0F);
   }

   public int getTextBackgroundColor(int fallbackColor) {
      return this.backgroundForChatOnly.getValue()
         ? fallbackColor
         : ColorHelper.fromFloats(this.textBackgroundOpacity.getValue().floatValue(), 0.0F, 0.0F, 0.0F);
   }

   private void acceptProfiledOptions(GameOptions.OptionVisitor visitor) {
      visitor.accept("ao", this.ao);
      visitor.accept("biomeBlendRadius", this.biomeBlendRadius);
      visitor.accept("enableVsync", this.enableVsync);
      visitor.accept("entityDistanceScaling", this.entityDistanceScaling);
      visitor.accept("entityShadows", this.entityShadows);
      visitor.accept("forceUnicodeFont", this.forceUnicodeFont);
      visitor.accept("japaneseGlyphVariants", this.japaneseGlyphVariants);
      visitor.accept("fov", this.fov);
      visitor.accept("fovEffectScale", this.fovEffectScale);
      visitor.accept("darknessEffectScale", this.darknessEffectScale);
      visitor.accept("glintSpeed", this.glintSpeed);
      visitor.accept("glintStrength", this.glintStrength);
      visitor.accept("prioritizeChunkUpdates", this.chunkBuilderMode);
      visitor.accept("fullscreen", this.fullscreen);
      visitor.accept("gamma", this.gamma);
      visitor.accept("graphicsMode", this.graphicsMode);
      visitor.accept("guiScale", this.guiScale);
      visitor.accept("maxFps", this.maxFps);
      visitor.accept("inactivityFpsLimit", this.inactivityFpsLimit);
      visitor.accept("mipmapLevels", this.mipmapLevels);
      visitor.accept("narrator", this.narrator);
      visitor.accept("particles", this.particles);
      visitor.accept("reducedDebugInfo", this.reducedDebugInfo);
      visitor.accept("renderClouds", this.cloudRenderMode);
      visitor.accept("renderDistance", this.viewDistance);
      visitor.accept("simulationDistance", this.simulationDistance);
      visitor.accept("screenEffectScale", this.distortionEffectScale);
      visitor.accept("soundDevice", this.soundDevice);
   }

   private void accept(GameOptions.Visitor visitor) {
      this.acceptProfiledOptions(visitor);
      visitor.accept("autoJump", this.autoJump);
      visitor.accept("rotateWithMinecart", this.rotateWithMinecart);
      visitor.accept("operatorItemsTab", this.operatorItemsTab);
      visitor.accept("autoSuggestions", this.autoSuggestions);
      visitor.accept("chatColors", this.chatColors);
      visitor.accept("chatLinks", this.chatLinks);
      visitor.accept("chatLinksPrompt", this.chatLinksPrompt);
      visitor.accept("discrete_mouse_scroll", this.discreteMouseScroll);
      visitor.accept("invertYMouse", this.invertYMouse);
      visitor.accept("realmsNotifications", this.realmsNotifications);
      visitor.accept("showSubtitles", this.showSubtitles);
      visitor.accept("directionalAudio", this.directionalAudio);
      visitor.accept("touchscreen", this.touchscreen);
      visitor.accept("bobView", this.bobView);
      visitor.accept("toggleCrouch", this.sneakToggled);
      visitor.accept("toggleSprint", this.sprintToggled);
      visitor.accept("darkMojangStudiosBackground", this.monochromeLogo);
      visitor.accept("hideLightningFlashes", this.hideLightningFlashes);
      visitor.accept("hideSplashTexts", this.hideSplashTexts);
      visitor.accept("mouseSensitivity", this.mouseSensitivity);
      visitor.accept("damageTiltStrength", this.damageTiltStrength);
      visitor.accept("highContrast", this.highContrast);
      visitor.accept("highContrastBlockOutline", this.highContrastBlockOutline);
      visitor.accept("narratorHotkey", this.narratorHotkey);
      this.resourcePacks = visitor.visitObject("resourcePacks", this.resourcePacks, GameOptions::parseList, GSON::toJson);
      this.incompatibleResourcePacks = visitor.visitObject("incompatibleResourcePacks", this.incompatibleResourcePacks, GameOptions::parseList, GSON::toJson);
      this.lastServer = visitor.visitString("lastServer", this.lastServer);
      this.language = visitor.visitString("lang", this.language);
      visitor.accept("chatVisibility", this.chatVisibility);
      visitor.accept("chatOpacity", this.chatOpacity);
      visitor.accept("chatLineSpacing", this.chatLineSpacing);
      visitor.accept("textBackgroundOpacity", this.textBackgroundOpacity);
      visitor.accept("backgroundForChatOnly", this.backgroundForChatOnly);
      this.hideServerAddress = visitor.visitBoolean("hideServerAddress", this.hideServerAddress);
      this.advancedItemTooltips = visitor.visitBoolean("advancedItemTooltips", this.advancedItemTooltips);
      this.pauseOnLostFocus = visitor.visitBoolean("pauseOnLostFocus", this.pauseOnLostFocus);
      this.overrideWidth = visitor.visitInt("overrideWidth", this.overrideWidth);
      this.overrideHeight = visitor.visitInt("overrideHeight", this.overrideHeight);
      visitor.accept("chatHeightFocused", this.chatHeightFocused);
      visitor.accept("chatDelay", this.chatDelay);
      visitor.accept("chatHeightUnfocused", this.chatHeightUnfocused);
      visitor.accept("chatScale", this.chatScale);
      visitor.accept("chatWidth", this.chatWidth);
      visitor.accept("notificationDisplayTime", this.notificationDisplayTime);
      this.useNativeTransport = visitor.visitBoolean("useNativeTransport", this.useNativeTransport);
      visitor.accept("mainHand", this.mainArm);
      visitor.accept("attackIndicator", this.attackIndicator);
      this.tutorialStep = visitor.visitObject("tutorialStep", this.tutorialStep, TutorialStep::byName, TutorialStep::getName);
      visitor.accept("mouseWheelSensitivity", this.mouseWheelSensitivity);
      visitor.accept("rawMouseInput", this.rawMouseInput);
      this.glDebugVerbosity = visitor.visitInt("glDebugVerbosity", this.glDebugVerbosity);
      this.skipMultiplayerWarning = visitor.visitBoolean("skipMultiplayerWarning", this.skipMultiplayerWarning);
      visitor.accept("hideMatchedNames", this.hideMatchedNames);
      this.joinedFirstServer = visitor.visitBoolean("joinedFirstServer", this.joinedFirstServer);
      this.syncChunkWrites = visitor.visitBoolean("syncChunkWrites", this.syncChunkWrites);
      visitor.accept("showAutosaveIndicator", this.showAutosaveIndicator);
      visitor.accept("allowServerListing", this.allowServerListing);
      visitor.accept("onlyShowSecureChat", this.onlyShowSecureChat);
      visitor.accept("panoramaScrollSpeed", this.panoramaSpeed);
      visitor.accept("telemetryOptInExtra", this.telemetryOptInExtra);
      this.onboardAccessibility = visitor.visitBoolean("onboardAccessibility", this.onboardAccessibility);
      visitor.accept("menuBackgroundBlurriness", this.menuBackgroundBlurriness);

      for (KeyBinding keyBinding : this.allKeys) {
         String string = keyBinding.getBoundKeyTranslationKey();
         String string2 = visitor.visitString("key_" + keyBinding.getTranslationKey(), string);
         if (!string.equals(string2)) {
            keyBinding.setBoundKey(InputUtil.fromTranslationKey(string2));
         }
      }

      for (SoundCategory soundCategory : SoundCategory.values()) {
         visitor.accept("soundCategory_" + soundCategory.getName(), this.soundVolumeLevels.get(soundCategory));
      }

      for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
         boolean bl = this.enabledPlayerModelParts.contains(playerModelPart);
         boolean bl2 = visitor.visitBoolean("modelPart_" + playerModelPart.getName(), bl);
         if (bl2 != bl) {
            this.setPlayerModelPart(playerModelPart, bl2);
         }
      }
   }

   public void load() {
      try {
         if (!this.optionsFile.exists()) {
            Locale locale = Locale.getDefault();
            String systemLanguage = locale.getLanguage().toLowerCase(Locale.ROOT);
            if (!locale.getCountry().isEmpty()) {
               systemLanguage += "_" + locale.getCountry().toLowerCase(Locale.ROOT);
            }
            this.language = systemLanguage;
            return;
         }

         NbtCompound nbtCompound = new NbtCompound();

         try (BufferedReader bufferedReader = Files.newReader(this.optionsFile, Charsets.UTF_8)) {
            bufferedReader.lines().forEach(line -> {
               try {
                  Iterator<String> iterator = COLON_SPLITTER.split(line).iterator();
                  nbtCompound.putString(iterator.next(), iterator.next());
               } catch (Exception exceptionx) {
                  LOGGER.warn("Skipping bad option: {}", line);
               }
            });
         }

         final NbtCompound nbtCompound2 = this.update(nbtCompound);
         if (!nbtCompound2.contains("graphicsMode") && nbtCompound2.contains("fancyGraphics")) {
            if (isTrue(nbtCompound2.getString("fancyGraphics"))) {
               this.graphicsMode.setValue(GraphicsMode.FANCY);
            } else {
               this.graphicsMode.setValue(GraphicsMode.FAST);
            }
         }

         this.accept(
            new GameOptions.Visitor() {
               @Nullable
               private String find(String key) {
                  return nbtCompound2.contains(key) ? nbtCompound2.get(key).asString() : null;
               }

               @Override
               public <T> void accept(String key, SimpleOption<T> option) {
                  String string = this.find(key);
                  if (string != null) {
                     JsonReader jsonReader = new JsonReader(new StringReader(string.isEmpty() ? "\"\"" : string));
                     JsonElement jsonElement = JsonParser.parseReader(jsonReader);
                     DataResult<T> dataResult = option.getCodec().parse(JsonOps.INSTANCE, jsonElement);
                     dataResult.error()
                        .ifPresent(error -> GameOptions.LOGGER.error("Error parsing option value " + string + " for option " + option + ": " + error.message()));
                     dataResult.ifSuccess(option::setValue);
                  }
               }

               @Override
               public int visitInt(String key, int current) {
                  String string = this.find(key);
                  if (string != null) {
                     try {
                        return Integer.parseInt(string);
                     } catch (NumberFormatException numberFormatException) {
                        GameOptions.LOGGER.warn("Invalid integer value for option {} = {}", new Object[]{key, string, numberFormatException});
                     }
                  }

                  return current;
               }

               @Override
               public boolean visitBoolean(String key, boolean current) {
                  String string = this.find(key);
                  return string != null ? GameOptions.isTrue(string) : current;
               }

               @Override
               public String visitString(String key, String current) {
                  return (String)MoreObjects.firstNonNull(this.find(key), current);
               }

               @Override
               public float visitFloat(String key, float current) {
                  String string = this.find(key);
                  if (string == null) {
                     return current;
                  }

                  if (GameOptions.isTrue(string)) {
                     return 1.0F;
                  }

                  if (GameOptions.isFalse(string)) {
                     return 0.0F;
                  }

                  try {
                     return Float.parseFloat(string);
                  } catch (NumberFormatException numberFormatException) {
                     GameOptions.LOGGER.warn("Invalid floating point value for option {} = {}", new Object[]{key, string, numberFormatException});
                     return current;
                  }
               }

               @Override
               public <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder) {
                  String string = this.find(key);
                  return string == null ? current : decoder.apply(string);
               }
            }
         );
         if (nbtCompound2.contains("fullscreenResolution")) {
            this.fullscreenResolution = nbtCompound2.getString("fullscreenResolution");
         }

         KeyBinding.updateKeysByCode();
      } catch (Exception exception) {
         LOGGER.error("Failed to load options", exception);
      }
   }

   static boolean isTrue(String value) {
      return "true".equals(value);
   }

   static boolean isFalse(String value) {
      return "false".equals(value);
   }

   private NbtCompound update(NbtCompound nbt) {
      int i = 0;

      try {
         i = Integer.parseInt(nbt.getString("version"));
      } catch (RuntimeException var4) {
      }

      return DataFixTypes.OPTIONS.update(this.client.getDataFixer(), nbt, i);
   }

   public void write() {
      try (final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8))) {
         printWriter.println("version:" + SharedConstants.getGameVersion().getSaveVersion().getId());
         this.accept(
            new GameOptions.Visitor() {
               public void print(String key) {
                  printWriter.print(key);
                  printWriter.print(':');
               }

               @Override
               public <T> void accept(String key, SimpleOption<T> option) {
                  option.getCodec()
                     .encodeStart(JsonOps.INSTANCE, option.getValue())
                     .ifError(error -> GameOptions.LOGGER.error("Error saving option " + option + ": " + error))
                     .ifSuccess(json -> {
                        this.print(key);
                        printWriter.println(GameOptions.GSON.toJson(json));
                     });
               }

               @Override
               public int visitInt(String key, int current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               @Override
               public boolean visitBoolean(String key, boolean current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               @Override
               public String visitString(String key, String current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               @Override
               public float visitFloat(String key, float current) {
                  this.print(key);
                  printWriter.println(current);
                  return current;
               }

               @Override
               public <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder) {
                  this.print(key);
                  printWriter.println(encoder.apply(current));
                  return current;
               }
            }
         );
         if (this.client.getWindow().getFullscreenVideoMode().isPresent()) {
            printWriter.println("fullscreenResolution:" + this.client.getWindow().getFullscreenVideoMode().get().asString());
         }
      } catch (Exception exception) {
         LOGGER.error("Failed to save options", exception);
      }

      this.sendClientSettings();
   }

   public SyncedClientOptions getSyncedOptions() {
      int i = 0;

      for (PlayerModelPart playerModelPart : this.enabledPlayerModelParts) {
         i |= playerModelPart.getBitFlag();
      }

      return new SyncedClientOptions(
         this.language,
         this.viewDistance.getValue(),
         this.chatVisibility.getValue(),
         this.chatColors.getValue(),
         i,
         this.mainArm.getValue(),
         this.client.shouldFilterText(),
         this.allowServerListing.getValue(),
         this.particles.getValue()
      );
   }

   public void sendClientSettings() {
      if (this.client.player != null) {
         this.client.player.networkHandler.syncOptions(this.getSyncedOptions());
      }
   }

   public void setPlayerModelPart(PlayerModelPart part, boolean enabled) {
      if (enabled) {
         this.enabledPlayerModelParts.add(part);
      } else {
         this.enabledPlayerModelParts.remove(part);
      }
   }

   public boolean isPlayerModelPartEnabled(PlayerModelPart part) {
      return this.enabledPlayerModelParts.contains(part);
   }

   public CloudRenderMode getCloudRenderModeValue() {
      if (this.getClampedViewDistance() < 4) {
         return CloudRenderMode.OFF;
      }

      return Iris.getPipelineManager().getPipeline().map(pipeline -> switch (pipeline.getCloudSetting()) {
         case OFF -> CloudRenderMode.OFF;
         case FAST -> CloudRenderMode.FAST;
         case FANCY -> CloudRenderMode.FANCY;
         default -> this.cloudRenderMode.getValue();
      }).orElseGet(this.cloudRenderMode::getValue);
   }

   public boolean shouldUseNativeTransport() {
      if (!this.useNativeTransport) {
         ViaFabricPlusImpl.INSTANCE
            .logger()
            .error("Native transport is disabled, but ViaFabricPlus requires it to distinguish server pings from connections; enabling it.");
      }
      return true;
   }

   public void addResourcePackProfilesToManager(ResourcePackManager manager) {
      Set<String> set = Sets.newLinkedHashSet();
      Iterator<String> iterator = this.resourcePacks.iterator();

      while (iterator.hasNext()) {
         String string = iterator.next();
         ResourcePackProfile resourcePackProfile = manager.getProfile(string);
         if (resourcePackProfile == null && !string.startsWith("file/")) {
            resourcePackProfile = manager.getProfile("file/" + string);
         }

         if (resourcePackProfile == null) {
            LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", string);
            iterator.remove();
         } else if (!resourcePackProfile.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(string)) {
            LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", string);
            iterator.remove();
         } else if (resourcePackProfile.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(string)) {
            LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", string);
            this.incompatibleResourcePacks.remove(string);
         } else {
            set.add(resourcePackProfile.getId());
         }
      }

      manager.setEnabledProfiles(set);
   }

   public Perspective getPerspective() {
      return this.perspective;
   }

   public void setPerspective(Perspective perspective) {
      this.perspective = perspective;
   }

   private static List<String> parseList(String content) {
      List<String> list = (List<String>)JsonHelper.deserialize(GSON, content, STRING_LIST_TYPE);
      return list != null ? list : Lists.newArrayList();
   }

   public File getOptionsFile() {
      return this.optionsFile;
   }

   public String collectProfiledOptions() {
      final List<Pair<String, Object>> list = new ArrayList<>();
      this.acceptProfiledOptions(new GameOptions.OptionVisitor() {
         @Override
         public <T> void accept(String key, SimpleOption<T> option) {
            list.add(Pair.of(key, option.getValue()));
         }
      });
      list.add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenResolution)));
      list.add(Pair.of("glDebugVerbosity", this.glDebugVerbosity));
      list.add(Pair.of("overrideHeight", this.overrideHeight));
      list.add(Pair.of("overrideWidth", this.overrideWidth));
      list.add(Pair.of("syncChunkWrites", this.syncChunkWrites));
      list.add(Pair.of("useNativeTransport", this.useNativeTransport));
      list.add(Pair.of("resourcePacks", this.resourcePacks));
      return list.stream()
         .sorted(Comparator.comparing(Pair::getFirst))
         .map(option -> (String)option.getFirst() + ": " + option.getSecond())
         .collect(Collectors.joining(System.lineSeparator()));
   }

   public void setServerViewDistance(int serverViewDistance) {
      this.serverViewDistance = ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_17_1) ? 0 : serverViewDistance;
   }

   public int getClampedViewDistance() {
      return this.serverViewDistance > 0 ? Math.min(this.viewDistance.getValue(), this.serverViewDistance) : this.viewDistance.getValue();
   }

   private static Text getPixelValueText(Text prefix, int value) {
      return Text.translatable("options.pixel_value", new Object[]{prefix, value});
   }

   private static Text getPercentValueText(Text prefix, double value) {
      return Text.translatable("options.percent_value", new Object[]{prefix, (int)(value * 100.0)});
   }

   public static Text getGenericValueText(Text prefix, Text value) {
      return Text.translatable("options.generic_value", new Object[]{prefix, value});
   }

   public static Text getGenericValueText(Text prefix, int value) {
      return getGenericValueText(prefix, Text.literal(Integer.toString(value)));
   }

   public static Text getGenericValueOrOffText(Text prefix, int value) {
      return value == 0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getGenericValueText(prefix, value);
   }

   private static Text getPercentValueOrOffText(Text prefix, double value) {
      return value == 0.0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getPercentValueText(prefix, value);
   }

   interface OptionVisitor {
      <T> void accept(String key, SimpleOption<T> option);
   }

   public interface Visitor extends GameOptions.OptionVisitor {
      int visitInt(String key, int current);

      boolean visitBoolean(String key, boolean current);

      String visitString(String key, String current);

      float visitFloat(String key, float current);

      <T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder);
   }
}
