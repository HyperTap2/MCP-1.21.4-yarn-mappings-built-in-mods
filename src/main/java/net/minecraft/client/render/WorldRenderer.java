package net.minecraft.client.render;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderProgramLoader;
import com.github.argon4w.acceleratedrendering.features.filter.FilterFeature;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import dev.tr7zw.entityculling.EntityCullingManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FlawlessFrames;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.VanillaRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shadows.CullingDataCache;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.SimpleFramebufferFactory;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.tick.TickManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.slf4j.Logger;

public class WorldRenderer implements SynchronousResourceReloader, AutoCloseable, LevelRendererExtension, LevelRendererAccessor, CullingDataCache {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Identifier TRANSPARENCY = Identifier.ofVanilla("transparency");
   private static final Identifier ENTITY_OUTLINE = Identifier.ofVanilla("entity_outline");
   public static final int field_32759 = 16;
   public static final int field_34812 = 8;
   public static final int field_54162 = 32;
   private static final int field_54163 = 15;
   private final MinecraftClient client;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   public BufferBuilderStorage bufferBuilders;
   private final SkyRendering skyRendering = new SkyRendering();
   private final CloudRenderer cloudRenderer = new CloudRenderer();
   private final WorldBorderRendering worldBorderRendering = new WorldBorderRendering();
   private final WeatherRendering weatherRendering = new WeatherRendering();
   @Nullable
   private ClientWorld world;
   private final ChunkRenderingDataPreparer chunkRenderingDataPreparer = new ChunkRenderingDataPreparer();
   private ObjectArrayList<ChunkBuilder.BuiltChunk> builtChunks = new ObjectArrayList(10000);
   private ObjectArrayList<ChunkBuilder.BuiltChunk> iris$savedBuiltChunks = new ObjectArrayList(10000);
   private final ObjectArrayList<ChunkBuilder.BuiltChunk> nearbyChunks = new ObjectArrayList(50);
   private final Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
   @Nullable
   private BuiltChunkStorage chunks;
   private int ticks;
   public final Int2ObjectMap<BlockBreakingInfo> blockBreakingInfos = new Int2ObjectOpenHashMap();
   private final Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions = new Long2ObjectOpenHashMap();
   @Nullable
   private Framebuffer entityOutlineFramebuffer;
   private final DefaultFramebufferSet framebufferSet = new DefaultFramebufferSet();
   private int cameraChunkX = Integer.MIN_VALUE;
   private int cameraChunkY = Integer.MIN_VALUE;
   private int cameraChunkZ = Integer.MIN_VALUE;
   private double lastCameraX = Double.MIN_VALUE;
   private double lastCameraY = Double.MIN_VALUE;
   private double lastCameraZ = Double.MIN_VALUE;
   private double lastCameraPitch = Double.MIN_VALUE;
   private double lastCameraYaw = Double.MIN_VALUE;
   private double iris$savedLastCameraPitch = Double.MIN_VALUE;
   private double iris$savedLastCameraYaw = Double.MIN_VALUE;
   @Nullable
   private ChunkBuilder chunkBuilder;
   private int viewDistance = -1;
   private final List<Entity> renderedEntities = new ArrayList<>();
   private int renderedEntitiesCount;
   private Frustum frustum;
   private boolean shouldCaptureFrustum;
   @Nullable
   private Frustum capturedFrustum;
   @Nullable
   private BlockPos prevTranslucencySortCameraPos;
   private int chunkIndex;
   private final SodiumWorldRenderer sodium$renderer;
   private final WorldRenderingPipeline iris$vanillaPipeline = new VanillaRenderingPipeline();
   private WorldRenderingPipeline iris$pipeline = this.iris$vanillaPipeline;
   @Nullable
   private Groupable iris$batchedGroupable;
   private boolean iris$batchedRenderingActive;

   public WorldRenderer(
      MinecraftClient client,
      EntityRenderDispatcher entityRenderDispatcher,
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      BufferBuilderStorage bufferBuilders
   ) {
      this.client = client;
      this.entityRenderDispatcher = entityRenderDispatcher;
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.bufferBuilders = bufferBuilders;
      this.sodium$renderer = new SodiumWorldRenderer(client);
   }

   @Override
   public SodiumWorldRenderer sodium$getWorldRenderer() {
      return this.sodium$renderer;
   }

   @Override
   public EntityRenderDispatcher getEntityRenderDispatcher() {
      return this.entityRenderDispatcher;
   }

   @Override
   public void invokeRenderSectionLayer(RenderLayer layer, double x, double y, double z, Matrix4f modelView, Matrix4f projection) {
      this.renderLayer(layer, x, y, z, modelView, projection);
   }

