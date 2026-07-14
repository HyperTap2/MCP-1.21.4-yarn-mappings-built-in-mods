package net.caffeinemc.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTracker;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class SodiumWorldRenderer {
   private final MinecraftClient client;
   private ClientWorld level;
   private int renderDistance;
   private Vector3d lastCameraPos;
   private double lastCameraPitch;
   private double lastCameraYaw;
   private float lastFogDistance;
   private Matrix4f lastProjectionMatrix;
   private boolean useEntityCulling;
   private RenderSectionManager renderSectionManager;
   private static boolean iris$renderLightsOnly;
   private static int iris$renderedBlockEntities;
   private float iris$lastSunAngle;
   private static final double MAX_ENTITY_CHECK_VOLUME = 61440.0;

   static {
      ShadowRenderingState.setBlockEntityRenderFunction(
         (shadowRenderer, bufferSource, modelView, camera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, lightsOnly) -> {
            iris$renderLightsOnly = lightsOnly;
            iris$renderedBlockEntities = 0;

            try {
               SodiumWorldRenderer.instance()
                  .renderBlockEntities(
                     modelView,
                     bufferSource,
                     ((LevelRendererAccessor)MinecraftClient.getInstance().worldRenderer).getField_20950(),
                     camera,
                     tickDelta
                  );
               return iris$renderedBlockEntities;
            } finally {
               iris$renderLightsOnly = false;
               iris$renderedBlockEntities = 0;
            }
         }
      );
   }

   public static SodiumWorldRenderer instance() {
      SodiumWorldRenderer instance = instanceNullable();
      if (instance == null) {
         throw new IllegalStateException("No renderer attached to active level");
      } else {
         return instance;
      }
   }

   public static SodiumWorldRenderer instanceNullable() {
      return MinecraftClient.getInstance().worldRenderer instanceof LevelRendererExtension extension ? extension.sodium$getWorldRenderer() : null;
   }

   public SodiumWorldRenderer(MinecraftClient client) {
      this.client = client;
   }

   public void setLevel(ClientWorld level) {
      if (this.level != level) {
         if (this.level != null) {
            this.unloadLevel();
         }

         if (level != null) {
            this.loadLevel(level);
         }
      }
   }

   private void loadLevel(ClientWorld level) {
      this.level = level;

      try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
         this.initRenderer(commandList);
      }
   }

   private void unloadLevel() {
      if (this.renderSectionManager != null) {
         this.renderSectionManager.destroy();
         this.renderSectionManager = null;
      }

      this.level = null;
   }

   public int getVisibleChunkCount() {
      return this.renderSectionManager.getVisibleChunkCount();
   }

   public void scheduleTerrainUpdate() {
      if (this.renderSectionManager != null) {
         this.renderSectionManager.markGraphDirty();
      }
   }

   public boolean isTerrainRenderComplete() {
      return this.renderSectionManager.getBuilder().isBuildQueueEmpty();
   }

   public void setupTerrain(Camera camera, Viewport viewport, Fog fogParameters, boolean spectator, boolean updateChunksImmediately) {
      NativeBuffer.reclaim(false);
      this.processChunkEvents();
      this.useEntityCulling = SodiumClientMod.options().performance.useEntityCulling;
      if (this.client.options.getClampedViewDistance() != this.renderDistance) {
         this.reload();
      }

      Profiler profiler = Profilers.get();
      profiler.push("camera_setup");
      ClientPlayerEntity player = this.client.player;
      if (player == null) {
         throw new IllegalStateException("Client instance has no active player entity");
      } else {
         Vec3d posRaw = camera.getPos();
         Vector3d pos = new Vector3d(posRaw.getX(), posRaw.getY(), posRaw.getZ());
         Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
         float pitch = camera.getPitch();
         float yaw = camera.getYaw();
         float fogDistance = RenderSystem.getShaderFog().end();
         if (this.lastCameraPos == null) {
            this.lastCameraPos = new Vector3d(pos);
         }

         if (this.lastProjectionMatrix == null) {
            this.lastProjectionMatrix = new Matrix4f(projectionMatrix);
         }

         boolean cameraLocationChanged = !pos.equals(this.lastCameraPos);
         boolean cameraAngleChanged = pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;
         boolean cameraProjectionChanged = !projectionMatrix.equals(this.lastProjectionMatrix);
         this.lastProjectionMatrix = projectionMatrix;
         this.lastCameraPitch = pitch;
         this.lastCameraYaw = yaw;
         if (cameraLocationChanged || cameraAngleChanged || cameraProjectionChanged) {
            this.renderSectionManager.markGraphDirty();
         }

         this.lastFogDistance = fogDistance;
         this.renderSectionManager.updateCameraState(pos, camera);
         if (cameraLocationChanged) {
            profiler.swap("translucent_triggering");
            this.renderSectionManager.processGFNIMovement(new CameraMovement(this.lastCameraPos, pos));
            this.lastCameraPos = new Vector3d(pos);
         }

         int maxChunkUpdates = updateChunksImmediately ? this.renderDistance : 1;

         for (int i = 0; i < maxChunkUpdates; i++) {
            if (this.iris$needsGraphUpdateInShadowPass()) {
               profiler.swap("chunk_render_lists");
               this.renderSectionManager.update(camera, viewport, fogParameters, spectator);
            }

            profiler.swap("chunk_update");
            this.renderSectionManager.cleanupAndFlip();
            this.renderSectionManager.updateChunks(updateChunksImmediately);
            profiler.swap("chunk_upload");
            this.renderSectionManager.uploadChunks();
            if (!this.iris$needsGraphUpdateAfterShadowPass()) {
               break;
            }
         }

         profiler.swap("chunk_render_tick");
         this.renderSectionManager.tickVisibleRenders();
         profiler.pop();
         Entity.setRenderDistanceMultiplier(
            MathHelper.clamp(this.client.options.getClampedViewDistance() / 8.0, 1.0, 2.5) * this.client.options.getEntityDistanceScaling().getValue()
         );
      }
   }

   private void processChunkEvents() {
      ChunkTracker tracker = ChunkTrackerHolder.get(this.level);
      tracker.forEachEvent(this.renderSectionManager::onChunkAdded, this.renderSectionManager::onChunkRemoved);
   }

   private boolean iris$needsGraphUpdateInShadowPass() {
      if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
         float sunAngle = MinecraftClient.getInstance().world.getSkyAngleRadians(CapturedRenderingState.INSTANCE.getTickDelta());
         if (this.iris$lastSunAngle != sunAngle) {
            this.iris$lastSunAngle = sunAngle;
            return true;
         }
      }

      return this.renderSectionManager.needsUpdate();
   }

   private boolean iris$needsGraphUpdateAfterShadowPass() {
      return !ShadowRenderingState.areShadowsCurrentlyBeingRendered() && this.renderSectionManager.needsUpdate();
   }

   public void drawChunkLayer(RenderLayer renderLayer, ChunkRenderMatrices matrices, double x, double y, double z) {
      if (renderLayer == RenderLayer.getSolid()) {
         this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.SOLID, x, y, z);
         this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.CUTOUT, x, y, z);
      } else if (renderLayer == RenderLayer.getTranslucent()) {
         this.renderSectionManager.renderLayer(matrices, DefaultTerrainRenderPasses.TRANSLUCENT, x, y, z);
      }
   }

   public void reload() {
      if (this.level != null) {
         try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
         }
      }
   }

   private void initRenderer(CommandList commandList) {
      if (this.renderSectionManager != null) {
         this.renderSectionManager.destroy();
         this.renderSectionManager = null;
      }

      this.renderDistance = this.client.options.getClampedViewDistance();
      this.renderSectionManager = new RenderSectionManager(this.level, this.renderDistance, commandList);
      ChunkTracker tracker = ChunkTrackerHolder.get(this.level);
      ChunkTracker.forEachChunk(tracker.getReadyChunks(), this.renderSectionManager::onChunkAdded);
   }

   public void renderBlockEntities(
      MatrixStack matrices,
      BufferBuilderStorage bufferBuilders,
      Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
      Camera camera,
      float tickDelta
   ) {
      Immediate immediate = bufferBuilders.getEntityVertexConsumers();
      Vec3d cameraPos = camera.getPos();
      double x = cameraPos.getX();
      double y = cameraPos.getY();
      double z = cameraPos.getZ();
      ClientPlayerEntity player = this.client.player;
      if (player == null) {
         throw new IllegalStateException("Client instance has no active player entity");
      } else {
         BlockEntityRenderDispatcher blockEntityRenderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher();
         this.renderBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, player);
         this.renderGlobalBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, player);
      }
   }

   private void renderBlockEntities(
      MatrixStack matrices,
      BufferBuilderStorage bufferBuilders,
      Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
      float tickDelta,
      Immediate immediate,
      double x,
      double y,
      double z,
      BlockEntityRenderDispatcher blockEntityRenderer,
      ClientPlayerEntity player
   ) {
      for (ChunkRenderList renderList : this.renderSectionManager.getRenderLists()) {
         RenderRegion renderRegion = renderList.getRegion();
         ByteIterator renderSectionIterator = renderList.sectionsWithEntitiesIterator();
         if (renderSectionIterator != null) {
            while (renderSectionIterator.hasNext()) {
               int renderSectionId = renderSectionIterator.nextByteAsInt();
               RenderSection renderSection = renderRegion.getSection(renderSectionId);
               BlockEntity[] blockEntities = renderSection.getCulledBlockEntities();
               if (blockEntities != null) {
                  for (BlockEntity blockEntity : blockEntities) {
                     renderBlockEntity(
                        matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity, player
                     );
                  }
               }
            }
         }
      }
   }

   private void renderGlobalBlockEntities(
      MatrixStack matrices,
      BufferBuilderStorage bufferBuilders,
      Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
      float tickDelta,
      Immediate immediate,
      double x,
      double y,
      double z,
      BlockEntityRenderDispatcher blockEntityRenderer,
      ClientPlayerEntity player
   ) {
      for (RenderSection renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
         BlockEntity[] blockEntities = renderSection.getGlobalBlockEntities();
         if (blockEntities != null) {
            for (BlockEntity blockEntity : blockEntities) {
               renderBlockEntity(
                  matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity, player
               );
            }
         }
      }
   }

   private static void renderBlockEntity(
      MatrixStack matrices,
      BufferBuilderStorage bufferBuilders,
      Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions,
      float tickDelta,
      Immediate immediate,
      double x,
      double y,
      double z,
      BlockEntityRenderDispatcher dispatcher,
      BlockEntity entity,
      ClientPlayerEntity player
   ) {
      if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
         if (iris$renderLightsOnly && entity.getCachedState().getLuminance() == 0) {
            return;
         }

         iris$renderedBlockEntities++;
      }

      BlockPos pos = entity.getPos();
      matrices.push();
      matrices.translate(pos.getX() - x, pos.getY() - y, pos.getZ() - z);
      VertexConsumerProvider consumer = immediate;
      SortedSet<BlockBreakingInfo> breakingInfo = (SortedSet<BlockBreakingInfo>)blockBreakingProgressions.get(pos.asLong());
      if (breakingInfo != null && !breakingInfo.isEmpty()) {
         int stage = breakingInfo.last().getStage();
         if (stage >= 0) {
            VertexConsumer bufferBuilder = bufferBuilders.getEffectVertexConsumers().getBuffer(ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(stage));
            Entry entry = matrices.peek();
            VertexConsumer transformer = new OverlayVertexConsumer(bufferBuilder, entry, 1.0F);
            consumer = layer -> layer.hasCrumbling() ? VertexConsumers.union(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
         }
      }

      dispatcher.render(entity, tickDelta, matrices, consumer);
      matrices.pop();
   }

   public void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
      for (ChunkRenderList renderList : this.renderSectionManager.getRenderLists()) {
         RenderRegion renderRegion = renderList.getRegion();
         ByteIterator renderSectionIterator = renderList.sectionsWithEntitiesIterator();
         if (renderSectionIterator != null) {
            while (renderSectionIterator.hasNext()) {
               int renderSectionId = renderSectionIterator.nextByteAsInt();
               RenderSection renderSection = renderRegion.getSection(renderSectionId);
               BlockEntity[] blockEntities = renderSection.getCulledBlockEntities();
               if (blockEntities != null) {
                  for (BlockEntity blockEntity : blockEntities) {
                     blockEntityConsumer.accept(blockEntity);
                  }
               }
            }
         }
      }

      for (RenderSection renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
         BlockEntity[] blockEntities = renderSection.getGlobalBlockEntities();
         if (blockEntities != null) {
            for (BlockEntity blockEntity : blockEntities) {
               blockEntityConsumer.accept(blockEntity);
            }
         }
      }
   }

   public <T extends Entity, S extends EntityRenderState> boolean isEntityVisible(EntityRenderer<T, S> renderer, T entity) {
      if (!this.useEntityCulling) {
         return true;
      } else if (!this.client.hasOutline(entity) && !entity.shouldRenderName()) {
         Box bb = renderer.sodium$getCullingBox(entity);
         double entityVolume = (bb.maxX - bb.minX) * (bb.maxY - bb.minY) * (bb.maxZ - bb.minZ);
         return entityVolume > 61440.0 ? true : this.isBoxVisible(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
      } else {
         return true;
      }
   }

   public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
      if (!(y2 < this.level.getBottomY() + 0.5) && !(y1 > this.level.getTopYInclusive() - 0.5)) {
         int minX = ChunkSectionPos.getSectionCoord(x1 - 0.5);
         int minY = ChunkSectionPos.getSectionCoord(y1 - 0.5);
         int minZ = ChunkSectionPos.getSectionCoord(z1 - 0.5);
         int maxX = ChunkSectionPos.getSectionCoord(x2 + 0.5);
         int maxY = ChunkSectionPos.getSectionCoord(y2 + 0.5);
         int maxZ = ChunkSectionPos.getSectionCoord(z2 + 0.5);

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               for (int y = minY; y <= maxY; y++) {
                  if (this.renderSectionManager.isSectionVisible(x, y, z)) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public String getChunksDebugString() {
      return String.format(
         "C: %d/%d D: %d", this.renderSectionManager.getVisibleChunkCount(), this.renderSectionManager.getTotalSections(), this.renderDistance
      );
   }

   public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
      this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
   }

   public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
      for (int chunkX = minX; chunkX <= maxX; chunkX++) {
         for (int chunkY = minY; chunkY <= maxY; chunkY++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
               this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
            }
         }
      }
   }

   public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
      this.renderSectionManager.scheduleRebuild(x, y, z, important);
   }

   public Collection<String> getDebugStrings() {
      return this.renderSectionManager.getDebugStrings();
   }

   public boolean isSectionReady(int x, int y, int z) {
      return this.renderSectionManager.isSectionBuilt(x, y, z);
   }
}
