package net.minecraft.client.gui.hud;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlDebugInfo;
import com.mojang.datafixers.DataFixUtils;
import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.protocol.storage.BedrockJoinGameTracker;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import dev.tr7zw.entityculling.EntityCullingManager;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import me.flashyreese.mods.sodiumextra.client.SodiumExtraClientMod;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.irisshaders.batchedentityrendering.impl.BatchingDebugMessageHelper;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.debug.PacketSizeChart;
import net.minecraft.client.gui.hud.debug.PieChart;
import net.minecraft.client.gui.hud.debug.PingChart;
import net.minecraft.client.gui.hud.debug.RenderingChart;
import net.minecraft.client.gui.hud.debug.TickChart;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import net.minecraft.util.profiler.ServerTickType;
import net.minecraft.util.profiler.log.DebugSampleType;
import net.minecraft.world.LightType;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.SpawnHelper.Info;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.tick.TickManager;
import org.jetbrains.annotations.Nullable;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ServerAuthMovementMode;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.GameSessionStorage;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.classic.c0_30cpetoc0_28_30.storage.ExtensionProtocolMetadataStorage;
import net.raphimc.vialegacy.protocol.release.r1_1tor1_2_1_3.storage.SeedStorage;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.storage.EntityTracker;
import org.cloudburstmc.netty.channel.raknet.RakClientChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;

public class DebugHud {
   private static final BufferPoolMXBean IRIS_DIRECT_BUFFER_POOL = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
      .stream()
      .filter(pool -> pool.getName().equals("direct"))
      .findFirst()
      .orElseThrow();
   private final List<String> sodiumExtraLeftTextCache = new ArrayList<>();
   private final List<String> sodiumExtraRightTextCache = new ArrayList<>();
   private long sodiumExtraNextRefreshTime;
   private boolean sodiumExtraRebuildText = true;
   private static final int TEXT_COLOR = 14737632;
   private static final int field_32188 = 2;
   private static final int field_32189 = 2;
   private static final int field_32190 = 2;
   private static final Map<Type, String> HEIGHT_MAP_TYPES = (Map<Type, String>)Util.make(new EnumMap(Type.class), types -> {
      types.put(Type.WORLD_SURFACE_WG, "SW");
      types.put(Type.WORLD_SURFACE, "S");
      types.put(Type.OCEAN_FLOOR_WG, "OW");
      types.put(Type.OCEAN_FLOOR, "O");
      types.put(Type.MOTION_BLOCKING, "M");
      types.put(Type.MOTION_BLOCKING_NO_LEAVES, "ML");
   });
   private final MinecraftClient client;
   private final DebugHud.AllocationRateCalculator allocationRateCalculator;
   private final TextRenderer textRenderer;
   private HitResult blockHit;
   private HitResult fluidHit;
   @Nullable
   private ChunkPos pos;
   @Nullable
   private WorldChunk chunk;
   @Nullable
   private CompletableFuture<WorldChunk> chunkFuture;
   private boolean showDebugHud;
   private boolean renderingChartVisible;
   private boolean renderingAndTickChartsVisible;
   private boolean packetSizeAndPingChartsVisible;
   private final MultiValueDebugSampleLogImpl frameNanosLog = new MultiValueDebugSampleLogImpl(1);
   private final MultiValueDebugSampleLogImpl tickNanosLog = new MultiValueDebugSampleLogImpl(ServerTickType.values().length);
   private final MultiValueDebugSampleLogImpl pingLog = new MultiValueDebugSampleLogImpl(1);
   private final MultiValueDebugSampleLogImpl packetSizeLog = new MultiValueDebugSampleLogImpl(1);
   private final Map<DebugSampleType, MultiValueDebugSampleLogImpl> receivedDebugSamples = Map.of(DebugSampleType.TICK_TIME, this.tickNanosLog);
   private final RenderingChart renderingChart;
   private final TickChart tickChart;
   private final PingChart pingChart;
   private final PacketSizeChart packetSizeChart;
   private final PieChart pieChart;

