package net.caffeinemc.mods.sodium.client.services.standalone;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AmbientOcclusionMode;
import net.caffeinemc.mods.sodium.client.services.FluidRendererFactory;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelRenderHooks;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TranslucentBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

public final class StandalonePlatform implements PlatformRuntimeInformation, PlatformModelAccess, PlatformLevelRenderHooks,
   PlatformLevelAccess, PlatformBlockAccess, FluidRendererFactory {
   private static final StandalonePlatform INSTANCE = new StandalonePlatform();
   private static final SodiumModelDataContainer EMPTY_MODEL_DATA = new SodiumModelDataContainer(Long2ObjectMaps.emptyMap());

   private StandalonePlatform() {
   }

   public static <T> T get(Class<T> type) {
      if (!type.isInstance(INSTANCE)) {
         throw new IllegalArgumentException("No standalone Sodium service for " + type.getName());
      }

      return type.cast(INSTANCE);
   }

   @Override
   public boolean isDevelopmentEnvironment() {
      return true;
   }

   @Override
   public Path getGameDirectory() {
      return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
   }

   @Override
   public Path getConfigDirectory() {
      return this.getGameDirectory().resolve("config");
   }

   @Override
   public boolean platformHasEarlyLoadingScreen() {
      return false;
   }

   @Override
   public boolean platformUsesRefmap() {
      return false;
   }

   @Override
   public boolean isModInLoadingList(String modId) {
      return false;
   }

   @Override
   public boolean usesAlphaMultiplication() {
      return false;
   }

   @Override
   public Iterable<RenderLayer> getModelRenderTypes(BlockRenderView level, BakedModel model, BlockState state, BlockPos pos, Random random, SodiumModelData data) {
      return Collections.singleton(net.minecraft.client.render.RenderLayers.getBlockLayer(state));
   }

   @Override
   public List<BakedQuad> getQuads(BlockRenderView level, BlockPos pos, BakedModel model, BlockState state, Direction face, Random random,
      RenderLayer renderType, SodiumModelData data) {
      return model.getQuads(state, face, random);
   }

   @Override
   public SodiumModelDataContainer getModelDataContainer(World level, ChunkSectionPos sectionPos) {
      return EMPTY_MODEL_DATA;
   }

   @Override
   public SodiumModelData getModelData(LevelSlice slice, BakedModel model, BlockState state, BlockPos pos, SodiumModelData data) {
      return SodiumModelData.EMPTY;
   }

   @Override
   public SodiumModelData getEmptyModelData() {
      return SodiumModelData.EMPTY;
   }

   @Override
   public void runChunkLayerEvents(RenderLayer layer, World level, WorldRenderer renderer, Matrix4f modelMatrix, Matrix4f projectionMatrix,
      int ticks, Camera camera, Frustum frustum) {
   }

   @Override
   public List<?> retrieveChunkMeshAppenders(World level, BlockPos origin) {
      return List.of();
   }

   @Override
   public void runChunkMeshAppenders(List<?> renderers, Function<RenderLayer, VertexConsumer> consumerFactory, LevelSlice slice) {
   }

   @Override
   public Object getBlockEntityData(BlockEntity blockEntity) {
      return null;
   }

   @Override
   public SodiumAuxiliaryLightManager getLightManager(WorldChunk chunk, ChunkSectionPos pos) {
      return null;
   }

   @Override
   public int getLightEmission(BlockState state, BlockRenderView level, BlockPos pos) {
      return state.getLuminance();
   }

   @Override
   public boolean shouldSkipRender(BlockView level, BlockState selfState, BlockState otherState, BlockPos selfPos, BlockPos otherPos, Direction facing) {
      return false;
   }

   @Override
   public boolean shouldShowFluidOverlay(BlockState state, BlockRenderView level, BlockPos pos, FluidState fluidState) {
      return state.getBlock() instanceof TranslucentBlock || state.getBlock() instanceof LeavesBlock;
   }

   @Override
   public boolean platformHasBlockData() {
      return false;
   }

   @Override
   public float getNormalVectorShade(ModelQuadView quad, BlockRenderView level, boolean shade) {
      float x = NormI8.unpackX(quad.getFaceNormal());
      float y = NormI8.unpackY(quad.getFaceNormal());
      float z = NormI8.unpackZ(quad.getFaceNormal());
      float sum = 0.0F;
      float divisor = 0.0F;
      for (Direction direction : Direction.values()) {
         float component = switch (direction.getAxis()) {
            case X -> x * direction.getDirection().offset();
            case Y -> y * direction.getDirection().offset();
            case Z -> z * direction.getDirection().offset();
         };
         if (component > 0.0F) {
            sum += component * level.getBrightness(direction, shade);
            divisor += component;
         }
      }
      return divisor == 0.0F ? 1.0F : sum / divisor;
   }

   @Override
   public AmbientOcclusionMode usesAmbientOcclusion(
      BakedModel model, BlockState state, SodiumModelData data, RenderLayer renderType, BlockRenderView level, BlockPos pos
   ) {
      return model.useAmbientOcclusion() ? AmbientOcclusionMode.DEFAULT : AmbientOcclusionMode.DISABLED;
   }

   @Override
   public boolean shouldBlockEntityGlow(BlockEntity blockEntity, ClientPlayerEntity player) {
      return false;
   }

   @Override
   public boolean shouldOccludeFluid(Direction direction, BlockState adjacentState, FluidState fluid) {
      return adjacentState.getFluidState().getFluid().matchesType(fluid.getFluid());
   }

   @Override
   public FluidRenderer createPlatformFluidRenderer(ColorProviderRegistry colors, LightPipelineProvider lighters) {
      return new StandaloneFluidRenderer(colors, lighters);
   }

   @Override
   public BlendedColorProvider<FluidState> getWaterColorProvider() {
      return new BlendedColorProvider<>() {
         @Override
         protected int getColor(LevelSlice slice, FluidState state, BlockPos pos) {
            return 0xFF000000 | BiomeColors.getWaterColor(slice, pos);
         }
      };
   }

   @Override
   public BlendedColorProvider<BlockState> getWaterBlockColorProvider() {
      return new BlendedColorProvider<>() {
         @Override
         protected int getColor(LevelSlice slice, BlockState state, BlockPos pos) {
            return 0xFF000000 | BiomeColors.getWaterColor(slice, pos);
         }
      };
   }

   private static final class StandaloneFluidRenderer extends FluidRenderer {
      private static final ColorProvider<FluidState> WHITE = (slice, pos, scratchPos, state, quad, output) -> Arrays.fill(output, -1);
      private final ColorProviderRegistry colors;
      private final DefaultFluidRenderer renderer;

      private StandaloneFluidRenderer(ColorProviderRegistry colors, LightPipelineProvider lighters) {
         this.colors = colors;
         this.renderer = new DefaultFluidRenderer(lighters);
      }

      @Override
      public void render(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos pos, BlockPos offset,
         TranslucentGeometryCollector collector, ChunkBuildBuffers buffers) {
         Material material = DefaultMaterials.forFluidState(fluidState);
         ChunkModelBuilder builder = buffers.get(material);
         boolean lava = fluidState.isIn(FluidTags.LAVA);
         BakedModel model = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(blockState);
         Sprite still = model.getParticleSprite();
         Sprite flow = (lava ? ModelBaker.LAVA_FLOW : ModelBaker.WATER_FLOW).getSprite();
         Sprite overlay = lava ? null : ModelBaker.WATER_OVERLAY.getSprite();
         ColorProvider<FluidState> provider = this.colors.getColorProvider(fluidState.getFluid());
         this.renderer.render(level, blockState, fluidState, pos, offset, collector, builder, material,
            provider == null ? WHITE : provider, new Sprite[]{still, flow, overlay});
      }
   }
}