   @Override
   public void invokeSetupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator) {
      this.setupTerrain(camera, frustum, hasForcedFrustum, spectator);
   }

   @Override
   public void invokeRenderEntity(
      Entity entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers
   ) {
      this.renderEntity(entity, x, y, z, tickDelta, matrices, consumers);
   }

   @Override
   public ClientWorld getLevel() {
      return this.world;
   }

   @Override
   public BufferBuilderStorage getRenderBuffers() {
      return this.bufferBuilders;
   }

   @Override
   public void setRenderBuffers(BufferBuilderStorage buffers) {
      this.bufferBuilders = buffers;
   }

   @Override
   public boolean invokeMethod_43788(Camera camera) {
      return this.hasBlindnessOrDarkness(camera);
   }

   @Override
   public Long2ObjectMap<SortedSet<BlockBreakingInfo>> getField_20950() {
      return this.blockBreakingProgressions;
   }

   @Override
   public void saveState() {
      this.iris$swapCullingState();
   }

   @Override
   public void restoreState() {
      this.iris$swapCullingState();
   }

   private void iris$swapCullingState() {
      ObjectArrayList<ChunkBuilder.BuiltChunk> chunks = this.builtChunks;
      this.builtChunks = this.iris$savedBuiltChunks;
      this.iris$savedBuiltChunks = chunks;
      double pitch = this.lastCameraPitch;
      this.lastCameraPitch = this.iris$savedLastCameraPitch;
      this.iris$savedLastCameraPitch = pitch;
      double yaw = this.lastCameraYaw;
      this.lastCameraYaw = this.iris$savedLastCameraYaw;
      this.iris$savedLastCameraYaw = yaw;
   }

   public void addWeatherParticlesAndSound(Camera camera) {
      this.weatherRendering.addParticlesAndSound(this.client.world, camera, this.ticks, this.client.options.getParticles().getValue());
   }

   @Override
   public void close() {
      if (this.entityOutlineFramebuffer != null) {
         this.entityOutlineFramebuffer.delete();
      }

      this.skyRendering.close();
      this.cloudRenderer.close();
      if (ComputeShaderProgramLoader.isProgramsLoaded()) {
         CoreBuffers.deleteBuffers();
         IrisCompatBuffers.deleteBuffers();
         ComputeShaderProgramLoader.delete();
      }
   }

   public void reload(ResourceManager manager) {
      this.iris$disableFabulousGraphics();
      this.loadEntityOutlinePostProcessor();
   }

   public void loadEntityOutlinePostProcessor() {
      if (this.entityOutlineFramebuffer != null) {
         this.entityOutlineFramebuffer.delete();
      }

      this.entityOutlineFramebuffer = new SimpleFramebuffer(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight(), true);
      this.entityOutlineFramebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
   }

   @Nullable
   private PostEffectProcessor getTransparencyPostEffectProcessor() {
      if (!MinecraftClient.isFabulousGraphicsOrBetter()) {
         return null;
      }

      PostEffectProcessor postEffectProcessor = this.client.getShaderLoader().loadPostEffect(TRANSPARENCY, DefaultFramebufferSet.STAGES);
      if (postEffectProcessor == null) {
         this.client.options.getGraphicsMode().setValue(GraphicsMode.FANCY);
         this.client.options.write();
      }

      return postEffectProcessor;
   }

   public void drawEntityOutlinesFramebuffer() {
      if (this.canDrawEntityOutlines()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(
            GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE
         );
         this.entityOutlineFramebuffer.drawInternal(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   protected boolean canDrawEntityOutlines() {
      return !this.client.gameRenderer.isRenderingPanorama() && this.entityOutlineFramebuffer != null && this.client.player != null;
   }

   public void setWorld(@Nullable ClientWorld world) {
      this.cameraChunkX = Integer.MIN_VALUE;
      this.cameraChunkY = Integer.MIN_VALUE;
      this.cameraChunkZ = Integer.MIN_VALUE;
      this.entityRenderDispatcher.setWorld(world);
      this.world = world;
      RenderDevice.enterManagedCode();
      try {
         this.sodium$renderer.setLevel(world);
      } finally {
         RenderDevice.exitManagedCode();
      }
      if (world != null) {
         this.reload();
      } else {
         if (this.chunks != null) {
            this.chunks.clear();
            this.chunks = null;
         }

         if (this.chunkBuilder != null) {
            this.chunkBuilder.stop();
         }

         this.chunkBuilder = null;
         this.noCullingBlockEntities.clear();
         this.chunkRenderingDataPreparer.setStorage(null);
         this.clear();
      }
   }

   private void clear() {
      this.builtChunks.clear();
      this.nearbyChunks.clear();
   }

   public void reload() {
      this.iris$disableFabulousGraphics();
      if (this.world != null) {
         this.world.reloadColor();
         if (this.chunkBuilder == null) {
            this.chunkBuilder = new ChunkBuilder(
               this.world,
               this,
               Util.getMainWorkerExecutor(),
               this.bufferBuilders,
               this.client.getBlockRenderManager(),
               this.client.getBlockEntityRenderDispatcher()
            );
         } else {
            this.chunkBuilder.setWorld(this.world);
         }

         this.cloudRenderer.scheduleTerrainUpdate();
         RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
         this.viewDistance = this.client.options.getClampedViewDistance();
         if (this.chunks != null) {
            this.chunks.clear();
         }

         this.chunkBuilder.reset();
         synchronized (this.noCullingBlockEntities) {
            this.noCullingBlockEntities.clear();
         }

         this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.getClampedViewDistance(), this);
         this.chunkRenderingDataPreparer.setStorage(this.chunks);
         this.clear();
         Camera camera = this.client.gameRenderer.getCamera();
         this.chunks.updateCameraPosition(ChunkSectionPos.from(camera.getPos()));
         RenderDevice.enterManagedCode();
         try {
            this.sodium$renderer.reload();
         } finally {
            RenderDevice.exitManagedCode();
         }
      }
   }

   private void iris$disableFabulousGraphics() {
      if (Iris.getIrisConfig() != null
         && Iris.getIrisConfig().areShadersEnabled()
         && this.client.options.getGraphicsMode().getValue() == GraphicsMode.FABULOUS) {
         this.client.options.getGraphicsMode().setValue(GraphicsMode.FANCY);
      }
   }

   public void onResized(int width, int height) {
      this.scheduleTerrainUpdate();
      if (this.entityOutlineFramebuffer != null) {
         this.entityOutlineFramebuffer.resize(width, height);
      }
   }

   public String getChunksDebugString() {
      if (this.sodium$renderer != null) {
         return this.sodium$renderer.getChunksDebugString();
      }
      int i = this.chunks.chunks.length;
      int j = this.getCompletedChunkCount();
      return String.format(
         Locale.ROOT,
         "C: %d/%d %sD: %d, %s",
         j,
         i,
         this.client.chunkCullingEnabled ? "(s) " : "",
         this.viewDistance,
         this.chunkBuilder == null ? "null" : this.chunkBuilder.getDebugString()
      );
   }

   public ChunkBuilder getChunkBuilder() {
      return this.chunkBuilder;
   }

   public double getChunkCount() {
      return this.chunks.chunks.length;
   }

   public double getViewDistance() {
      return this.viewDistance;
   }

   public int getCompletedChunkCount() {
      if (this.sodium$renderer != null) {
         return this.sodium$renderer.getVisibleChunkCount();
      }
      int i = 0;
      ObjectListIterator var2 = this.builtChunks.iterator();

      while (var2.hasNext()) {
         ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)var2.next();
         if (builtChunk.getData().hasNonEmptyLayers()) {
            i++;
         }
      }

      return i;
   }

   public String getEntitiesDebugString() {
      return "E: " + this.renderedEntitiesCount + "/" + this.world.getRegularEntityCount() + ", SD: " + this.world.getSimulationDistance();
   }

   private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator) {
      if (this.sodium$renderer != null) {
         Viewport viewport = ((ViewportProvider)frustum).sodium$createViewport();
         RenderDevice.enterManagedCode();
         try {
            this.sodium$renderer.setupTerrain(camera, viewport, Fog.DUMMY, spectator, FlawlessFrames.isActive());
         } finally {
            RenderDevice.exitManagedCode();
         }
         return;
      }
      Vec3d vec3d = camera.getPos();
      if (this.client.options.getClampedViewDistance() != this.viewDistance) {
         this.reload();
      }

      Profiler profiler = Profilers.get();
      profiler.push("camera");
      int i = ChunkSectionPos.getSectionCoord(vec3d.getX());
      int j = ChunkSectionPos.getSectionCoord(vec3d.getY());
      int k = ChunkSectionPos.getSectionCoord(vec3d.getZ());
      if (this.cameraChunkX != i || this.cameraChunkY != j || this.cameraChunkZ != k) {
         this.cameraChunkX = i;
         this.cameraChunkY = j;
         this.cameraChunkZ = k;
         this.chunks.updateCameraPosition(ChunkSectionPos.from(vec3d));
      }

      this.chunkBuilder.setCameraPosition(vec3d);
      profiler.swap("cull");
      double d = Math.floor(vec3d.x / 8.0);
      double e = Math.floor(vec3d.y / 8.0);
      double f = Math.floor(vec3d.z / 8.0);
      if (d != this.lastCameraX || e != this.lastCameraY || f != this.lastCameraZ) {
         this.chunkRenderingDataPreparer.scheduleTerrainUpdate();
      }

      this.lastCameraX = d;
      this.lastCameraY = e;
      this.lastCameraZ = f;
      profiler.swap("update");
      if (!hasForcedFrustum) {
         boolean bl = this.client.chunkCullingEnabled;
         if (spectator && this.world.getBlockState(camera.getBlockPos()).isOpaqueFullCube()) {
            bl = false;
         }

         profiler.push("section_occlusion_graph");
         this.chunkRenderingDataPreparer.updateSectionOcclusionGraph(bl, camera, frustum, this.builtChunks, this.world.getChunkManager().getActiveSections());
         profiler.pop();
         double g = Math.floor(camera.getPitch() / 2.0F);
         double h = Math.floor(camera.getYaw() / 2.0F);
         if (this.chunkRenderingDataPreparer.method_52836() || g != this.lastCameraPitch || h != this.lastCameraYaw) {
            this.applyFrustum(offsetFrustum(frustum));
            this.lastCameraPitch = g;
            this.lastCameraYaw = h;
         }
      }

      profiler.pop();
   }

   public static Frustum offsetFrustum(Frustum frustum) {
      return new Frustum(frustum).coverBoxAroundSetPosition(8);
   }

   private void applyFrustum(Frustum frustum) {
      if (!MinecraftClient.getInstance().isOnThread()) {
         throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
      }

      Profilers.get().push("apply_frustum");
      this.clear();
      this.chunkRenderingDataPreparer.collectChunks(frustum, this.builtChunks, this.nearbyChunks);
      Profilers.get().pop();
   }

   public void addBuiltChunk(ChunkBuilder.BuiltChunk chunk) {
      this.chunkRenderingDataPreparer.schedulePropagationFrom(chunk);
   }

   public void setupFrustum(Vec3d pos, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
      this.frustum = new Frustum(positionMatrix, projectionMatrix);
      this.frustum.setPosition(pos.getX(), pos.getY(), pos.getZ());
   }

   public void render(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix
   ) {
      CoreFeature.setRenderingLevel();
      WorldRenderingPipeline configuredPipeline = Iris.getPipelineManager().getPipelineNullable();
      boolean iris$shaderPackActive = configuredPipeline instanceof IrisRenderingPipeline;
      boolean iris$levelRenderingStarted = false;
      try {
      if (iris$shaderPackActive) {
         this.iris$beginBatchedEntityRendering();
         iris$levelRenderingStarted = true;
         this.iris$beginLevelRendering(tickCounter, camera, positionMatrix, projectionMatrix);
      } else {
         this.iris$pipeline = configuredPipeline instanceof VanillaRenderingPipeline ? configuredPipeline : this.iris$vanillaPipeline;
      }

      float f = tickCounter.getTickDelta(false);
      RenderSystem.setShaderGameTime(this.world.getTime(), f);
      this.blockEntityRenderDispatcher.configure(this.world, camera, this.client.crosshairTarget);
      this.entityRenderDispatcher.configure(this.world, camera, this.client.targetedEntity);
      final Profiler profiler = Profilers.get();
      profiler.swap("light_update_queue");
      this.world.runQueuedChunkUpdates();
      profiler.swap("light_updates");
      this.world.getChunkManager().getLightingProvider().doLightUpdates();
      Vec3d vec3d = camera.getPos();
      double d = vec3d.getX();
      double e = vec3d.getY();
      double g = vec3d.getZ();
      profiler.swap("culling");
      boolean bl = this.capturedFrustum != null;
      Frustum frustum = bl ? this.capturedFrustum : this.frustum;
      Profilers.get().swap("captureFrustum");
      if (this.shouldCaptureFrustum) {
         this.capturedFrustum = bl ? new Frustum(positionMatrix, projectionMatrix) : frustum;
         this.capturedFrustum.setPosition(d, e, g);
         this.shouldCaptureFrustum = false;
      }

      profiler.swap("fog");
      float h = gameRenderer.getViewDistance();
      boolean bl2 = this.client.world.getDimensionEffects().useThickFog(MathHelper.floor(d), MathHelper.floor(e))
         || this.client.inGameHud.getBossBarHud().shouldThickenFog();
      Vector4f vector4f = BackgroundRenderer.getFogColor(
         camera, f, this.client.world, this.client.options.getClampedViewDistance(), gameRenderer.getSkyDarkness(f)
      );
      Fog fog = BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, vector4f, h, bl2, f);
      Fog fog2 = BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, vector4f, h, bl2, f);
      profiler.swap("cullEntities");
      boolean bl3 = this.getEntitiesToRender(camera, frustum, this.renderedEntities);
      this.renderedEntitiesCount = this.renderedEntities.size();
      this.iris$pipeline.renderShadows(this, camera);
      profiler.swap("terrain_setup");
      if (!(this.iris$pipeline instanceof IrisRenderingPipeline irisPipeline) || !irisPipeline.skipAllRendering()) {
         this.setupTerrain(camera, frustum, bl, this.client.player.isSpectator());
      }
      profiler.swap("compile_sections");
      this.updateChunks(camera);
      Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
      matrix4fStack.pushMatrix();
      matrix4fStack.mul(positionMatrix);
      FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
      this.framebufferSet.mainFramebuffer = frameGraphBuilder.createObjectNode("main", this.client.getFramebuffer());
      int i = this.client.getFramebuffer().textureWidth;
      int j = this.client.getFramebuffer().textureHeight;
      SimpleFramebufferFactory simpleFramebufferFactory = new SimpleFramebufferFactory(i, j, true);
      PostEffectProcessor postEffectProcessor = this.getTransparencyPostEffectProcessor();
      if (postEffectProcessor != null) {
         this.framebufferSet.translucentFramebuffer = frameGraphBuilder.createResourceHandle("translucent", simpleFramebufferFactory);
         this.framebufferSet.itemEntityFramebuffer = frameGraphBuilder.createResourceHandle("item_entity", simpleFramebufferFactory);
         this.framebufferSet.particlesFramebuffer = frameGraphBuilder.createResourceHandle("particles", simpleFramebufferFactory);
         this.framebufferSet.weatherFramebuffer = frameGraphBuilder.createResourceHandle("weather", simpleFramebufferFactory);
         this.framebufferSet.cloudsFramebuffer = frameGraphBuilder.createResourceHandle("clouds", simpleFramebufferFactory);
      }

      if (this.entityOutlineFramebuffer != null) {
         this.framebufferSet.entityOutlineFramebuffer = frameGraphBuilder.createObjectNode("entity_outline", this.entityOutlineFramebuffer);
      }

      RenderPass renderPass = frameGraphBuilder.createPass("clear");
      this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      renderPass.setRenderer(() -> {
         RenderSystem.clearColor(vector4f.x, vector4f.y, vector4f.z, 0.0F);
         RenderSystem.clear(16640);
      });
      if (iris$shaderPackActive) {
         RenderPass irisSetupPass = frameGraphBuilder.createPass("iris_setup");
         this.framebufferSet.mainFramebuffer = irisSetupPass.transfer(this.framebufferSet.mainFramebuffer);
         irisSetupPass.addRequired(renderPass);
         irisSetupPass.setRenderer(() -> {
            Fog previousFog = RenderSystem.getShaderFog();
            RenderSystem.setShaderFog(fog);
            this.iris$pipeline.onBeginClear();
            RenderSystem.setShaderFog(previousFog);
         });
      }

      if (this.iris$pipeline != null || !bl2) {
         this.renderSky(frameGraphBuilder, camera, f, fog2);
      }

      this.renderMain(frameGraphBuilder, frustum, camera, positionMatrix, projectionMatrix, fog, renderBlockOutline, bl3, tickCounter, profiler);
      PostEffectProcessor postEffectProcessor2 = this.client.getShaderLoader().loadPostEffect(ENTITY_OUTLINE, DefaultFramebufferSet.MAIN_AND_ENTITY_OUTLINE);
      if (bl3 && postEffectProcessor2 != null) {
         postEffectProcessor2.render(frameGraphBuilder, i, j, this.framebufferSet);
      }

      this.renderParticles(frameGraphBuilder, camera, f, fog);
      CloudRenderMode cloudRenderMode = this.client.options.getCloudRenderModeValue();
      if (cloudRenderMode != CloudRenderMode.OFF) {
         float k = this.world.getDimensionEffects().getCloudsHeight();
         if (!Float.isNaN(k)) {
            float l = this.ticks + f;
            int m = this.world.getCloudsColor(f);
            this.renderClouds(frameGraphBuilder, positionMatrix, projectionMatrix, cloudRenderMode, camera.getPos(), l, m, k + 0.33F);
         }
      }

      this.renderWeather(frameGraphBuilder, camera.getPos(), f, fog);
      if (postEffectProcessor != null) {
         postEffectProcessor.render(frameGraphBuilder, i, j, this.framebufferSet);
      }

      this.renderLateDebug(frameGraphBuilder, vec3d, fog);
      profiler.swap("framegraph");
      frameGraphBuilder.run(allocator, new FrameGraphBuilder.Profiler() {
         @Override
         public void push(String location) {
            profiler.push(location);
         }

         @Override
         public void pop(String location) {
            profiler.pop();
         }
      });
      this.client.getFramebuffer().beginWrite(false);
      this.renderedEntities.clear();
      this.framebufferSet.clear();
      matrix4fStack.popMatrix();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.setShaderFog(Fog.DUMMY);
      if (iris$shaderPackActive) {
         this.iris$finishLevelRendering(positionMatrix, tickCounter, camera, gameRenderer);
      }
       } finally {
          try {
             if (iris$levelRenderingStarted) {
                this.iris$restoreLevelRenderingState();
             }
          } finally {
             try {
                 if (iris$shaderPackActive) {
                    this.iris$endBatchedEntityRendering();
                 }
             } finally {
                CoreFeature.resetRenderingLevel();
             }
          }
       }
   }

   private void iris$beginBatchedEntityRendering() {
      if (!Iris.isPackInUseQuick()) {
         return;
      }

      if (this.bufferBuilders instanceof DrawCallTrackingRenderBuffers drawCallTracking) {
         drawCallTracking.resetDrawCounts();
      }

      ((RenderBuffersExt)this.bufferBuilders).beginLevelRendering();
      ImmediateState.isRenderingLevel = true;
      this.iris$batchedRenderingActive = true;
      VertexConsumerProvider provider = this.bufferBuilders.getEntityVertexConsumers();
      this.iris$batchedGroupable = provider instanceof Groupable groupable ? groupable : null;
   }

   private void iris$endBatchedEntityRendering() {
      try {
         if (this.iris$batchedRenderingActive) {
            ((RenderBuffersExt)this.bufferBuilders).endLevelRendering();
         }
      } finally {
         ImmediateState.isRenderingLevel = false;
         this.iris$batchedRenderingActive = false;
         this.iris$batchedGroupable = null;
      }
   }

   private void iris$beginLevelRendering(RenderTickCounter tickCounter, Camera camera, Matrix4f modelView, Matrix4f projection) {
      ((PhasedParticleEngine)this.client.particleManager).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
      DHCompat.checkFrame();
      IrisTimeUniforms.updateTime();
      float tickDelta = tickCounter.getTickDelta(false);
      CapturedRenderingState.INSTANCE.setGbufferModelView(modelView);
      CapturedRenderingState.INSTANCE.setGbufferProjection(projection);
      CapturedRenderingState.INSTANCE.setTickDelta(tickDelta);
      CapturedRenderingState.INSTANCE.setCloudTime((this.ticks + tickDelta) * 0.03F);
      this.iris$pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
      if (this.iris$pipeline.shouldDisableFrustumCulling()) {
         this.frustum = new NonCullingFrustum();
         this.frustum.setPosition(camera.getPos().getX(), camera.getPos().getY(), camera.getPos().getZ());
      }

      this.iris$pipeline.beginLevelRendering();
      this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
      IrisRenderSystem.backupAndDisableCullingState(this.iris$pipeline.shouldDisableOcclusionCulling());
      if (Iris.shouldActivateWireframe() && this.client.isInSingleplayer()) {
         IrisRenderSystem.setPolygonMode(6913);
      }
   }

   private void iris$finishLevelRendering(
      Matrix4f modelView, RenderTickCounter tickCounter, Camera camera, GameRenderer gameRenderer
   ) {
      HandRenderer.INSTANCE.renderTranslucent(modelView, tickCounter.getTickDelta(true), camera, gameRenderer, this.iris$pipeline);
      Profilers.get().swap("iris_final");
      this.iris$pipeline.finalizeLevelRendering();
   }

   private void iris$restoreLevelRenderingState() {
      IrisRenderSystem.restoreCullingState();
      if (Iris.shouldActivateWireframe() && this.client.isInSingleplayer()) {
         IrisRenderSystem.setPolygonMode(6914);
      }

      this.iris$pipeline = null;
   }

   private void renderMain(
      FrameGraphBuilder frameGraphBuilder,
      Frustum frustum,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Fog fog,
      boolean renderBlockOutline,
      boolean renderEntityOutlines,
      RenderTickCounter renderTickCounter,
      Profiler profiler
   ) {
      RenderPass renderPass = frameGraphBuilder.createPass("main");
      this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      if (this.framebufferSet.translucentFramebuffer != null) {
         this.framebufferSet.translucentFramebuffer = renderPass.transfer(this.framebufferSet.translucentFramebuffer);
      }

      if (this.framebufferSet.itemEntityFramebuffer != null) {
         this.framebufferSet.itemEntityFramebuffer = renderPass.transfer(this.framebufferSet.itemEntityFramebuffer);
      }

      if (this.framebufferSet.weatherFramebuffer != null) {
         this.framebufferSet.weatherFramebuffer = renderPass.transfer(this.framebufferSet.weatherFramebuffer);
      }

      if (renderEntityOutlines && this.framebufferSet.entityOutlineFramebuffer != null) {
         this.framebufferSet.entityOutlineFramebuffer = renderPass.transfer(this.framebufferSet.entityOutlineFramebuffer);
      }

      Handle<Framebuffer> handle = this.framebufferSet.mainFramebuffer;
      Handle<Framebuffer> handle2 = this.framebufferSet.translucentFramebuffer;
      Handle<Framebuffer> handle3 = this.framebufferSet.itemEntityFramebuffer;
      Handle<Framebuffer> handle4 = this.framebufferSet.weatherFramebuffer;
      Handle<Framebuffer> handle5 = this.framebufferSet.entityOutlineFramebuffer;
      renderPass.setRenderer(() -> {
         try {
         RenderSystem.setShaderFog(fog);
         float f = renderTickCounter.getTickDelta(false);
         Vec3d vec3d = camera.getPos();
         double d = vec3d.getX();
         double e = vec3d.getY();
         double g = vec3d.getZ();
         profiler.push("terrain");
         this.renderLayer(RenderLayer.getSolid(), d, e, g, positionMatrix, projectionMatrix);
         this.renderLayer(RenderLayer.getCutoutMipped(), d, e, g, positionMatrix, projectionMatrix);
         this.renderLayer(RenderLayer.getCutout(), d, e, g, positionMatrix, projectionMatrix);
         if (this.world.getDimensionEffects().isDarkened()) {
            DiffuseLighting.enableForLevel();
         } else {
            DiffuseLighting.disableForLevel();
         }

         if (handle3 != null) {
            handle3.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle3.get().clear();
            handle3.get().copyDepthFrom(this.client.getFramebuffer());
            handle.get().beginWrite(false);
         }

         if (handle4 != null) {
            handle4.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle4.get().clear();
         }

         if (this.canDrawEntityOutlines() && handle5 != null) {
            handle5.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle5.get().clear();
            handle.get().beginWrite(false);
         }

         this.iris$renderOpaqueParticles(camera, f);
         MatrixStack matrixStack = new MatrixStack();
         VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
         VertexConsumerProvider.Immediate immediate2 = this.bufferBuilders.getEffectVertexConsumers();
         profiler.swap("entities");
         this.renderEntities(matrixStack, immediate, camera, renderTickCounter, this.renderedEntities);
         immediate.drawCurrentLayer();
         this.checkEmpty(matrixStack);
         profiler.swap("blockentities");
         this.renderBlockEntities(matrixStack, immediate, immediate2, camera, f);
         immediate.drawCurrentLayer();
         this.checkEmpty(matrixStack);
         immediate.draw(RenderLayer.getSolid());
         immediate.draw(RenderLayer.getEndPortal());
         immediate.draw(RenderLayer.getEndGateway());
         immediate.draw(TexturedRenderLayers.getEntitySolid());
         immediate.draw(TexturedRenderLayers.getEntityCutout());
         immediate.draw(TexturedRenderLayers.getBeds());
         immediate.draw(TexturedRenderLayers.getShulkerBoxes());
         immediate.draw(TexturedRenderLayers.getSign());
         immediate.draw(TexturedRenderLayers.getHangingSign());
         immediate.draw(TexturedRenderLayers.getChest());
         this.bufferBuilders.getOutlineVertexConsumers().draw();
          if (AcceleratedRendering.isAvailable() && ComputeShaderProgramLoader.isProgramsLoaded()) {
            CoreBuffers.drawOutlineBuffers();
         }
         if (renderBlockOutline) {
            this.renderTargetBlockOutline(camera, immediate, matrixStack, false);
         }

         this.iris$pipeline.setPhase(WorldRenderingPhase.DEBUG);
         profiler.swap("debug");
         this.client.debugRenderer.render(matrixStack, frustum, immediate, d, e, g);
         immediate.drawCurrentLayer();
         this.checkEmpty(matrixStack);
         this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
         immediate.draw(TexturedRenderLayers.getItemEntityTranslucentCull());
         immediate.draw(TexturedRenderLayers.getBannerPatterns());
         immediate.draw(TexturedRenderLayers.getShieldPatterns());
         immediate.draw(RenderLayer.getArmorEntityGlint());
         immediate.draw(RenderLayer.getGlint());
         immediate.draw(RenderLayer.getGlintTranslucent());
         immediate.draw(RenderLayer.getEntityGlint());
         profiler.swap("destroyProgress");
         this.renderBlockDamage(matrixStack, camera, immediate2);
         immediate2.draw();
         this.checkEmpty(matrixStack);
         immediate.draw(RenderLayer.getWaterMask());
         this.iris$flushEntityBuffersBeforeTranslucents(immediate);
         if (handle2 != null) {
            handle2.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle2.get().clear();
            handle2.get().copyDepthFrom(handle.get());
         }

         this.iris$pipeline.beginHand();
         HandRenderer.INSTANCE.renderSolid(
            positionMatrix, renderTickCounter.getTickDelta(true), camera, MinecraftClient.getInstance().gameRenderer, this.iris$pipeline
         );
         profiler.swap("iris_pre_translucent");
         this.iris$pipeline.beginTranslucents();
         profiler.swap("translucent");
         this.renderLayer(RenderLayer.getTranslucent(), d, e, g, positionMatrix, projectionMatrix);
         profiler.swap("string");
         this.renderLayer(RenderLayer.getTripwire(), d, e, g, positionMatrix, projectionMatrix);
         if (renderBlockOutline) {
            this.renderTargetBlockOutline(camera, immediate, matrixStack, true);
         }

          if (AcceleratedRendering.isAvailable() && ComputeShaderProgramLoader.isProgramsLoaded()) {
            CoreBuffers.drawBuffers();
         }
         immediate.draw();
         profiler.pop();
         } finally {
            this.iris$endBatchedEntityRendering();
         }
      });
   }

   private void iris$flushEntityBuffersBeforeTranslucents(VertexConsumerProvider.Immediate immediate) {
       if (AcceleratedRendering.isAvailable() && ComputeShaderProgramLoader.isProgramsLoaded()) {
         CoreBuffers.drawBuffers();
      }
      if (immediate instanceof FullyBufferedMultiBufferSource fullyBuffered) {
         fullyBuffered.readyUp();
      }

      if (WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws()) {
         Profilers.get().swap("entity_draws_opaque");
         if (immediate instanceof FullyBufferedMultiBufferSource fullyBuffered) {
            fullyBuffered.endBatchWithType(TransparencyType.OPAQUE);
            fullyBuffered.endBatchWithType(TransparencyType.OPAQUE_DECAL);
            fullyBuffered.endBatchWithType(TransparencyType.WATER_MASK);
         } else {
            immediate.draw();
         }
      } else {
         Profilers.get().swap("entity_draws");
         immediate.draw();
      }
   }

   private void renderParticles(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog) {
      RenderPass renderPass = frameGraphBuilder.createPass("particles");
      if (this.framebufferSet.particlesFramebuffer != null) {
         this.framebufferSet.particlesFramebuffer = renderPass.transfer(this.framebufferSet.particlesFramebuffer);
         renderPass.dependsOn(this.framebufferSet.mainFramebuffer);
      } else {
         this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      }

      Handle<Framebuffer> handle = this.framebufferSet.mainFramebuffer;
      Handle<Framebuffer> handle2 = this.framebufferSet.particlesFramebuffer;
      renderPass.setRenderer(() -> {
         RenderSystem.setShaderFog(fog);
         if (handle2 != null) {
            handle2.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle2.get().clear();
            handle2.get().copyDepthFrom(handle.get());
         }

         ParticleRenderingSettings settings = this.iris$getParticleRenderingSettings();
         if (settings == ParticleRenderingSettings.AFTER) {
            this.client.particleManager.renderParticles(camera, tickDelta, this.bufferBuilders.getEntityVertexConsumers());
         } else if (settings == ParticleRenderingSettings.MIXED) {
            ((PhasedParticleEngine)this.client.particleManager).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
            this.client.particleManager.renderParticles(camera, tickDelta, this.bufferBuilders.getEntityVertexConsumers());
         }
      });
   }

   private void iris$renderOpaqueParticles(Camera camera, float tickDelta) {
      ParticleRenderingSettings settings = this.iris$getParticleRenderingSettings();
      if (settings == ParticleRenderingSettings.BEFORE) {
         this.client.particleManager.renderParticles(camera, tickDelta, this.bufferBuilders.getEntityVertexConsumers());
      } else if (settings == ParticleRenderingSettings.MIXED) {
         ((PhasedParticleEngine)this.client.particleManager).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
         this.client.particleManager.renderParticles(camera, tickDelta, this.bufferBuilders.getEntityVertexConsumers());
      }
   }

   private ParticleRenderingSettings iris$getParticleRenderingSettings() {
      return Iris.getPipelineManager()
         .getPipeline()
         .map(WorldRenderingPipeline::getParticleRenderingSettings)
         .orElse(ParticleRenderingSettings.MIXED);
   }

   private void renderClouds(
      FrameGraphBuilder frameGraphBuilder,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      CloudRenderMode renderMode,
      Vec3d cameraPos,
      float ticks,
      int color,
      float cloudHeight
   ) {
      RenderPass renderPass = frameGraphBuilder.createPass("clouds");
      if (this.framebufferSet.cloudsFramebuffer != null) {
         this.framebufferSet.cloudsFramebuffer = renderPass.transfer(this.framebufferSet.cloudsFramebuffer);
      } else {
         this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      }

      Handle<Framebuffer> handle = this.framebufferSet.cloudsFramebuffer;
      renderPass.setRenderer(() -> {
         this.iris$pipeline.setPhase(WorldRenderingPhase.CLOUDS);
         try {
         if (handle != null) {
            handle.get().setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            handle.get().clear();
         }

         this.cloudRenderer.renderClouds(color, renderMode, cloudHeight, positionMatrix, projectionMatrix, cameraPos, ticks);
         } finally {
            this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
         }
      });
   }

   private void renderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog) {
      int i = this.client.options.getClampedViewDistance() * 16;
      float f = this.client.gameRenderer.getFarPlaneDistance();
      RenderPass renderPass = frameGraphBuilder.createPass("weather");
      if (this.framebufferSet.weatherFramebuffer != null) {
         this.framebufferSet.weatherFramebuffer = renderPass.transfer(this.framebufferSet.weatherFramebuffer);
      } else {
         this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      }

      renderPass.setRenderer(() -> {
         this.iris$pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
         try {
         RenderSystem.setShaderFog(fog);
         VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
         this.weatherRendering.renderPrecipitation(this.client.world, immediate, this.ticks, tickDelta, pos);
         this.iris$pipeline.setPhase(WorldRenderingPhase.WORLD_BORDER);
         this.worldBorderRendering.render(this.world.getWorldBorder(), pos, i, f);
         immediate.draw();
         } finally {
            this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
         }
      });
   }

   private void renderLateDebug(FrameGraphBuilder frameGraphBuilder, Vec3d pos, Fog fog) {
      RenderPass renderPass = frameGraphBuilder.createPass("late_debug");
      this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
      if (this.framebufferSet.itemEntityFramebuffer != null) {
         this.framebufferSet.itemEntityFramebuffer = renderPass.transfer(this.framebufferSet.itemEntityFramebuffer);
      }

      Handle<Framebuffer> handle = this.framebufferSet.mainFramebuffer;
      renderPass.setRenderer(() -> {
         RenderSystem.setShaderFog(fog);
         handle.get().beginWrite(false);
         MatrixStack matrixStack = new MatrixStack();
         VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
         this.client.debugRenderer.renderLate(matrixStack, immediate, pos.x, pos.y, pos.z);
         immediate.drawCurrentLayer();
         this.checkEmpty(matrixStack);
      });
   }

   private boolean getEntitiesToRender(Camera camera, Frustum frustum, List<Entity> output) {
      if (this.iris$pipeline instanceof IrisRenderingPipeline irisPipeline && irisPipeline.skipAllRendering()) {
         return false;
      }

      Vec3d vec3d = camera.getPos();
      double d = vec3d.getX();
      double e = vec3d.getY();
      double f = vec3d.getZ();
      boolean bl = false;
      boolean bl2 = this.canDrawEntityOutlines();
      Entity.setRenderDistanceMultiplier(
         MathHelper.clamp(this.client.options.getClampedViewDistance() / 8.0, 1.0, 2.5) * this.client.options.getEntityDistanceScaling().getValue()
      );

      for (Entity entity : this.iris$sortEntitiesByType(this.world.getEntities())) {
         if (this.entityRenderDispatcher.shouldRender(entity, frustum, d, e, f) || entity.hasPassengerDeep(this.client.player)) {
            BlockPos blockPos = entity.getBlockPos();
            if ((this.world.isOutOfHeightLimit(blockPos.getY()) || this.isRenderingReady(blockPos))
               && (
                  entity != camera.getFocusedEntity()
                     || camera.isThirdPerson()
                     || camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)camera.getFocusedEntity()).isSleeping()
               )
               && (!(entity instanceof ClientPlayerEntity) || camera.getFocusedEntity() == entity)) {
               output.add(entity);
               if (bl2 && this.client.hasOutline(entity)) {
                  bl = true;
               }
            }
         }
      }

      return bl;
   }

   private Iterable<Entity> iris$sortEntitiesByType(Iterable<Entity> entities) {
      if (!this.iris$batchedRenderingActive) {
         return entities;
      }

      Profiler profiler = Profilers.get();
      profiler.push("sortEntityList");

      try {
         Map<EntityType<?>, List<Entity>> sortedEntities = new HashMap<>();
         for (Entity entity : entities) {
            sortedEntities.computeIfAbsent(entity.getType(), entityType -> new ArrayList<>(32)).add(entity);
         }

         List<Entity> result = new ArrayList<>();
         sortedEntities.values().forEach(result::addAll);
         return result;
      } finally {
         profiler.pop();
      }
   }

   private void renderEntities(
      MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Camera camera, RenderTickCounter tickCounter, List<Entity> entities
   ) {
      Vec3d vec3d = camera.getPos();
      double d = vec3d.getX();
      double e = vec3d.getY();
      double f = vec3d.getZ();
      TickManager tickManager = this.client.world.getTickManager();
      boolean bl = this.canDrawEntityOutlines();

      for (Entity entity : entities) {
         if (entity.age == 0) {
            entity.lastRenderX = entity.getX();
            entity.lastRenderY = entity.getY();
            entity.lastRenderZ = entity.getZ();
         }

         VertexConsumerProvider vertexConsumerProvider;
         if (bl && this.client.hasOutline(entity)) {
            OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
            vertexConsumerProvider = outlineVertexConsumerProvider;
            int i = entity.getTeamColorValue();
            outlineVertexConsumerProvider.setColor(ColorHelper.getRed(i), ColorHelper.getGreen(i), ColorHelper.getBlue(i), 255);
         } else {
            vertexConsumerProvider = immediate;
         }

         float g = tickCounter.getTickDelta(!tickManager.shouldSkipTick(entity));
         if (this.iris$batchedGroupable != null) {
            this.iris$batchedGroupable.startGroup();
         }

         try {
            this.renderEntity(entity, d, e, f, g, matrices, vertexConsumerProvider);
         } finally {
            if (this.iris$batchedGroupable != null) {
               this.iris$batchedGroupable.endGroup();
            }
         }
      }
   }

   private void renderBlockEntities(
      MatrixStack matrices, VertexConsumerProvider.Immediate immediate, VertexConsumerProvider.Immediate immediate2, Camera camera, float tickDelta
   ) {
      if (this.sodium$renderer != null) {
         this.sodium$renderer.renderBlockEntities(matrices, this.bufferBuilders, this.blockBreakingProgressions, camera, tickDelta);
         return;
      }
      Vec3d vec3d = camera.getPos();
      double d = vec3d.getX();
      double e = vec3d.getY();
      double f = vec3d.getZ();
      ObjectListIterator var13 = this.builtChunks.iterator();

      while (var13.hasNext()) {
         ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)var13.next();
         List<BlockEntity> list = builtChunk.getData().getBlockEntities();
         if (!list.isEmpty()) {
            for (BlockEntity blockEntity : list) {
               BlockPos blockPos = blockEntity.getPos();
               VertexConsumerProvider vertexConsumerProvider = immediate;
               matrices.push();
               matrices.translate(blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - f);
               SortedSet<BlockBreakingInfo> sortedSet = (SortedSet<BlockBreakingInfo>)this.blockBreakingProgressions.get(blockPos.asLong());
               if (sortedSet != null && !sortedSet.isEmpty()) {
                  int i = sortedSet.last().getStage();
                  if (i >= 0) {
                     MatrixStack.Entry entry = matrices.peek();
                     VertexConsumer vertexConsumer = new OverlayVertexConsumer(
                        immediate2.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(i)), entry, 1.0F
                     );
                     vertexConsumerProvider = renderLayer -> {
                        VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                        return renderLayer.hasCrumbling() ? VertexConsumers.union(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                     };
                  }
               }

               this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, vertexConsumerProvider);
               matrices.pop();
            }
         }
      }

      synchronized (this.noCullingBlockEntities) {
         for (BlockEntity blockEntity2 : this.noCullingBlockEntities) {
            BlockPos blockPos2 = blockEntity2.getPos();
            matrices.push();
            matrices.translate(blockPos2.getX() - d, blockPos2.getY() - e, blockPos2.getZ() - f);
            this.blockEntityRenderDispatcher.render(blockEntity2, tickDelta, matrices, immediate);
            matrices.pop();
         }
      }
   }

   private void renderBlockDamage(MatrixStack matrices, Camera camera, VertexConsumerProvider.Immediate vertexConsumers) {
      Vec3d vec3d = camera.getPos();
      double d = vec3d.getX();
      double e = vec3d.getY();
      double f = vec3d.getZ();
      ObjectIterator var11 = this.blockBreakingProgressions.long2ObjectEntrySet().iterator();

      while (var11.hasNext()) {
         Entry<SortedSet<BlockBreakingInfo>> entry = (Entry<SortedSet<BlockBreakingInfo>>)var11.next();
         BlockPos blockPos = BlockPos.fromLong(entry.getLongKey());
         if (!(blockPos.getSquaredDistanceFromCenter(d, e, f) > 1024.0)) {
            SortedSet<BlockBreakingInfo> sortedSet = (SortedSet<BlockBreakingInfo>)entry.getValue();
            if (sortedSet != null && !sortedSet.isEmpty()) {
               int i = sortedSet.last().getStage();
               matrices.push();
               matrices.translate(blockPos.getX() - d, blockPos.getY() - e, blockPos.getZ() - f);
               MatrixStack.Entry entry2 = matrices.peek();
               VertexConsumer vertexConsumer = new OverlayVertexConsumer(
                  vertexConsumers.getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(i)), entry2, 1.0F
               );
               this.client.getBlockRenderManager().renderDamage(this.world.getBlockState(blockPos), blockPos, this.world, matrices, vertexConsumer);
               matrices.pop();
            }
         }
      }
   }

   private void renderTargetBlockOutline(Camera camera, VertexConsumerProvider.Immediate vertexConsumers, MatrixStack matrices, boolean translucent) {
      if (this.client.crosshairTarget instanceof BlockHitResult blockHitResult) {
         if (blockHitResult.getType() != Type.MISS) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos)) {
               boolean bl = RenderLayers.getBlockLayer(blockState).isTranslucent();
               if (bl != translucent) {
                  return;
               }

               Vec3d vec3d = camera.getPos();
               Boolean boolean_ = this.client.options.getHighContrastBlockOutline().getValue();
               if (boolean_) {
                  VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getSecondaryBlockOutline());
                  this.drawBlockOutline(matrices, vertexConsumer, camera.getFocusedEntity(), vec3d.x, vec3d.y, vec3d.z, blockPos, blockState, -16777216);
               }

               VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getLines());
               int i = boolean_ ? -11010079 : ColorHelper.withAlpha(102, -16777216);
               this.drawBlockOutline(matrices, vertexConsumer, camera.getFocusedEntity(), vec3d.x, vec3d.y, vec3d.z, blockPos, blockState, i);
               vertexConsumers.drawCurrentLayer();
            }
         }
      }
   }

   private void checkEmpty(MatrixStack matrices) {
      if (!matrices.isEmpty()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void renderEntity(
      Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers
   ) {
      boolean filtered = FilterFeature.beginEntity(entity);
      try {
      double d = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
      double e = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
      double f = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
      EntityCullingManager entityCulling = EntityCullingManager.getInstance();
      if (entityCulling.shouldSkipEntity(entity)) {
         if (entityCulling.getConfig().renderNametagsThroughWalls) {
            net.minecraft.client.render.entity.EntityRenderer renderer = this.entityRenderDispatcher.getRenderer(entity);
            if (renderer != null) {
               matrices.push();
               matrices.translate(d - cameraX, e - cameraY, f - cameraZ);
               renderer.entityCulling$renderLabelIfPresent(
                  entity, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta)
               );
               matrices.pop();
            }
         }
         return;
      }
      this.entityRenderDispatcher
         .render(entity, d - cameraX, e - cameraY, f - cameraZ, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta));
      } finally {
         FilterFeature.end(filtered);
      }
   }

   private void translucencySort(Vec3d cameraPos) {
      if (!this.builtChunks.isEmpty()) {
         BlockPos blockPos = BlockPos.ofFloored(cameraPos);
         boolean bl = !blockPos.equals(this.prevTranslucencySortCameraPos);
         Profilers.get().push("translucent_sort");
         ChunkBuilder.NormalizedRelativePos normalizedRelativePos = new ChunkBuilder.NormalizedRelativePos();
         ObjectListIterator i = this.nearbyChunks.iterator();

         while (i.hasNext()) {
            ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)i.next();
            this.scheduleChunkTranslucencySort(builtChunk, normalizedRelativePos, cameraPos, bl, true);
         }

         this.chunkIndex = this.chunkIndex % this.builtChunks.size();
         int ix = Math.max(this.builtChunks.size() / 8, 15);

         while (ix-- > 0) {
            int j = this.chunkIndex++ % this.builtChunks.size();
            this.scheduleChunkTranslucencySort((ChunkBuilder.BuiltChunk)this.builtChunks.get(j), normalizedRelativePos, cameraPos, bl, false);
         }

         this.prevTranslucencySortCameraPos = blockPos;
         Profilers.get().pop();
      }
   }

   private void scheduleChunkTranslucencySort(
      ChunkBuilder.BuiltChunk chunk, ChunkBuilder.NormalizedRelativePos relativePos, Vec3d cameraPos, boolean needsUpdate, boolean ignoreCameraAlignment
   ) {
      relativePos.with(cameraPos, chunk.getSectionPos());
      boolean bl = !relativePos.equals(chunk.relativePos.get());
      boolean bl2 = needsUpdate && (relativePos.isOnCameraAxis() || ignoreCameraAlignment);
      if ((bl2 || bl) && !chunk.isCurrentlySorting() && chunk.hasTranslucentLayer()) {
         chunk.scheduleSort(this.chunkBuilder);
      }
   }

   private void renderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f viewMatrix, Matrix4f positionMatrix) {
      if (this.iris$pipeline != null) {
         this.iris$pipeline.setPhase(WorldRenderingPhase.fromTerrainRenderType(renderLayer));
      }

      try {
      if (this.sodium$renderer != null) {
         RenderDevice.enterManagedCode();
         try {
            this.sodium$renderer.drawChunkLayer(renderLayer, new ChunkRenderMatrices(positionMatrix, viewMatrix), x, y, z);
         } finally {
            RenderDevice.exitManagedCode();
         }
         return;
      }
      RenderSystem.assertOnRenderThread();
      ScopedProfiler scopedProfiler = Profilers.get().scoped(() -> "render_" + renderLayer.name);
      scopedProfiler.addLabel(renderLayer::toString);
      boolean bl = renderLayer != RenderLayer.getTranslucent();
      ObjectListIterator<ChunkBuilder.BuiltChunk> objectListIterator = this.builtChunks.listIterator(bl ? 0 : this.builtChunks.size());
      renderLayer.startDrawing();
      ShaderProgram shaderProgram = RenderSystem.getShader();
      if (shaderProgram == null) {
         renderLayer.endDrawing();
         scopedProfiler.close();
      } else {
         shaderProgram.initializeUniforms(VertexFormat.DrawMode.QUADS, viewMatrix, positionMatrix, this.client.getWindow());
         shaderProgram.bind();
         GlUniform glUniform = shaderProgram.modelOffset;

         while (bl ? objectListIterator.hasNext() : objectListIterator.hasPrevious()) {
            ChunkBuilder.BuiltChunk builtChunk = bl
               ? (ChunkBuilder.BuiltChunk)objectListIterator.next()
               : (ChunkBuilder.BuiltChunk)objectListIterator.previous();
            if (!builtChunk.getData().isEmpty(renderLayer)) {
               VertexBuffer vertexBuffer = builtChunk.getBuffer(renderLayer);
               BlockPos blockPos = builtChunk.getOrigin();
               if (glUniform != null) {
                  glUniform.set((float)(blockPos.getX() - x), (float)(blockPos.getY() - y), (float)(blockPos.getZ() - z));
                  glUniform.upload();
               }

               vertexBuffer.bind();
               vertexBuffer.draw();
            }
         }

         if (glUniform != null) {
            glUniform.set(0.0F, 0.0F, 0.0F);
         }

         shaderProgram.unbind();
         VertexBuffer.unbind();
         scopedProfiler.close();
         renderLayer.endDrawing();
      }
      } finally {
         if (this.iris$pipeline != null) {
            this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
         }
      }
   }

   public void captureFrustum() {
      this.shouldCaptureFrustum = true;
   }

   public void killFrustum() {
      this.capturedFrustum = null;
   }

   public void tick() {
      if (this.world.getTickManager().shouldTick()) {
         this.ticks++;
      }

      if (this.ticks % 20 == 0) {
         Iterator<BlockBreakingInfo> iterator = this.blockBreakingInfos.values().iterator();

         while (iterator.hasNext()) {
            BlockBreakingInfo blockBreakingInfo = iterator.next();
            int i = blockBreakingInfo.getLastUpdateTick();
            if (this.ticks - i > 400) {
               iterator.remove();
               this.removeBlockBreakingInfo(blockBreakingInfo);
            }
         }
      }
   }

   private void removeBlockBreakingInfo(BlockBreakingInfo info) {
      long l = info.getPos().asLong();
      Set<BlockBreakingInfo> set = (Set<BlockBreakingInfo>)this.blockBreakingProgressions.get(l);
      set.remove(info);
      if (set.isEmpty()) {
         this.blockBreakingProgressions.remove(l);
      }
   }

   private void renderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog) {
      CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
      if (Iris.getCurrentPack().isPresent()
         || cameraSubmersionType != CameraSubmersionType.POWDER_SNOW
            && cameraSubmersionType != CameraSubmersionType.LAVA
            && !this.hasBlindnessOrDarkness(camera))
       {
         DimensionEffects dimensionEffects = this.world.getDimensionEffects();
         DimensionEffects.SkyType skyType = dimensionEffects.getSkyType();
         if (skyType != DimensionEffects.SkyType.NONE) {
            RenderPass renderPass = frameGraphBuilder.createPass("sky");
            this.framebufferSet.mainFramebuffer = renderPass.transfer(this.framebufferSet.mainFramebuffer);
            renderPass.setRenderer(() -> {
               this.iris$pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
               try {
               RenderSystem.setShaderFog(fog);
               RenderSystem.setShader(ShaderProgramKeys.POSITION);
               if (skyType == DimensionEffects.SkyType.END) {
                  this.skyRendering.renderEndSky();
               } else {
                  MatrixStack matrixStack = new MatrixStack();
                  float g = this.world.getSkyAngleRadians(tickDelta);
                  float h = this.world.getSkyAngle(tickDelta);
                  float i = 1.0F - this.world.getRainGradient(tickDelta);
                  float j = this.world.getStarBrightness(tickDelta) * i;
                  int k = dimensionEffects.getSkyColor(h);
                  int l = this.world.getMoonPhase();
                  int m = this.world.getSkyColor(this.client.gameRenderer.getCamera().getPos(), tickDelta);
                  float n = ColorHelper.getRedFloat(m);
                  float o = ColorHelper.getGreenFloat(m);
                  float p = ColorHelper.getBlueFloat(m);
                  this.skyRendering.renderSky(n, o, p);
                  VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
                  if (dimensionEffects.isSunRisingOrSetting(h)) {
                     this.skyRendering.renderGlowingSky(matrixStack, immediate, g, k);
                  }

                  this.skyRendering.renderCelestialBodies(matrixStack, immediate, h, l, i, j, fog);
                  immediate.draw();
                  if (this.isSkyDark(tickDelta)) {
                     this.skyRendering.renderSkyDark(matrixStack);
                  }
               }
               } finally {
                  this.iris$pipeline.setPhase(WorldRenderingPhase.NONE);
               }
            });
         }
      }
   }

   private boolean isSkyDark(float tickDelta) {
      return this.client.player.getCameraPosVec(tickDelta).y - this.world.getLevelProperties().getSkyDarknessHeight(this.world) < 0.0;
   }

   private boolean hasBlindnessOrDarkness(Camera camera) {
      return !(camera.getFocusedEntity() instanceof LivingEntity livingEntity)
         ? false
         : livingEntity.hasStatusEffect(StatusEffects.BLINDNESS) || livingEntity.hasStatusEffect(StatusEffects.DARKNESS);
   }

   private void updateChunks(Camera camera) {
      if (this.sodium$renderer != null) {
         return;
      }

      Profiler profiler = Profilers.get();
      profiler.push("populate_sections_to_compile");
      ChunkRendererRegionBuilder chunkRendererRegionBuilder = new ChunkRendererRegionBuilder();
      BlockPos blockPos = camera.getBlockPos();
      List<ChunkBuilder.BuiltChunk> list = Lists.newArrayList();
      ObjectListIterator var6 = this.builtChunks.iterator();

      while (var6.hasNext()) {
         ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)var6.next();
         if (builtChunk.needsRebuild() && builtChunk.shouldBuild()) {
            boolean bl = false;
            if (this.client.options.getChunkBuilderMode().getValue() == ChunkBuilderMode.NEARBY) {
               BlockPos blockPos2 = builtChunk.getOrigin().add(8, 8, 8);
               bl = blockPos2.getSquaredDistance(blockPos) < 768.0 || builtChunk.needsImportantRebuild();
            } else if (this.client.options.getChunkBuilderMode().getValue() == ChunkBuilderMode.PLAYER_AFFECTED) {
               bl = builtChunk.needsImportantRebuild();
            }

            if (bl) {
               profiler.push("build_near_sync");
               this.chunkBuilder.rebuild(builtChunk, chunkRendererRegionBuilder);
               builtChunk.cancelRebuild();
               profiler.pop();
            } else {
               list.add(builtChunk);
            }
         }
      }

      profiler.swap("upload");
      this.chunkBuilder.upload();
      profiler.swap("schedule_async_compile");

      for (ChunkBuilder.BuiltChunk builtChunk : list) {
         builtChunk.scheduleRebuild(this.chunkBuilder, chunkRendererRegionBuilder);
         builtChunk.cancelRebuild();
      }

      profiler.pop();
      this.translucencySort(camera.getPos());
   }

   private void drawBlockOutline(
      MatrixStack matrices,
      VertexConsumer vertexConsumer,
      Entity entity,
      double cameraX,
      double cameraY,
      double cameraZ,
      BlockPos pos,
      BlockState state,
      int color
   ) {
      VertexRendering.drawOutline(
         matrices,
         vertexConsumer,
         state.getOutlineShape(this.world, pos, ShapeContext.of(entity)),
         pos.getX() - cameraX,
         pos.getY() - cameraY,
         pos.getZ() - cameraZ,
         color
      );
   }

   public void updateBlock(BlockView world, BlockPos pos, BlockState oldState, BlockState newState, int flags) {
      this.scheduleSectionRender(pos, (flags & 8) != 0);
   }

   private void scheduleSectionRender(BlockPos pos, boolean important) {
      if (this.sodium$renderer != null) {
         this.sodium$renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
         return;
      }
      for (int i = pos.getZ() - 1; i <= pos.getZ() + 1; i++) {
         for (int j = pos.getX() - 1; j <= pos.getX() + 1; j++) {
            for (int k = pos.getY() - 1; k <= pos.getY() + 1; k++) {
               this.scheduleChunkRender(ChunkSectionPos.getSectionCoord(j), ChunkSectionPos.getSectionCoord(k), ChunkSectionPos.getSectionCoord(i), important);
            }
         }
      }
   }

   public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (this.sodium$renderer != null) {
         this.sodium$renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
         return;
      }
      for (int i = minZ - 1; i <= maxZ + 1; i++) {
         for (int j = minX - 1; j <= maxX + 1; j++) {
            for (int k = minY - 1; k <= maxY + 1; k++) {
               this.scheduleChunkRender(ChunkSectionPos.getSectionCoord(j), ChunkSectionPos.getSectionCoord(k), ChunkSectionPos.getSectionCoord(i));
            }
         }
      }
   }

   public void scheduleBlockRerenderIfNeeded(BlockPos pos, BlockState old, BlockState updated) {
      if (this.client.getBakedModelManager().shouldRerender(old, updated)) {
         this.scheduleBlockRenders(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
      }
   }

   public void scheduleChunkRenders3x3x3(int x, int y, int z) {
      this.sodium$renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
   }

   public void scheduleChunkRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      for (int i = minZ; i <= maxZ; i++) {
         for (int j = minX; j <= maxX; j++) {
            for (int k = minY; k <= maxY; k++) {
               this.scheduleChunkRender(j, k, i);
            }
         }
      }
   }

   public void scheduleChunkRender(int chunkX, int chunkY, int chunkZ) {
      this.scheduleChunkRender(chunkX, chunkY, chunkZ, false);
   }

   private void scheduleChunkRender(int x, int y, int z, boolean important) {
      this.sodium$renderer.scheduleRebuildForChunk(x, y, z, important);
   }

   public void onChunkUnload(long sectionPos) {
      ChunkBuilder.BuiltChunk builtChunk = this.chunks.getRenderedChunk(sectionPos);
      if (builtChunk != null) {
         this.chunkRenderingDataPreparer.schedulePropagationFrom(builtChunk);
      }
   }

   public void addParticle(ParticleEffect parameters, boolean force, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
      this.addParticle(parameters, force, false, x, y, z, velocityX, velocityY, velocityZ);
   }

   public void addParticle(
      ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal, double x, double y, double z, double velocityX, double velocityY, double velocityZ
   ) {
      try {
         this.spawnParticle(parameters, force, canSpawnOnMinimal, x, y, z, velocityX, velocityY, velocityZ);
      } catch (Throwable throwable) {
         CrashReport crashReport = CrashReport.create(throwable, "Exception while adding particle");
         CrashReportSection crashReportSection = crashReport.addElement("Particle being added");
         crashReportSection.add("ID", Registries.PARTICLE_TYPE.getId(parameters.getType()));
         crashReportSection.add(
            "Parameters", () -> ParticleTypes.TYPE_CODEC.encodeStart(this.world.getRegistryManager().getOps(NbtOps.INSTANCE), parameters).toString()
         );
         crashReportSection.add("Position", () -> CrashReportSection.createPositionString(this.world, x, y, z));
         throw new CrashException(crashReport);
      }
   }

   public <T extends ParticleEffect> void addParticle(T parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
      this.addParticle(parameters, parameters.getType().shouldAlwaysSpawn(), x, y, z, velocityX, velocityY, velocityZ);
   }

   @Nullable
   public Particle spawnParticle(ParticleEffect parameters, boolean force, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
      return this.spawnParticle(parameters, force, false, x, y, z, velocityX, velocityY, velocityZ);
   }

   @Nullable
   private Particle spawnParticle(
      ParticleEffect parameters, boolean force, boolean canSpawnOnMinimal, double x, double y, double z, double velocityX, double velocityY, double velocityZ
   ) {
      Camera camera = this.client.gameRenderer.getCamera();
      ParticlesMode particlesMode = this.getRandomParticleSpawnChance(canSpawnOnMinimal);
      if (force) {
         return this.client.particleManager.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
      } else if (camera.getPos().squaredDistanceTo(x, y, z) > 1024.0) {
         return null;
      } else {
         return particlesMode == ParticlesMode.MINIMAL ? null : this.client.particleManager.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
      }
   }

   private ParticlesMode getRandomParticleSpawnChance(boolean canSpawnOnMinimal) {
      ParticlesMode particlesMode = this.client.options.getParticles().getValue();
      if (canSpawnOnMinimal && particlesMode == ParticlesMode.MINIMAL && this.world.random.nextInt(10) == 0) {
         particlesMode = ParticlesMode.DECREASED;
      }

      if (particlesMode == ParticlesMode.DECREASED && this.world.random.nextInt(3) == 0) {
         particlesMode = ParticlesMode.MINIMAL;
      }

      return particlesMode;
   }

   public void setBlockBreakingInfo(int entityId, BlockPos pos, int stage) {
      if (stage >= 0 && stage < 10) {
         BlockBreakingInfo blockBreakingInfo = (BlockBreakingInfo)this.blockBreakingInfos.get(entityId);
         if (blockBreakingInfo != null) {
            this.removeBlockBreakingInfo(blockBreakingInfo);
         }

         if (blockBreakingInfo == null
            || blockBreakingInfo.getPos().getX() != pos.getX()
            || blockBreakingInfo.getPos().getY() != pos.getY()
            || blockBreakingInfo.getPos().getZ() != pos.getZ()) {
            blockBreakingInfo = new BlockBreakingInfo(entityId, pos);
            this.blockBreakingInfos.put(entityId, blockBreakingInfo);
         }

         blockBreakingInfo.setStage(stage);
         blockBreakingInfo.setLastUpdateTick(this.ticks);
         ((SortedSet)this.blockBreakingProgressions.computeIfAbsent(blockBreakingInfo.getPos().asLong(), l -> Sets.newTreeSet())).add(blockBreakingInfo);
      } else {
         BlockBreakingInfo blockBreakingInfo = (BlockBreakingInfo)this.blockBreakingInfos.remove(entityId);
         if (blockBreakingInfo != null) {
            this.removeBlockBreakingInfo(blockBreakingInfo);
         }
      }
   }

   public boolean isTerrainRenderComplete() {
      return this.sodium$renderer.isTerrainRenderComplete();
   }

   public void scheduleNeighborUpdates(ChunkPos chunkPos) {
      this.chunkRenderingDataPreparer.addNeighbors(chunkPos);
   }

   public void scheduleTerrainUpdate() {
      this.chunkRenderingDataPreparer.scheduleTerrainUpdate();
      this.cloudRenderer.scheduleTerrainUpdate();
      this.sodium$renderer.scheduleTerrainUpdate();
   }

   public void updateNoCullingBlockEntities(Collection<BlockEntity> removed, Collection<BlockEntity> added) {
      synchronized (this.noCullingBlockEntities) {
         this.noCullingBlockEntities.removeAll(removed);
         this.noCullingBlockEntities.addAll(added);
      }
   }

   public static int getLightmapCoordinates(BlockRenderView world, BlockPos pos) {
      return getLightmapCoordinates(world, world.getBlockState(pos), pos);
   }

   public static int getLightmapCoordinates(BlockRenderView world, BlockState state, BlockPos pos) {
      if (state.hasEmissiveLighting(world, pos)) {
         return 15728880;
      }

      int i = world.getLightLevel(LightType.SKY, pos);
      int j = world.getLightLevel(LightType.BLOCK, pos);
      int k = state.getLuminance();
      if (j < k) {
         j = k;
      }

      return i << 20 | j << 4;
   }

   public boolean isRenderingReady(BlockPos pos) {
      return this.sodium$renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
   }

   @Nullable
   public Framebuffer getEntityOutlinesFramebuffer() {
      return this.framebufferSet.entityOutlineFramebuffer != null ? this.framebufferSet.entityOutlineFramebuffer.get() : null;
   }

   @Nullable
   public Framebuffer getTranslucentFramebuffer() {
      return this.framebufferSet.translucentFramebuffer != null ? this.framebufferSet.translucentFramebuffer.get() : null;
   }

   @Nullable
   public Framebuffer getEntityFramebuffer() {
      return this.framebufferSet.itemEntityFramebuffer != null ? this.framebufferSet.itemEntityFramebuffer.get() : null;
   }

   @Nullable
   public Framebuffer getParticlesFramebuffer() {
      return this.framebufferSet.particlesFramebuffer != null ? this.framebufferSet.particlesFramebuffer.get() : null;
   }

   @Nullable
   public Framebuffer getWeatherFramebuffer() {
      return this.framebufferSet.weatherFramebuffer != null ? this.framebufferSet.weatherFramebuffer.get() : null;
   }

   @Nullable
   public Framebuffer getCloudsFramebuffer() {
      return this.framebufferSet.cloudsFramebuffer != null ? this.framebufferSet.cloudsFramebuffer.get() : null;
   }

   @Debug
   public ObjectArrayList<ChunkBuilder.BuiltChunk> getBuiltChunks() {
      return this.builtChunks;
   }

   @Debug
   public ChunkRenderingDataPreparer getChunkRenderingDataPreparer() {
      return this.chunkRenderingDataPreparer;
   }

   @Nullable
   public Frustum getCapturedFrustum() {
      return this.capturedFrustum;
   }

   public CloudRenderer getCloudRenderer() {
      return this.cloudRenderer;
   }

   @Nullable
   public Frustum getCullingFrustum() {
      return this.frustum;
   }
}