   public DebugHud(MinecraftClient client) {
      this.client = client;
      this.allocationRateCalculator = new DebugHud.AllocationRateCalculator();
      this.textRenderer = client.textRenderer;
      this.renderingChart = new RenderingChart(this.textRenderer, this.frameNanosLog);
      this.tickChart = new TickChart(this.textRenderer, this.tickNanosLog, () -> client.world.getTickManager().getMillisPerTick());
      this.pingChart = new PingChart(this.textRenderer, this.pingLog);
      this.packetSizeChart = new PacketSizeChart(this.textRenderer, this.packetSizeLog);
      this.pieChart = new PieChart(this.textRenderer);
   }

   public void resetChunk() {
      this.chunkFuture = null;
      this.chunk = null;
   }

   public void render(DrawContext context) {
      if (SodiumExtraClientMod.isMixinEnabled("steady_debug_hud.MixinDebugScreenOverlay")
         && SodiumExtraClientMod.options().extraSettings.steadyDebugHud) {
         long currentTime = Util.getMeasuringTimeMs();
         this.sodiumExtraRebuildText = currentTime > this.sodiumExtraNextRefreshTime;
         if (this.sodiumExtraRebuildText) {
            this.sodiumExtraNextRefreshTime = currentTime + SodiumExtraClientMod.options().extraSettings.steadyDebugHudRefreshInterval * 50L;
         }
      } else {
         this.sodiumExtraRebuildText = true;
      }

      Profiler profiler = Profilers.get();
      profiler.push("debug");
      Entity entity = this.client.getCameraEntity();
      this.blockHit = entity.raycast(20.0, 0.0F, false);
      this.fluidHit = entity.raycast(20.0, 0.0F, true);
      this.drawLeftText(context);
      this.drawRightText(context);
      this.pieChart.setBottomMargin(10);
      if (this.renderingAndTickChartsVisible) {
         int i = context.getScaledWindowWidth();
         int j = i / 2;
         this.renderingChart.render(context, 0, this.renderingChart.getWidth(j));
         if (this.tickNanosLog.getLength() > 0) {
            int k = this.tickChart.getWidth(j);
            this.tickChart.render(context, i - k, k);
         }

         this.pieChart.setBottomMargin(this.tickChart.getHeight());
      }

      if (this.packetSizeAndPingChartsVisible) {
         int i = context.getScaledWindowWidth();
         int j = i / 2;
         if (!this.client.isInSingleplayer()) {
            this.packetSizeChart.render(context, 0, this.packetSizeChart.getWidth(j));
         }

         int k = this.pingChart.getWidth(j);
         this.pingChart.render(context, i - k, k);
         this.pieChart.setBottomMargin(this.pingChart.getHeight());
      }

      ScopedProfiler scopedProfiler = profiler.scoped("profilerPie");

      try {
         this.pieChart.render(context);
      } catch (Throwable var8) {
         if (scopedProfiler != null) {
            try {
               scopedProfiler.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (scopedProfiler != null) {
         scopedProfiler.close();
      }

      profiler.pop();
   }

   protected void drawLeftText(DrawContext context) {
      if (this.sodiumExtraRebuildText) {
         List<String> list = this.getLeftText();
         list.add("");
         boolean bl = this.client.getServer() != null;
         list.add(
            "Debug charts: [F3+1] Profiler "
               + (this.renderingChartVisible ? "visible" : "hidden")
               + "; [F3+2] "
               + (bl ? "FPS + TPS " : "FPS ")
               + (this.renderingAndTickChartsVisible ? "visible" : "hidden")
               + "; [F3+3] "
               + (!this.client.isInSingleplayer() ? "Bandwidth + Ping" : "Ping")
               + (this.packetSizeAndPingChartsVisible ? " visible" : " hidden")
         );
         list.add("For help: press F3 + Q");
         this.sodiumExtraLeftTextCache.clear();
         this.sodiumExtraLeftTextCache.addAll(list);
      }

      this.drawText(context, this.sodiumExtraLeftTextCache, true);
   }

   protected void drawRightText(DrawContext context) {
      if (this.sodiumExtraRebuildText) {
         this.sodiumExtraRightTextCache.clear();
         this.sodiumExtraRightTextCache.addAll(this.getRightText());
      }

      this.drawText(context, this.sodiumExtraRightTextCache, false);
   }

   private void drawText(DrawContext context, List<String> text, boolean left) {
      int i = 9;

      for (int j = 0; j < text.size(); j++) {
         String string = text.get(j);
         if (!Strings.isNullOrEmpty(string)) {
            int k = this.textRenderer.getWidth(string);
            int l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
            int m = 2 + i * j;
            context.fill(l - 1, m - 1, l + k + 1, m + i - 1, -1873784752);
         }
      }

      for (int j = 0; j < text.size(); j++) {
         String string = text.get(j);
         if (!Strings.isNullOrEmpty(string)) {
            int k = this.textRenderer.getWidth(string);
            int l = left ? 2 : context.getScaledWindowWidth() - 2 - k;
            int m = 2 + i * j;
            context.drawText(this.textRenderer, string, l, m, 14737632, false);
         }
      }
   }

   protected List<String> getLeftText() {
      IntegratedServer integratedServer = this.client.getServer();
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
      ClientConnection clientConnection = clientPlayNetworkHandler.getConnection();
      float f = clientConnection.getAveragePacketsSent();
      float g = clientConnection.getAveragePacketsReceived();
      TickManager tickManager = this.getWorld().getTickManager();
      String string;
      if (tickManager.isStepping()) {
         string = " (frozen - stepping)";
      } else if (tickManager.isFrozen()) {
         string = " (frozen)";
      } else {
         string = "";
      }

      String string3;
      if (integratedServer != null) {
         ServerTickManager serverTickManager = integratedServer.getTickManager();
         boolean bl = serverTickManager.isSprinting();
         if (bl) {
            string = " (sprinting)";
         }

         String string2 = bl ? "-" : String.format(Locale.ROOT, "%.1f", tickManager.getMillisPerTick());
         string3 = String.format(
            Locale.ROOT, "Integrated server @ %.1f/%s ms%s, %.0f tx, %.0f rx", integratedServer.getAverageTickTime(), string2, string, f, g
         );
      } else {
         string3 = String.format(Locale.ROOT, "\"%s\" server%s, %.0f tx, %.0f rx", clientPlayNetworkHandler.getBrand(), string, f, g);
      }

      BlockPos blockPos = this.client.getCameraEntity().getBlockPos();
      if (this.client.hasReducedDebugInfo()) {
         return this.iris$appendShaderPackText(this.viaFabricPlus$appendInformation(Lists.newArrayList(
            new String[]{
               "Minecraft "
                  + SharedConstants.getGameVersion().getName()
                  + " ("
                  + this.client.getGameVersion()
                  + "/"
                  + ClientBrandRetriever.getClientModName()
                  + ")",
               this.client.fpsDebugString,
               string3,
               this.client.worldRenderer.getChunksDebugString(),
               this.client.worldRenderer.getEntitiesDebugString(),
               "P: " + this.client.particleManager.getDebugString() + ". T: " + this.client.world.getRegularEntityCount(),
               this.client.world.asString(),
               "",
               String.format(Locale.ROOT, "Chunk-relative: %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15)
            }
         )));
      }

      Entity entity = this.client.getCameraEntity();
      Direction direction = entity.getHorizontalFacing();

      String string4 = switch (direction) {
         case NORTH -> "Towards negative Z";
         case SOUTH -> "Towards positive Z";
         case WEST -> "Towards negative X";
         case EAST -> "Towards positive X";
         default -> "Invalid";
      };
      ChunkPos chunkPos = new ChunkPos(blockPos);
      if (!Objects.equals(this.pos, chunkPos)) {
         this.pos = chunkPos;
         this.resetChunk();
      }

      World world = this.getWorld();
      LongSet longSet = (LongSet)(world instanceof ServerWorld ? ((ServerWorld)world).getForcedChunks() : LongSets.EMPTY_SET);
      List<String> list = Lists.newArrayList(
         new String[]{
            "Minecraft "
               + SharedConstants.getGameVersion().getName()
               + " ("
               + this.client.getGameVersion()
               + "/"
               + ClientBrandRetriever.getClientModName()
               + ("release".equalsIgnoreCase(this.client.getVersionType()) ? "" : "/" + this.client.getVersionType())
               + ")",
            this.client.fpsDebugString,
            string3,
            this.client.worldRenderer.getChunksDebugString(),
            this.client.worldRenderer.getEntitiesDebugString(),
            "P: " + this.client.particleManager.getDebugString() + ". T: " + this.client.world.getRegularEntityCount(),
            this.client.world.asString()
         }
      );
      String string5 = this.getServerWorldDebugString();
      if (string5 != null) {
         list.add(string5);
      }

      list.add(this.client.world.getRegistryKey().getValue() + " FC: " + longSet.size());
      list.add("");
      list.add(
         String.format(
            Locale.ROOT,
            "XYZ: %.3f / %.5f / %.3f",
            this.client.getCameraEntity().getX(),
            this.client.getCameraEntity().getY(),
            this.client.getCameraEntity().getZ()
         )
      );
      list.add(
         String.format(
            Locale.ROOT,
            "Block: %d %d %d [%d %d %d]",
            blockPos.getX(),
            blockPos.getY(),
            blockPos.getZ(),
            blockPos.getX() & 15,
            blockPos.getY() & 15,
            blockPos.getZ() & 15
         )
      );
      list.add(
         String.format(
            Locale.ROOT,
            "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
            chunkPos.x,
            ChunkSectionPos.getSectionCoord(blockPos.getY()),
            chunkPos.z,
            chunkPos.getRegionRelativeX(),
            chunkPos.getRegionRelativeZ(),
            chunkPos.getRegionX(),
            chunkPos.getRegionZ()
         )
      );
      list.add(
         String.format(
            Locale.ROOT,
            "Facing: %s (%s) (%.1f / %.1f)",
            direction,
            string4,
            MathHelper.wrapDegrees(entity.getYaw()),
            MathHelper.wrapDegrees(entity.getPitch())
         )
      );
      WorldChunk worldChunk = this.getClientChunk();
      if (worldChunk.isEmpty()) {
         list.add("Waiting for chunk...");
      } else {
         int i = this.client.world.getChunkManager().getLightingProvider().getLight(blockPos, 0);
         int j = this.client.world.getLightLevel(LightType.SKY, blockPos);
         int k = this.client.world.getLightLevel(LightType.BLOCK, blockPos);
         list.add("Client Light: " + i + " (" + j + " sky, " + k + " block)");
         WorldChunk worldChunk2 = this.getChunk();
         StringBuilder stringBuilder = new StringBuilder("CH");

         for (Type type : Type.values()) {
            if (type.shouldSendToClient()) {
               stringBuilder.append(" ")
                  .append(HEIGHT_MAP_TYPES.get(type))
                  .append(": ")
                  .append(worldChunk.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
            }
         }

         list.add(stringBuilder.toString());
         stringBuilder.setLength(0);
         stringBuilder.append("SH");

         for (Type type : Type.values()) {
            if (type.isStoredServerSide()) {
               stringBuilder.append(" ").append(HEIGHT_MAP_TYPES.get(type)).append(": ");
               if (worldChunk2 != null) {
                  stringBuilder.append(worldChunk2.sampleHeightmap(type, blockPos.getX(), blockPos.getZ()));
               } else {
                  stringBuilder.append("??");
               }
            }
         }

         list.add(stringBuilder.toString());
         if (this.client.world.isInHeightLimit(blockPos.getY())) {
            list.add("Biome: " + getBiomeString(this.client.world.getBiome(blockPos)));
            if (worldChunk2 != null) {
               float h = world.getMoonSize();
               long l = worldChunk2.getInhabitedTime();
               LocalDifficulty localDifficulty = new LocalDifficulty(world.getDifficulty(), world.getTimeOfDay(), l, h);
               list.add(
                  String.format(
                     Locale.ROOT,
                     "Local Difficulty: %.2f // %.2f (Day %d)",
                     localDifficulty.getLocalDifficulty(),
                     localDifficulty.getClampedLocalDifficulty(),
                     this.client.world.getTimeOfDay() / 24000L
                  )
               );
            } else {
               list.add("Local Difficulty: ??");
            }
         }

         if (worldChunk2 != null && worldChunk2.usesOldNoise()) {
            list.add("Blending: Old");
         }
      }

      ServerWorld serverWorld = this.getServerWorld();
      if (serverWorld != null) {
         ServerChunkManager serverChunkManager = serverWorld.getChunkManager();
         ChunkGenerator chunkGenerator = serverChunkManager.getChunkGenerator();
         NoiseConfig noiseConfig = serverChunkManager.getNoiseConfig();
         chunkGenerator.appendDebugHudText(list, noiseConfig, blockPos);
         MultiNoiseSampler multiNoiseSampler = noiseConfig.getMultiNoiseSampler();
         BiomeSource biomeSource = chunkGenerator.getBiomeSource();
         biomeSource.addDebugInfo(list, blockPos, multiNoiseSampler);
         Info info = serverChunkManager.getSpawnInfo();
         if (info != null) {
            Object2IntMap<SpawnGroup> object2IntMap = info.getGroupToCount();
            int m = info.getSpawningChunkCount();
            list.add(
               "SC: "
                  + m
                  + ", "
                  + Stream.of(SpawnGroup.values())
                     .map(group -> Character.toUpperCase(group.getName().charAt(0)) + ": " + object2IntMap.getInt(group))
                     .collect(Collectors.joining(", "))
            );
         } else {
            list.add("SC: N/A");
         }
      }

      Identifier identifier = this.client.gameRenderer.getPostProcessorId();
      if (identifier != null) {
         list.add("Post: " + identifier);
      }

      list.add(
         this.client.getSoundManager().getDebugString()
            + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(this.client.player.getMoodPercentage() * 100.0F))
      );
      list.add(EntityCullingManager.getInstance().getDebugStringAndReset());
      return this.iris$appendShaderPackText(this.viaFabricPlus$appendInformation(list));
   }

   private List<String> iris$appendShaderPackText(List<String> messages) {
      messages.add(3, "Direct Buffers: +" + iris$humanReadableByteCountBin(IRIS_DIRECT_BUFFER_POOL.getMemoryUsed()));
      messages.add("");
      messages.add("[Iris] Version: " + Iris.getFormattedVersion());
      messages.add("");
      if (Iris.getIrisConfig().areShadersEnabled()) {
         messages.add("[Iris] Shaderpack: " + Iris.getCurrentPackName() + (Iris.isFallback() ? " (fallback)" : ""));
         Iris.getCurrentPack().ifPresent(pack -> messages.add("[Iris] " + pack.getProfileInfo()));
         messages.add("[Iris] Color space: " + IrisVideoSettings.colorSpace.name());
      } else {
         messages.add("[Iris] Shaders are disabled");
      }

      return messages;
   }

   private static String iris$humanReadableByteCountBin(long bytes) {
      long absolute = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
      if (absolute < 1024L) {
         return bytes + " B";
      }

      long value = absolute;
      CharacterIterator units = new StringCharacterIterator("KMGTPE");
      for (int shift = 40; shift >= 0 && absolute > 1152865209611504844L >> shift; shift -= 10) {
         value >>= 10;
         units.next();
      }

      value *= Long.signum(bytes);
      return String.format("%.3f %ciB", value / 1024.0, units.current());
   }

   private List<String> viaFabricPlus$appendInformation(List<String> lines) {
      if (!(Boolean)GeneralSettings.INSTANCE.showExtraInformationInDebugHud.getValue()) {
         return lines;
      }

      List<String> information = new ArrayList<>();
      information.add("");
      information.add(ChatUtil.PREFIX + Formatting.RESET + " " + ViaFabricPlus.getImpl().getVersion());
      UserConnection connection = ProtocolTranslator.getPlayNetworkUserConnection();
      if (connection == null) {
         lines.addAll(information);
         return lines;
      }

      ProtocolInfo info = connection.getProtocolInfo();
      information.add("P: " + info.getPipeline().pipes().size() + " C: " + info.protocolVersion() + " S: " + info.serverProtocolVersion());
      EntityTracker entityTracker = connection.get(EntityTracker.class);
      if (entityTracker != null) {
         information.add(
            "1.7 Entities: "
               + entityTracker.getTrackedEntities().size()
               + ", Virtual holograms: "
               + entityTracker.getVirtualHolograms().size()
         );
      }

      SeedStorage seedStorage = connection.get(SeedStorage.class);
      if (seedStorage != null && info.serverProtocolVersion().newerThanOrEqualTo(LegacyProtocolVersion.a1_2_0toa1_2_1_1)) {
         information.add("World Seed: " + seedStorage.seed);
      }

      ExtensionProtocolMetadataStorage extensionStorage = connection.get(ExtensionProtocolMetadataStorage.class);
      if (extensionStorage != null) {
         information.add("CPE extensions: " + extensionStorage.getExtensionCount());
      }

      BedrockJoinGameTracker joinTracker = connection.get(BedrockJoinGameTracker.class);
      if (joinTracker != null) {
         GameSessionStorage gameSession = connection.get(GameSessionStorage.class);
         ServerAuthMovementMode movementMode = gameSession == null ? null : gameSession.getMovementMode();
         information.add(
            "Bedrock Level: "
               + joinTracker.getLevelId()
               + ", Enchantment Seed: "
               + joinTracker.getEnchantmentSeed()
               + ", Movement: "
               + (movementMode == null ? "unknown" : movementMode.name())
         );
         information.add("World Seed: " + joinTracker.getSeed());
      }

      ChunkTracker chunkTracker = connection.get(ChunkTracker.class);
      if (chunkTracker != null) {
         int requests = viaFabricPlus$getContainerSize(chunkTracker, "subChunkRequests");
         int pending = viaFabricPlus$getContainerSize(chunkTracker, "pendingSubChunks");
         int chunks = viaFabricPlus$getContainerSize(chunkTracker, "chunks");
         if (requests >= 0 && pending >= 0 && chunks >= 0) {
            information.add("Chunk Tracker: R: " + requests + ", P: " + pending + ", C: " + chunks);
         }
      }

      if (connection.getChannel() instanceof RakClientChannel rakClientChannel) {
         RakSessionCodec rakSessionCodec = rakClientChannel.parent().pipeline().get(RakSessionCodec.class);
         if (rakSessionCodec != null) {
            int outgoing = viaFabricPlus$getContainerSize(rakSessionCodec, "outgoingPackets");
            int sent = viaFabricPlus$getContainerSize(rakSessionCodec, "sentDatagrams");
            information.add(
               "RTT: "
                  + Math.round(rakSessionCodec.getRTT())
                  + " ms, P: "
                  + rakSessionCodec.getPing()
                  + " ms, TQ: "
                  + outgoing
                  + ", RTQ: "
                  + sent
            );
         }
      }

      lines.addAll(information);
      return lines;
   }

   private static int viaFabricPlus$getContainerSize(Object owner, String fieldName) {
      try {
         Class<?> type = owner.getClass();
         Field field = null;
         while (type != null && field == null) {
            try {
               field = type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
               type = type.getSuperclass();
            }
         }
         if (field == null) {
            return -1;
         }
         field.setAccessible(true);
         Object value = field.get(owner);
         if (value instanceof Collection<?> collection) {
            return collection.size();
         }
         if (value instanceof Map<?, ?> map) {
            return map.size();
         }
         Method size = value.getClass().getMethod("size");
         return ((Number)size.invoke(value)).intValue();
      } catch (ReflectiveOperationException | RuntimeException ignored) {
         return -1;
      }
   }

   private static String getBiomeString(RegistryEntry<Biome> biome) {
      return (String)biome.getKeyOrValue().map(biomeKey -> biomeKey.getValue().toString(), biome_ -> "[unregistered " + biome_ + "]");
   }

   @Nullable
   private ServerWorld getServerWorld() {
      IntegratedServer integratedServer = this.client.getServer();
      return integratedServer != null ? integratedServer.getWorld(this.client.world.getRegistryKey()) : null;
   }

   @Nullable
   private String getServerWorldDebugString() {
      ServerWorld serverWorld = this.getServerWorld();
      return serverWorld != null ? serverWorld.asString() : null;
   }

   private World getWorld() {
      return (World)DataFixUtils.orElse(
         Optional.ofNullable(this.client.getServer()).flatMap(server -> Optional.ofNullable(server.getWorld(this.client.world.getRegistryKey()))),
         this.client.world
      );
   }

   @Nullable
   private WorldChunk getChunk() {
      if (this.chunkFuture == null) {
         ServerWorld serverWorld = this.getServerWorld();
         if (serverWorld == null) {
            return null;
         }

         this.chunkFuture = serverWorld.getChunkManager()
            .getChunkFutureSyncOnMainThread(this.pos.x, this.pos.z, ChunkStatus.FULL, false)
            .thenApply(optionalChunk -> (WorldChunk)optionalChunk.orElse(null));
      }

      return this.chunkFuture.getNow(null);
   }

   private WorldChunk getClientChunk() {
      if (this.chunk == null) {
         this.chunk = this.client.world.getChunk(this.pos.x, this.pos.z);
      }

      return this.chunk;
   }

   protected List<String> getRightText() {
      long l = Runtime.getRuntime().maxMemory();
      long m = Runtime.getRuntime().totalMemory();
      long n = Runtime.getRuntime().freeMemory();
      long o = m - n;
      List<String> list = Lists.newArrayList(
         new String[]{
            String.format(Locale.ROOT, "Java: %s", System.getProperty("java.version")),
            String.format(Locale.ROOT, "Mem: %2d%% %03d/%03dMB", o * 100L / l, toMiB(o), toMiB(l)),
            String.format(Locale.ROOT, "Allocation rate: %03dMB/s", toMiB(this.allocationRateCalculator.get(o))),
            String.format(Locale.ROOT, "Allocated: %2d%% %03dMB", m * 100L / l, toMiB(m)),
            "",
            String.format(Locale.ROOT, "CPU: %s", GlDebugInfo.getCpuInfo()),
            "",
            String.format(
               Locale.ROOT,
               "Display: %dx%d (%s)",
               MinecraftClient.getInstance().getWindow().getFramebufferWidth(),
               MinecraftClient.getInstance().getWindow().getFramebufferHeight(),
               GlDebugInfo.getVendor()
            ),
            GlDebugInfo.getRenderer(),
            GlDebugInfo.getVersion()
         }
      );
      if (this.client.hasReducedDebugInfo()) {
         return this.iris$appendPipelineDebugText(list);
      }

      if (this.blockHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
         BlockPos blockPos = ((BlockHitResult)this.blockHit).getBlockPos();
         BlockState blockState = this.client.world.getBlockState(blockPos);
         list.add("");
         list.add(Formatting.UNDERLINE + "Targeted Block: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
         list.add(String.valueOf(Registries.BLOCK.getId(blockState.getBlock())));

         for (Entry<Property<?>, Comparable<?>> entry : blockState.getEntries().entrySet()) {
            list.add(this.propertyToString(entry));
         }

         blockState.streamTags().map(tag -> "#" + tag.id()).forEach(list::add);
      }

      if (this.fluidHit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
         BlockPos blockPos = ((BlockHitResult)this.fluidHit).getBlockPos();
         FluidState fluidState = this.client.world.getFluidState(blockPos);
         list.add("");
         list.add(Formatting.UNDERLINE + "Targeted Fluid: " + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ());
         list.add(String.valueOf(Registries.FLUID.getId(fluidState.getFluid())));

         for (Entry<Property<?>, Comparable<?>> entry : fluidState.getEntries().entrySet()) {
            list.add(this.propertyToString(entry));
         }

         fluidState.streamTags().map(tag -> "#" + tag.id()).forEach(list::add);
      }

      Entity entity = this.client.targetedEntity;
      if (entity != null) {
         list.add("");
         list.add(Formatting.UNDERLINE + "Targeted Entity");
         list.add(String.valueOf(Registries.ENTITY_TYPE.getId(entity.getType())));
      }

      return this.iris$appendPipelineDebugText(list);
   }

   private List<String> iris$appendPipelineDebugText(List<String> messages) {
      Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.addDebugText(messages));
      if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
         DrawCallTrackingRenderBuffers drawTracker = (DrawCallTrackingRenderBuffers)this.client.getBufferBuilders();
         messages.add("");
         messages.add("[Entity Batching] " + BatchingDebugMessageHelper.getDebugMessage(drawTracker));
      }
      return messages;
   }

   private String propertyToString(Entry<Property<?>, Comparable<?>> propEntry) {
      Property<?> property = propEntry.getKey();
      Comparable<?> comparable = propEntry.getValue();
      String string = Util.getValueAsString(property, comparable);
      if (Boolean.TRUE.equals(comparable)) {
         string = Formatting.GREEN + string;
      } else if (Boolean.FALSE.equals(comparable)) {
         string = Formatting.RED + string;
      }

      return property.getName() + ": " + string;
   }

   private static long toMiB(long bytes) {
      return bytes / 1024L / 1024L;
   }

   public boolean shouldShowDebugHud() {
      return this.showDebugHud && !this.client.options.hudHidden;
   }

   public boolean shouldShowRenderingChart() {
      return this.shouldShowDebugHud() && this.renderingChartVisible;
   }

   public boolean shouldShowPacketSizeAndPingCharts() {
      return this.shouldShowDebugHud() && this.packetSizeAndPingChartsVisible;
   }

   public boolean shouldRenderTickCharts() {
      return this.shouldShowDebugHud() && this.renderingAndTickChartsVisible;
   }

   public void toggleDebugHud() {
      this.showDebugHud = !this.showDebugHud;
   }

   public void togglePacketSizeAndPingCharts() {
      this.packetSizeAndPingChartsVisible = !this.showDebugHud || !this.packetSizeAndPingChartsVisible;
      if (this.packetSizeAndPingChartsVisible) {
         this.showDebugHud = true;
         this.renderingAndTickChartsVisible = false;
      }
   }

   public void toggleRenderingAndTickCharts() {
      this.renderingAndTickChartsVisible = !this.showDebugHud || !this.renderingAndTickChartsVisible;
      if (this.renderingAndTickChartsVisible) {
         this.showDebugHud = true;
         this.packetSizeAndPingChartsVisible = false;
      }
   }

   public void toggleRenderingChart() {
      this.renderingChartVisible = !this.showDebugHud || !this.renderingChartVisible;
      if (this.renderingChartVisible) {
         this.showDebugHud = true;
      }
   }

   public void pushToFrameLog(long value) {
      this.frameNanosLog.push(value);
   }

   public MultiValueDebugSampleLogImpl getTickNanosLog() {
      return this.tickNanosLog;
   }

   public MultiValueDebugSampleLogImpl getPingLog() {
      return this.pingLog;
   }

   public MultiValueDebugSampleLogImpl getPacketSizeLog() {
      return this.packetSizeLog;
   }

   public PieChart getPieChart() {
      return this.pieChart;
   }

   public void set(long[] values, DebugSampleType type) {
      MultiValueDebugSampleLogImpl multiValueDebugSampleLogImpl = this.receivedDebugSamples.get(type);
      if (multiValueDebugSampleLogImpl != null) {
         multiValueDebugSampleLogImpl.set(values);
      }
   }

   public void clear() {
      this.showDebugHud = false;
      this.tickNanosLog.clear();
      this.pingLog.clear();
      this.packetSizeLog.clear();
   }

   static class AllocationRateCalculator {
      private static final int INTERVAL = 500;
      private static final List<GarbageCollectorMXBean> GARBAGE_COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
      private long lastCalculated = 0L;
      private long allocatedBytes = -1L;
      private long collectionCount = -1L;
      private long allocationRate = 0L;

      long get(long allocatedBytes) {
         long l = System.currentTimeMillis();
         if (l - this.lastCalculated < 500L) {
            return this.allocationRate;
         }

         long m = getCollectionCount();
         if (this.lastCalculated != 0L && m == this.collectionCount) {
            double d = (double)TimeUnit.SECONDS.toMillis(1L) / (l - this.lastCalculated);
            long n = allocatedBytes - this.allocatedBytes;
            this.allocationRate = Math.round(n * d);
         }

         this.lastCalculated = l;
         this.allocatedBytes = allocatedBytes;
         this.collectionCount = m;
         return this.allocationRate;
      }

      private static long getCollectionCount() {
         long l = 0L;

         for (GarbageCollectorMXBean garbageCollectorMXBean : GARBAGE_COLLECTORS) {
            l += garbageCollectorMXBean.getCollectionCount();
         }

         return l;
      }
   }
}
