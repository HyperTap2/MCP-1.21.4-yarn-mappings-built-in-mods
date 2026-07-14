package net.caffeinemc.mods.sodium.client.world;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelRenderHooks;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.caffeinemc.mods.sodium.client.world.biome.LevelBiomeSlice;
import net.caffeinemc.mods.sodium.client.world.biome.LevelColorCache;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LevelSlice implements BlockRenderView {
   private static final LightType[] LIGHT_TYPES = LightType.values();
   private static final int SECTION_BLOCK_COUNT = 4096;
   private static final int NEIGHBOR_BLOCK_RADIUS = 2;
   private static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUpToMultiple(2, 16) >> 4;
   private static final int SECTION_ARRAY_LENGTH = 1 + NEIGHBOR_CHUNK_RADIUS * 2;
   private static final int SECTION_ARRAY_SIZE = SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH;
   private static final int LOCAL_XYZ_BITS = 4;
   private static final BlockState EMPTY_BLOCK_STATE = Blocks.AIR.getDefaultState();
   private final ClientWorld level;
   private final LevelBiomeSlice biomeSlice;
   private final LevelColorCache biomeColors;
   private final BlockState[][] blockArrays;
   private final SodiumAuxiliaryLightManager[] auxLightManager;
   @Nullable
   private final ChunkNibbleArray[][] lightArrays;
   @Nullable
   private final Int2ReferenceMap<BlockEntity>[] blockEntityArrays;
   @Nullable
   private final Int2ReferenceMap<Object>[] blockEntityRenderDataArrays;
   private final SodiumModelDataContainer[] modelMapArrays;
   private int originBlockX;
   private int originBlockY;
   private int originBlockZ;
   private BlockBox volume;

   public static ChunkRenderContext prepare(World level, ChunkSectionPos pos, ClonedChunkSectionCache cache) {
      WorldChunk chunk = level.getChunk(pos.getX(), pos.getZ());
      ChunkSection section = chunk.getSectionArray()[level.sectionCoordToIndex(pos.getY())];
      if (section != null && !section.isEmpty()) {
         BlockBox box = new BlockBox(pos.getMinX() - 2, pos.getMinY() - 2, pos.getMinZ() - 2, pos.getMaxX() + 2, pos.getMaxY() + 2, pos.getMaxZ() + 2);
         int minChunkX = pos.getX() - NEIGHBOR_CHUNK_RADIUS;
         int minChunkY = pos.getY() - NEIGHBOR_CHUNK_RADIUS;
         int minChunkZ = pos.getZ() - NEIGHBOR_CHUNK_RADIUS;
         int maxChunkX = pos.getX() + NEIGHBOR_CHUNK_RADIUS;
         int maxChunkY = pos.getY() + NEIGHBOR_CHUNK_RADIUS;
         int maxChunkZ = pos.getZ() + NEIGHBOR_CHUNK_RADIUS;
         ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_ARRAY_SIZE];

         for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
               for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                  sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] = cache.acquire(chunkX, chunkY, chunkZ);
               }
            }
         }

         List<?> renderers = PlatformLevelRenderHooks.getInstance().retrieveChunkMeshAppenders(level, pos.getMinPos());
         return new ChunkRenderContext(pos, sections, box, renderers);
      } else {
         return null;
      }
   }

   public LevelSlice(ClientWorld level) {
      this.level = level;
      this.blockArrays = new BlockState[SECTION_ARRAY_SIZE][4096];
      this.lightArrays = new ChunkNibbleArray[SECTION_ARRAY_SIZE][LIGHT_TYPES.length];
      this.blockEntityArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
      this.blockEntityRenderDataArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
      this.auxLightManager = new SodiumAuxiliaryLightManager[SECTION_ARRAY_SIZE];
      this.modelMapArrays = new SodiumModelDataContainer[SECTION_ARRAY_SIZE];
      this.biomeSlice = new LevelBiomeSlice();
      this.biomeColors = new LevelColorCache(this.biomeSlice, MinecraftClient.getInstance().options.getBiomeBlendRadius().getValue());

      for (BlockState[] blockArray : this.blockArrays) {
         Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
      }
   }

   public void copyData(ChunkRenderContext context) {
      this.originBlockX = ChunkSectionPos.getBlockCoord(context.getOrigin().getX() - NEIGHBOR_CHUNK_RADIUS);
      this.originBlockY = ChunkSectionPos.getBlockCoord(context.getOrigin().getY() - NEIGHBOR_CHUNK_RADIUS);
      this.originBlockZ = ChunkSectionPos.getBlockCoord(context.getOrigin().getZ() - NEIGHBOR_CHUNK_RADIUS);
      this.volume = context.getVolume();

      for (int x = 0; x < SECTION_ARRAY_LENGTH; x++) {
         for (int y = 0; y < SECTION_ARRAY_LENGTH; y++) {
            for (int z = 0; z < SECTION_ARRAY_LENGTH; z++) {
               this.copySectionData(context, getLocalSectionIndex(x, y, z));
            }
         }
      }

      this.biomeSlice.update(this.level, context);
      this.biomeColors.update(context);
   }

   private void copySectionData(ChunkRenderContext context, int sectionIndex) {
      ClonedChunkSection section = context.getSections()[sectionIndex];
      Objects.requireNonNull(section, "Chunk section must be non-null");
      this.unpackBlockData(this.blockArrays[sectionIndex], context, section);
      this.lightArrays[sectionIndex][LightType.BLOCK.ordinal()] = section.getLightArray(LightType.BLOCK);
      this.lightArrays[sectionIndex][LightType.SKY.ordinal()] = section.getLightArray(LightType.SKY);
      this.blockEntityArrays[sectionIndex] = section.getBlockEntityMap();
      this.auxLightManager[sectionIndex] = section.getAuxLightManager();
      this.blockEntityRenderDataArrays[sectionIndex] = section.getBlockEntityRenderDataMap();
      this.modelMapArrays[sectionIndex] = section.getModelMap();
   }

   private void unpackBlockData(BlockState[] blockArray, ChunkRenderContext context, ClonedChunkSection section) {
      if (section.getBlockData() == null) {
         Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
      } else {
         PalettedContainerROExtension<BlockState> container = PalettedContainerROExtension.of(section.getBlockData());
         ChunkSectionPos sectionPos = section.getPosition();
         if (sectionPos.equals(context.getOrigin())) {
            container.sodium$unpack(blockArray);
         } else {
            BlockBox bounds = context.getVolume();
            int minBlockX = Math.max(bounds.getMinX(), sectionPos.getMinX());
            int maxBlockX = Math.min(bounds.getMaxX(), sectionPos.getMaxX());
            int minBlockY = Math.max(bounds.getMinY(), sectionPos.getMinY());
            int maxBlockY = Math.min(bounds.getMaxY(), sectionPos.getMaxY());
            int minBlockZ = Math.max(bounds.getMinZ(), sectionPos.getMinZ());
            int maxBlockZ = Math.min(bounds.getMaxZ(), sectionPos.getMaxZ());
            container.sodium$unpack(blockArray, minBlockX & 15, minBlockY & 15, minBlockZ & 15, maxBlockX & 15, maxBlockY & 15, maxBlockZ & 15);
         }
      }
   }

   public void reset() {
      for (int sectionIndex = 0; sectionIndex < SECTION_ARRAY_LENGTH; sectionIndex++) {
         Arrays.fill(this.lightArrays[sectionIndex], null);
         this.blockEntityArrays[sectionIndex] = null;
         this.auxLightManager[sectionIndex] = null;
         this.blockEntityRenderDataArrays[sectionIndex] = null;
      }
   }

   @NotNull
   @Override
   public BlockState getBlockState(BlockPos pos) {
      return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
   }

   public BlockState getBlockState(int blockX, int blockY, int blockZ) {
      if (!this.volume.contains(blockX, blockY, blockZ)) {
         return EMPTY_BLOCK_STATE;
      } else {
         int relBlockX = blockX - this.originBlockX;
         int relBlockY = blockY - this.originBlockY;
         int relBlockZ = blockZ - this.originBlockZ;
         return this.blockArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)][getLocalBlockIndex(
            relBlockX & 15, relBlockY & 15, relBlockZ & 15
         )];
      }
   }

   @NotNull
   @Override
   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   @Override
   public float getBrightness(Direction direction, boolean shaded) {
      return this.level.getBrightness(direction, shaded);
   }

   @NotNull
   @Override
   public LightingProvider getLightingProvider() {
      throw new UnsupportedOperationException();
   }

   @Override
   public int getLightLevel(LightType type, BlockPos pos) {
      if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
         return 0;
      } else {
         int relBlockX = pos.getX() - this.originBlockX;
         int relBlockY = pos.getY() - this.originBlockY;
         int relBlockZ = pos.getZ() - this.originBlockZ;
         ChunkNibbleArray lightArray = this.lightArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)][type.ordinal()];
         return lightArray == null ? 0 : lightArray.get(relBlockX & 15, relBlockY & 15, relBlockZ & 15);
      }
   }

   @Override
   public int getBaseLightLevel(BlockPos pos, int ambientDarkness) {
      if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
         return 0;
      } else {
         int relBlockX = pos.getX() - this.originBlockX;
         int relBlockY = pos.getY() - this.originBlockY;
         int relBlockZ = pos.getZ() - this.originBlockZ;
         ChunkNibbleArray[] lightArrays = this.lightArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];
         ChunkNibbleArray skyLightArray = lightArrays[LightType.SKY.ordinal()];
         ChunkNibbleArray blockLightArray = lightArrays[LightType.BLOCK.ordinal()];
         int localBlockX = relBlockX & 15;
         int localBlockY = relBlockY & 15;
         int localBlockZ = relBlockZ & 15;
         int skyLight = skyLightArray == null ? 0 : skyLightArray.get(localBlockX, localBlockY, localBlockZ) - ambientDarkness;
         int blockLight = blockLightArray == null ? 0 : blockLightArray.get(localBlockX, localBlockY, localBlockZ);
         return Math.max(blockLight, skyLight);
      }
   }

   @Override
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
   }

   public BlockEntity getBlockEntity(int blockX, int blockY, int blockZ) {
      if (!this.volume.contains(blockX, blockY, blockZ)) {
         return null;
      } else {
         int relBlockX = blockX - this.originBlockX;
         int relBlockY = blockY - this.originBlockY;
         int relBlockZ = blockZ - this.originBlockZ;
         Int2ReferenceMap<BlockEntity> blockEntities = this.blockEntityArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];
         return blockEntities == null ? null : (BlockEntity)blockEntities.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
      }
   }

   @Override
   public int getColor(BlockPos pos, ColorResolver colorResolver) {
      return this.biomeColors.getColor(colorResolver, pos.getX(), pos.getY(), pos.getZ());
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   @Override
   public int getBottomY() {
      return this.level.getBottomY();
   }

   @Nullable
   public Object getBlockEntityRenderData(BlockPos pos) {
      if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
         return null;
      } else {
         int relBlockX = pos.getX() - this.originBlockX;
         int relBlockY = pos.getY() - this.originBlockY;
         int relBlockZ = pos.getZ() - this.originBlockZ;
         Int2ReferenceMap<Object> blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(
            relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4
         )];
         return blockEntityRenderDataMap == null ? null : blockEntityRenderDataMap.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
      }
   }

   public SodiumModelData getPlatformModelData(BlockPos pos) {
      if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
         return SodiumModelData.EMPTY;
      } else {
         int relBlockX = pos.getX() - this.originBlockX;
         int relBlockY = pos.getY() - this.originBlockY;
         int relBlockZ = pos.getZ() - this.originBlockZ;
         SodiumModelDataContainer modelMap = this.modelMapArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];
         return modelMap.isEmpty() ? SodiumModelData.EMPTY : modelMap.getModelData(pos);
      }
   }

   public boolean hasBiomes() {
      return true;
   }

   public RegistryEntry<Biome> getBiomeFabric(BlockPos pos) {
      return this.biomeSlice.getBiome(pos.getX(), pos.getY(), pos.getZ());
   }

   public static int getLocalBlockIndex(int blockX, int blockY, int blockZ) {
      return blockY << 4 << 4 | blockZ << 4 | blockX;
   }

   public static int getLocalSectionIndex(int sectionX, int sectionY, int sectionZ) {
      return sectionY * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH + sectionZ * SECTION_ARRAY_LENGTH + sectionX;
   }

   @Nullable
   public Object getBlockEntityRenderAttachment(BlockPos pos) {
      if (!this.volume.contains(pos.getX(), pos.getY(), pos.getZ())) {
         return null;
      } else {
         int relBlockX = pos.getX() - this.originBlockX;
         int relBlockY = pos.getY() - this.originBlockY;
         int relBlockZ = pos.getZ() - this.originBlockZ;
         Int2ReferenceMap<Object> blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(
            relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4
         )];
         return blockEntityRenderDataMap == null ? null : blockEntityRenderDataMap.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
      }
   }
}
