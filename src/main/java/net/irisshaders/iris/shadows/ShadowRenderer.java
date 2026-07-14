package net.irisshaders.iris.shadows;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatBuffers;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.irisshaders.batchedentityrendering.impl.BatchingDebugMessageHelper;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.irisshaders.iris.shadows.frustum.CullEverythingFrustum;
import net.irisshaders.iris.shadows.frustum.FrustumHolder;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.advanced.ReversedAdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.BoxCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ShadowRenderer {
   public static boolean ACTIVE = false;
   public static List<BlockEntity> visibleBlockEntities;
   public static int renderDistance;
   public static Matrix4f MODELVIEW;
   public static Matrix4f PROJECTION;
   public static Frustum FRUSTUM;
   private final float halfPlaneLength;
   private final float nearPlane;
   private final float farPlane;
   private final float voxelDistance;
   private final float renderDistanceMultiplier;
   private final float entityShadowDistanceMultiplier;
   private final int resolution;
   private final float intervalSize;
   private final Float fov;
   private final ShadowRenderTargets targets;
   private final ShadowCullState packCullingState;
   private final ShadowCompositeRenderer compositeRenderer;
   private final boolean shouldRenderTerrain;
   private final boolean shouldRenderTranslucent;
   private final boolean shouldRenderEntities;
   private final boolean shouldRenderPlayer;
   private final boolean shouldRenderBlockEntities;
   private final boolean shouldRenderDH;
   private final float sunPathRotation;
   private final BufferBuilderStorage buffers;
   private final RenderBuffersExt renderBuffersExt;
   private final List<ShadowRenderer.MipmapPass> mipmapPasses = new ArrayList<>();
   private final String debugStringOverall;
   private final boolean separateHardwareSamplers;
   private final boolean shouldRenderLightBlockEntities;
   private final IrisRenderingPipeline pipeline;
   private boolean packHasVoxelization;
   private FrustumHolder terrainFrustumHolder;
   private FrustumHolder entityFrustumHolder;
   private String debugStringTerrain = "(unavailable)";
   private int renderedShadowEntities = 0;
   private int renderedShadowBlockEntities = 0;

   public ShadowRenderer(
      IrisRenderingPipeline pipeline,
      ProgramSource shadow,
      PackDirectives directives,
      ShadowRenderTargets shadowRenderTargets,
      ShadowCompositeRenderer compositeRenderer,
      CustomUniforms customUniforms,
      boolean separateHardwareSamplers
   ) {
      this.pipeline = pipeline;
      this.separateHardwareSamplers = separateHardwareSamplers;
      PackShadowDirectives shadowDirectives = directives.getShadowDirectives();
      this.halfPlaneLength = shadowDirectives.getDistance();
      this.nearPlane = shadowDirectives.getNearPlane();
      this.farPlane = shadowDirectives.getFarPlane();
      this.voxelDistance = shadowDirectives.getVoxelDistance();
      this.renderDistanceMultiplier = shadowDirectives.getDistanceRenderMul();
      this.entityShadowDistanceMultiplier = shadowDirectives.getEntityShadowDistanceMul();
      this.resolution = shadowDirectives.getResolution();
      this.intervalSize = shadowDirectives.getIntervalSize();
      this.shouldRenderTerrain = shadowDirectives.shouldRenderTerrain();
      this.shouldRenderTranslucent = shadowDirectives.shouldRenderTranslucent();
      this.shouldRenderEntities = shadowDirectives.shouldRenderEntities();
      this.shouldRenderPlayer = shadowDirectives.shouldRenderPlayer();
      this.shouldRenderBlockEntities = shadowDirectives.shouldRenderBlockEntities();
      this.shouldRenderLightBlockEntities = shadowDirectives.shouldRenderLightBlockEntities();
      this.shouldRenderDH = shadowDirectives.isDhShadowEnabled().orElse(false);
      this.compositeRenderer = compositeRenderer;
      this.debugStringOverall = "half plane = " + this.halfPlaneLength + " meters @ " + this.resolution + "x" + this.resolution;
      this.terrainFrustumHolder = new FrustumHolder();
      this.entityFrustumHolder = new FrustumHolder();
      this.fov = shadowDirectives.getFov();
      this.targets = shadowRenderTargets;
      if (shadow != null) {
         this.packHasVoxelization = shadow.getGeometrySource().isPresent();
         this.packCullingState = shadowDirectives.getCullingState();
      } else {
         this.packHasVoxelization = false;
         this.packCullingState = ShadowCullState.DEFAULT;
      }

      this.sunPathRotation = directives.getSunPathRotation();
      int processors = Runtime.getRuntime().availableProcessors();
      this.buffers = new BufferBuilderStorage(processors);
      if (this.buffers instanceof RenderBuffersExt) {
         this.renderBuffersExt = (RenderBuffersExt)this.buffers;
      } else {
         this.renderBuffersExt = null;
      }

      this.configureSamplingSettings(shadowDirectives);
   }

   public static MatrixStack createShadowModelView(float sunPathRotation, float intervalSize, float nearPlane, float farPlane) {
      Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
      double cameraX = cameraPos.x;
      double cameraY = cameraPos.y;
      double cameraZ = cameraPos.z;
      MatrixStack modelView = new MatrixStack();
      ShadowMatrices.createModelViewMatrix(modelView, getShadowAngle(), intervalSize, sunPathRotation, cameraX, cameraY, cameraZ, nearPlane, farPlane);
      return modelView;
   }

   private static ClientWorld getLevel() {
      return Objects.requireNonNull(MinecraftClient.getInstance().world);
   }

   private static float getSkyAngle() {
      return getLevel().getSkyAngle(CapturedRenderingState.INSTANCE.getTickDelta());
   }

   private static float getSunAngle() {
      float skyAngle = getSkyAngle();
      return skyAngle < 0.75F ? skyAngle + 0.25F : skyAngle - 0.75F;
   }

   private static float getShadowAngle() {
      float shadowAngle = getSunAngle();
      if (!CelestialUniforms.isDay()) {
         shadowAngle -= 0.5F;
      }

      return shadowAngle;
   }

   public void setUsesImages(boolean usesImages) {
      this.packHasVoxelization = this.packHasVoxelization || usesImages;
   }

   private void configureSamplingSettings(PackShadowDirectives shadowDirectives) {
      ImmutableList<PackShadowDirectives.DepthSamplingSettings> depthSamplingSettings = shadowDirectives.getDepthSamplingSettings();
      Int2ObjectMap<PackShadowDirectives.SamplingSettings> colorSamplingSettings = shadowDirectives.getColorSamplingSettings();
      RenderSystem.activeTexture(33988);
      this.configureDepthSampler(this.targets.getDepthTexture().getTextureId(), (PackShadowDirectives.DepthSamplingSettings)depthSamplingSettings.get(0));
      this.configureDepthSampler(
         this.targets.getDepthTextureNoTranslucents().getTextureId(), (PackShadowDirectives.DepthSamplingSettings)depthSamplingSettings.get(1)
      );

      for (int i = 0; i < this.targets.getNumColorTextures(); i++) {
         if (this.targets.get(i) != null) {
            int glTextureId = this.targets.get(i).getMainTexture();
            this.configureSampler(
               glTextureId, (PackShadowDirectives.SamplingSettings)colorSamplingSettings.computeIfAbsent(i, a -> new PackShadowDirectives.SamplingSettings())
            );
         }
      }

      RenderSystem.activeTexture(33984);
   }

   private void configureDepthSampler(int glTextureId, PackShadowDirectives.DepthSamplingSettings settings) {
      if (settings.getHardwareFiltering() && !this.separateHardwareSamplers) {
         IrisRenderSystem.texParameteri(glTextureId, 3553, 34892, 34894);
      }

      IrisRenderSystem.texParameteriv(glTextureId, 3553, 36422, new int[]{6403, 6403, 6403, 1});
      this.configureSampler(glTextureId, settings);
   }

   private void configureSampler(int glTextureId, PackShadowDirectives.SamplingSettings settings) {
      if (settings.getMipmap()) {
         int filteringMode = settings.getNearest() ? 9984 : 9987;
         this.mipmapPasses.add(new ShadowRenderer.MipmapPass(glTextureId, filteringMode));
      }

      if (!settings.getNearest()) {
         IrisRenderSystem.texParameteri(glTextureId, 3553, 10241, 9729);
         IrisRenderSystem.texParameteri(glTextureId, 3553, 10240, 9729);
      } else {
         IrisRenderSystem.texParameteri(glTextureId, 3553, 10241, 9728);
         IrisRenderSystem.texParameteri(glTextureId, 3553, 10240, 9728);
      }
   }

   private void generateMipmaps() {
      RenderSystem.activeTexture(33988);

      for (ShadowRenderer.MipmapPass mipmapPass : this.mipmapPasses) {
         this.setupMipmappingForTexture(mipmapPass.texture(), mipmapPass.targetFilteringMode());
      }

      RenderSystem.activeTexture(33984);
   }

   private void setupMipmappingForTexture(int texture, int filteringMode) {
      IrisRenderSystem.generateMipmaps(texture, 3553);
      IrisRenderSystem.texParameteri(texture, 3553, 10241, filteringMode);
   }

   private FrustumHolder createShadowFrustum(float renderMultiplier, FrustumHolder holder) {
      if ((this.packCullingState != ShadowCullState.DEFAULT || !this.packHasVoxelization) && this.packCullingState != ShadowCullState.DISTANCE) {
         boolean isReversed = this.packCullingState == ShadowCullState.REVERSED;
         if (isReversed && renderMultiplier < 0.0F) {
            renderMultiplier = 1.0F;
         }

         double distance = (isReversed ? this.voxelDistance : this.halfPlaneLength) * renderMultiplier;
         String setter = "(set by shader pack)";
         if (renderMultiplier < 0.0F) {
            distance = IrisVideoSettings.shadowDistance * 16;
            setter = "(set by user)";
         }

         String distanceInfo;
         BoxCuller boxCuller;
         if (distance >= MinecraftClient.getInstance().options.getClampedViewDistance() * 16 && !isReversed) {
            distanceInfo = "render distance = " + MinecraftClient.getInstance().options.getClampedViewDistance() * 16 + " blocks ";
            distanceInfo = distanceInfo
               + (MinecraftClient.getInstance().isInSingleplayer() ? "(capped by normal render distance)" : "(capped by normal/server render distance)");
            boxCuller = null;
         } else {
            distanceInfo = distance + " blocks " + setter;
            if (distance == 0.0 && !isReversed) {
               String cullingInfo = "no shadows rendered";
               holder.setInfo(new CullEverythingFrustum(), distanceInfo, cullingInfo);
            }

            boxCuller = new BoxCuller(distance);
         }

         String cullingInfo = (isReversed ? "Reversed" : "Advanced") + " Frustum Culling enabled";
         Vector4f shadowLightPosition = new CelestialUniforms(this.sunPathRotation).getShadowLightPositionInWorldSpace();
         Vector3f shadowLightVectorFromOrigin = new Vector3f(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());
         shadowLightVectorFromOrigin.normalize();
         Matrix4f projView = (this.shouldRenderDH && DHCompat.hasRenderingEnabled()
               ? DHCompat.getProjection()
               : CapturedRenderingState.INSTANCE.getGbufferProjection())
            .mul(CapturedRenderingState.INSTANCE.getGbufferModelView(), new Matrix4f());
         return isReversed
            ? holder.setInfo(
               new ReversedAdvancedShadowCullingFrustum(
                  projView, PROJECTION, shadowLightVectorFromOrigin, boxCuller, new BoxCuller(this.halfPlaneLength * renderMultiplier)
               ),
               distanceInfo,
               cullingInfo
            )
            : holder.setInfo(new AdvancedShadowCullingFrustum(projView, PROJECTION, shadowLightVectorFromOrigin, boxCuller), distanceInfo, cullingInfo);
      } else {
         double distance = this.halfPlaneLength * renderMultiplier;
         String reason;
         if (this.packCullingState == ShadowCullState.DISTANCE) {
            reason = "(set by shader pack)";
         } else {
            reason = "(voxelization detected)";
         }

         if (!(distance <= 0.0) && !(distance > MinecraftClient.getInstance().options.getClampedViewDistance() * 16)) {
            String distanceInfo = distance + " blocks (set by shader pack)";
            String cullingInfo = "distance only " + reason;
            BoxCuller boxCuller = new BoxCuller(distance);
            holder.setInfo(new BoxCullingFrustum(boxCuller), distanceInfo, cullingInfo);
            return holder;
         } else {
            String distanceInfo = "render distance = " + MinecraftClient.getInstance().options.getClampedViewDistance() * 16 + " blocks ";
            distanceInfo = distanceInfo
               + (MinecraftClient.getInstance().isInSingleplayer() ? "(capped by normal render distance)" : "(capped by normal/server render distance)");
            String cullingInfo = "disabled " + reason;
            return holder.setInfo(new NonCullingFrustum(), distanceInfo, cullingInfo);
         }
      }
   }

   public void setupShadowViewport() {
      RenderSystem.viewport(0, 0, this.resolution, this.resolution);
   }

   public void renderShadows(LevelRendererAccessor levelRenderer, Camera playerCamera) {
      if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) != 0) {
         MinecraftClient client = MinecraftClient.getInstance();
         Profiler profiler = Profilers.get();
         profiler.swap("shadows");
         ACTIVE = true;
         renderDistance = (int)(this.halfPlaneLength * this.renderDistanceMultiplier / 16.0F);
         if (this.renderDistanceMultiplier < 0.0F) {
            renderDistance = IrisVideoSettings.shadowDistance;
         }

         visibleBlockEntities = new ArrayList<>();
         BufferBuilderStorage playerBuffers = levelRenderer.getRenderBuffers();
         levelRenderer.setRenderBuffers(this.buffers);
         visibleBlockEntities = new ArrayList<>();
         this.setupShadowViewport();
         MatrixStack modelView = createShadowModelView(this.sunPathRotation, this.intervalSize, this.nearPlane, this.farPlane);
         MODELVIEW = new Matrix4f(modelView.peek().getPositionMatrix());
         RenderSystem.getModelViewStack().pushMatrix();
         RenderSystem.getModelViewStack().set(MODELVIEW);
         Matrix4f shadowProjection;
         if (this.fov != null) {
            shadowProjection = ShadowMatrices.createPerspectiveMatrix(this.fov);
         } else {
            shadowProjection = ShadowMatrices.createOrthoMatrix(
               this.halfPlaneLength,
               MathHelper.approximatelyEquals(this.nearPlane, -1.0F) ? -DHCompat.getRenderDistance() * 16 : this.nearPlane,
               MathHelper.approximatelyEquals(this.farPlane, -1.0F) ? DHCompat.getRenderDistance() * 16 : this.farPlane
            );
         }

         IrisRenderSystem.setShadowProjection(shadowProjection);
         PROJECTION = shadowProjection;
         profiler.push("terrain_setup");
         if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache)levelRenderer).saveState();
         }

         profiler.push("initialize frustum");
         this.terrainFrustumHolder = this.createShadowFrustum(this.renderDistanceMultiplier, this.terrainFrustumHolder);
         FRUSTUM = this.terrainFrustumHolder.getFrustum();
         Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
         double cameraX = cameraPos.x();
         double cameraY = cameraPos.y();
         double cameraZ = cameraPos.z();
         this.terrainFrustumHolder.getFrustum().setPosition(cameraX, cameraY, cameraZ);
         profiler.pop();
         boolean wasChunkCullingEnabled = client.chunkCullingEnabled;
         client.chunkCullingEnabled = false;
         ((WorldRenderer)levelRenderer).scheduleTerrainUpdate();
         levelRenderer.invokeSetupRender(playerCamera, this.terrainFrustumHolder.getFrustum(), false, false);
         client.chunkCullingEnabled = wasChunkCullingEnabled;
         profiler.swap("terrain");
         RenderSystem.disableCull();
         if (this.shouldRenderTerrain) {
            levelRenderer.invokeRenderSectionLayer(RenderLayer.getSolid(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderSectionLayer(RenderLayer.getCutout(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderSectionLayer(RenderLayer.getCutoutMipped(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
         }

         RenderSystem.viewport(0, 0, this.resolution, this.resolution);
         profiler.swap("entities");
         float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();
         boolean hasEntityFrustum = false;
         if (this.entityShadowDistanceMultiplier != 1.0F && !(this.entityShadowDistanceMultiplier < 0.0F)) {
            hasEntityFrustum = true;
            this.entityFrustumHolder = this.createShadowFrustum(this.renderDistanceMultiplier * this.entityShadowDistanceMultiplier, this.entityFrustumHolder);
         } else {
            this.entityFrustumHolder
               .setInfo(this.terrainFrustumHolder.getFrustum(), this.terrainFrustumHolder.getDistanceInfo(), this.terrainFrustumHolder.getCullingInfo());
         }

         Frustum entityShadowFrustum = this.entityFrustumHolder.getFrustum();
         entityShadowFrustum.setPosition(cameraX, cameraY, cameraZ);
         if (this.renderBuffersExt != null) {
            this.renderBuffersExt.beginLevelRendering();
         }

         if (this.buffers instanceof DrawCallTrackingRenderBuffers) {
            ((DrawCallTrackingRenderBuffers)this.buffers).resetDrawCounts();
         }

         Immediate bufferSource = this.buffers.getEntityVertexConsumers();
         EntityRenderDispatcher dispatcher = levelRenderer.getEntityRenderDispatcher();
         RenderSystem.getModelViewStack().identity();
         if (this.shouldRenderEntities) {
            this.renderedShadowEntities = this.renderEntities(
               levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ
            );
         } else if (this.shouldRenderPlayer) {
            this.renderedShadowEntities = this.renderPlayerEntity(
               levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ
            );
         }

         profiler.swap("build blockentities");
         if (this.shouldRenderBlockEntities) {
            this.renderedShadowBlockEntities = ShadowRenderingState.renderBlockEntities(
               this, this.buffers, modelView, playerCamera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, false
            );
         } else if (this.shouldRenderLightBlockEntities) {
            this.renderedShadowBlockEntities = ShadowRenderingState.renderBlockEntities(
               this, this.buffers, modelView, playerCamera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, true
            );
         }

         profiler.swap("draw entities");
         if (bufferSource instanceof FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource) {
            fullyBufferedMultiBufferSource.readyUp();
         }

         if (AcceleratedRendering.isAvailable()) {
            IrisCompatBuffers.drawBuffers();
         }
         bufferSource.draw();
         this.copyPreTranslucentDepth(levelRenderer);
         RenderSystem.getModelViewStack().set(MODELVIEW);
         profiler.swap("translucent terrain");
         if (this.shouldRenderTranslucent) {
            levelRenderer.invokeRenderSectionLayer(RenderLayer.getTranslucent(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
         }

         if (this.renderBuffersExt != null) {
            this.renderBuffersExt.endLevelRendering();
         }

         IrisRenderSystem.restorePlayerProjection();
         this.debugStringTerrain = ((WorldRenderer)levelRenderer).getChunksDebugString();
         profiler.swap("generate mipmaps");
         this.generateMipmaps();
         profiler.swap("restore gl state");
         RenderSystem.enableCull();
         MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
         RenderSystem.viewport(0, 0, client.getFramebuffer().textureWidth, client.getFramebuffer().textureHeight);
         if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache)levelRenderer).restoreState();
         }

         this.pipeline.removePhaseIfNeeded();
         GLDebug.pushGroup(901, "shadowcomp");
         this.compositeRenderer.renderAll();
         GLDebug.popGroup();
         levelRenderer.setRenderBuffers(playerBuffers);
         visibleBlockEntities = null;
         ACTIVE = false;
         RenderSystem.getModelViewStack().popMatrix();
         profiler.pop();
         profiler.swap("updatechunks");
      }
   }

   public int renderBlockEntities(
      BufferBuilderStorage bufferSource,
      MatrixStack modelView,
      Camera camera,
      double cameraX,
      double cameraY,
      double cameraZ,
      float tickDelta,
      boolean hasEntityFrustum,
      boolean lightsOnly
   ) {
      Profilers.get().push("build blockentities");
      int shadowBlockEntities = 0;
      BoxCuller culler = null;
      if (hasEntityFrustum) {
         culler = new BoxCuller(this.halfPlaneLength * (this.renderDistanceMultiplier * this.entityShadowDistanceMultiplier));
         culler.setPosition(cameraX, cameraY, cameraZ);
      }

      for (BlockEntity entity : visibleBlockEntities) {
         if (!lightsOnly || entity.getCachedState().getLuminance() != 0) {
            BlockPos pos = entity.getPos();
            if (!hasEntityFrustum || !culler.isCulled(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
               modelView.push();
               modelView.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);
               MinecraftClient.getInstance().getBlockEntityRenderDispatcher().render(entity, tickDelta, modelView, this.buffers.getEntityVertexConsumers());
               modelView.pop();
               shadowBlockEntities++;
            }
         }
      }

      Profilers.get().pop();
      return shadowBlockEntities;
   }

   private int renderEntities(
      LevelRendererAccessor levelRenderer,
      EntityRenderDispatcher dispatcher,
      Immediate bufferSource,
      MatrixStack modelView,
      float tickDelta,
      Frustum frustum,
      double cameraX,
      double cameraY,
      double cameraZ
   ) {
      Profilers.get().push("cull");
      List<Entity> renderedEntities = new ArrayList<>(32);

      for (Entity entity : getLevel().getEntities()) {
         if (dispatcher.shouldRender(entity, frustum, cameraX, cameraY, cameraZ) && !entity.isSpectator()) {
            renderedEntities.add(entity);
         }
      }

      Profilers.get().swap("sort");
      renderedEntities.sort(Comparator.comparingInt(entityx -> entityx.getType().hashCode()));
      Profilers.get().swap("build entity geometry");

      for (Entity entity : renderedEntities) {
         float realTickDelta = MinecraftClient.getInstance().world.getTickManager().shouldSkipTick(entity)
            ? tickDelta
            : CapturedRenderingState.INSTANCE.getRealTickDelta();
         levelRenderer.invokeRenderEntity(entity, cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
      }

      Profilers.get().pop();
      return renderedEntities.size();
   }

   private int renderPlayerEntity(
      LevelRendererAccessor levelRenderer,
      EntityRenderDispatcher dispatcher,
      Immediate bufferSource,
      MatrixStack modelView,
      float tickDelta,
      Frustum frustum,
      double cameraX,
      double cameraY,
      double cameraZ
   ) {
      Profilers.get().push("cull");
      Entity player = MinecraftClient.getInstance().player;
      int shadowEntities = 0;
      if (dispatcher.shouldRender(player, frustum, cameraX, cameraY, cameraZ) && !player.isSpectator()) {
         Profilers.get().swap("build geometry");
         if (!player.getPassengerList().isEmpty()) {
            for (int i = 0; i < player.getPassengerList().size(); i++) {
               float realTickDelta = MinecraftClient.getInstance().world.getTickManager().shouldSkipTick((Entity)player.getPassengerList().get(i))
                  ? tickDelta
                  : CapturedRenderingState.INSTANCE.getRealTickDelta();
               levelRenderer.invokeRenderEntity((Entity)player.getPassengerList().get(i), cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
               shadowEntities++;
            }
         }

         if (player.getVehicle() != null) {
            float realTickDelta = MinecraftClient.getInstance().world.getTickManager().shouldSkipTick(player.getVehicle())
               ? tickDelta
               : CapturedRenderingState.INSTANCE.getRealTickDelta();
            levelRenderer.invokeRenderEntity(player.getVehicle(), cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
            shadowEntities++;
         }

         float realTickDelta = MinecraftClient.getInstance().world.getTickManager().shouldSkipTick(player)
            ? tickDelta
            : CapturedRenderingState.INSTANCE.getRealTickDelta();
         levelRenderer.invokeRenderEntity(player, cameraX, cameraY, cameraZ, realTickDelta, modelView, bufferSource);
         shadowEntities++;
         Profilers.get().pop();
         return shadowEntities;
      } else {
         Profilers.get().pop();
         return 0;
      }
   }

   private void copyPreTranslucentDepth(LevelRendererAccessor levelRenderer) {
      Profilers.get().swap("translucent depth copy");
      this.targets.copyPreTranslucentDepth();
   }

   public void addDebugText(List<String> messages) {
      if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
         messages.add("[Iris] Shadow Maps: off, shadow distance 0");
      } else {
         if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
            messages.add("[Iris] Shadow Maps: " + this.debugStringOverall);
            messages.add(
               "[Iris] Shadow Distance Terrain: " + this.terrainFrustumHolder.getDistanceInfo() + " Entity: " + this.entityFrustumHolder.getDistanceInfo()
            );
            messages.add(
               "[Iris] Shadow Culling Terrain: " + this.terrainFrustumHolder.getCullingInfo() + " Entity: " + this.entityFrustumHolder.getCullingInfo()
            );
            messages.add("[Iris] Shadow Projection: " + this.getProjectionInfo());
            messages.add(
               "[Iris] Shadow Terrain: "
                  + this.debugStringTerrain
                  + (this.shouldRenderTerrain ? "" : " (no terrain) ")
                  + (this.shouldRenderTranslucent ? "" : "(no translucent)")
            );
            messages.add("[Iris] Shadow Entities: " + this.getEntitiesDebugString());
            messages.add("[Iris] Shadow Block Entities: " + this.getBlockEntitiesDebugString());
            if (this.buffers instanceof DrawCallTrackingRenderBuffers drawCallTracker && (this.shouldRenderEntities || this.shouldRenderPlayer)) {
               messages.add("[Iris] Shadow Entity Batching: " + BatchingDebugMessageHelper.getDebugMessage(drawCallTracker));
            }
         } else {
            messages.add("[Iris] Shadow info: " + this.debugStringTerrain);
            messages.add("[Iris] E: " + this.renderedShadowEntities);
            messages.add("[Iris] BE: " + this.renderedShadowBlockEntities);
         }
      }
   }

   private String getProjectionInfo() {
      return "Near: " + this.nearPlane + " Far: " + this.farPlane + " distance " + this.halfPlaneLength;
   }

   private String getEntitiesDebugString() {
      return !this.shouldRenderEntities && !this.shouldRenderPlayer
         ? "disabled by pack"
         : this.renderedShadowEntities + "/" + MinecraftClient.getInstance().world.getRegularEntityCount();
   }

   private String getBlockEntitiesDebugString() {
      return !this.shouldRenderBlockEntities && !this.shouldRenderLightBlockEntities ? "disabled by pack" : this.renderedShadowBlockEntities + "";
   }

   public void destroy() {
      ((MemoryTrackingRenderBuffers)this.buffers).freeAndDeleteBuffers();
   }

   private record MipmapPass(int texture, int targetFilteringMode) {
   }
}
