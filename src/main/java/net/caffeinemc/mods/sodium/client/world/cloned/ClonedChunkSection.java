package net.caffeinemc.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map.Entry;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.caffeinemc.mods.sodium.client.util.iterator.WrappedIterator;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.PalettedContainerROExtension;
import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.PalettedContainer.PaletteProvider;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClonedChunkSection {
   private static final ChunkNibbleArray DEFAULT_SKY_LIGHT_ARRAY = new ChunkNibbleArray(15);
   private static final ChunkNibbleArray DEFAULT_BLOCK_LIGHT_ARRAY = new ChunkNibbleArray(0);
   private static final PalettedContainer<BlockState> DEFAULT_STATE_CONTAINER = new PalettedContainer<>(
      Block.STATE_IDS, Blocks.AIR.getDefaultState(), PaletteProvider.BLOCK_STATE
   );
   private final ChunkSectionPos pos;
   @Nullable
   private final Int2ReferenceMap<BlockEntity> blockEntityMap;
   @Nullable
   private final Int2ReferenceMap<Object> blockEntityRenderDataMap;
   @Nullable
   private final ChunkNibbleArray[] lightDataArrays;
   @Nullable
   private final SodiumAuxiliaryLightManager auxLightManager;
   @Nullable
   private final ReadableContainer<BlockState> blockData;
   @Nullable
   private final ReadableContainer<RegistryEntry<Biome>> biomeData;
   private final SodiumModelDataContainer modelMap;
   private long lastUsedTimestamp = Long.MAX_VALUE;

   public ClonedChunkSection(World level, WorldChunk chunk, @Nullable ChunkSection section, ChunkSectionPos pos) {
      this.pos = pos;
      ReadableContainer<BlockState> blockData = null;
      ReadableContainer<RegistryEntry<Biome>> biomeData = null;
      Int2ReferenceMap<BlockEntity> blockEntityMap = null;
      Int2ReferenceMap<Object> blockEntityRenderDataMap = null;
      SodiumModelDataContainer modelMap = PlatformModelAccess.getInstance().getModelDataContainer(level, pos);
      this.auxLightManager = PlatformLevelAccess.INSTANCE.getLightManager(chunk, pos);
      if (section != null) {
         if (!section.isEmpty()) {
            if (!level.isDebugWorld()) {
               blockData = PalettedContainerROExtension.clone(section.getBlockStateContainer());
            } else {
               blockData = constructDebugWorldContainer(pos);
            }

            blockEntityMap = tryCopyBlockEntities(chunk, pos);
            if (blockEntityMap != null && PlatformBlockAccess.getInstance().platformHasBlockData()) {
               blockEntityRenderDataMap = copyBlockEntityRenderData(level, blockEntityMap);
            }
         }

         biomeData = PalettedContainerROExtension.clone(section.getBiomeContainer());
      }

      this.blockData = blockData;
      this.biomeData = biomeData;
      this.modelMap = modelMap;
      this.blockEntityMap = blockEntityMap;
      this.blockEntityRenderDataMap = blockEntityRenderDataMap;
      this.lightDataArrays = copyLightData(level, pos);
   }

   @NotNull
   private static PalettedContainer<BlockState> constructDebugWorldContainer(ChunkSectionPos pos) {
      if (pos.getY() != 3 && pos.getY() != 4) {
         return DEFAULT_STATE_CONTAINER;
      } else {
         PalettedContainer<BlockState> container = new PalettedContainer<>(Block.STATE_IDS, Blocks.AIR.getDefaultState(), PaletteProvider.BLOCK_STATE);
         if (pos.getY() == 3) {
            BlockState barrier = Blocks.BARRIER.getDefaultState();

            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  container.swapUnsafe(x, 12, z, barrier);
               }
            }
         } else if (pos.getY() == 4) {
            for (int z = 0; z < 16; z++) {
               for (int x = 0; x < 16; x++) {
                  container.swapUnsafe(
                     x, 6, z, DebugChunkGenerator.getBlockState(ChunkSectionPos.getOffsetPos(pos.getX(), x), ChunkSectionPos.getOffsetPos(pos.getZ(), z))
                  );
               }
            }
         }

         return container;
      }
   }

   @NotNull
   private static ChunkNibbleArray[] copyLightData(World level, ChunkSectionPos pos) {
      ChunkNibbleArray[] arrays = new ChunkNibbleArray[2];
      arrays[LightType.BLOCK.ordinal()] = copyLightArray(level, LightType.BLOCK, pos);
      if (level.getDimension().hasSkyLight()) {
         arrays[LightType.SKY.ordinal()] = copyLightArray(level, LightType.SKY, pos);
      }

      return arrays;
   }

   @NotNull
   private static ChunkNibbleArray copyLightArray(World level, LightType type, ChunkSectionPos pos) {
      ChunkNibbleArray array = level.getLightingProvider().get(type).getLightSection(pos);
      if (array == null) {
         array = switch (type) {
            case SKY -> DEFAULT_SKY_LIGHT_ARRAY;
            case BLOCK -> DEFAULT_BLOCK_LIGHT_ARRAY;
         };
      }

      return array;
   }

   @Nullable
   private static Int2ReferenceMap<BlockEntity> tryCopyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
      try {
         return copyBlockEntities(chunk, chunkCoord);
      } catch (WrappedIterator.Exception var3) {
         if (PlatformRuntimeInformation.getInstance().isModInLoadingList("entityculling")) {
            throw new RuntimeException(
               "Failed to iterate block entities! This is *very likely* the fault of the Entity Culling mod, and cannot be fixed by Sodium. See here for more details: https://link.caffeinemc.net/help/sodium/mod-issue/entity-culling/gh-2985",
               var3
            );
         } else {
            throw new RuntimeException(
               "Failed to iterate block entities! This is *very likely* the fault of another misbehaving mod, not Sodium. Please check your mods list.", var3
            );
         }
      }
   }

   @Nullable
   private static Int2ReferenceMap<BlockEntity> copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
      BlockBox box = new BlockBox(
         chunkCoord.getMinX(), chunkCoord.getMinY(), chunkCoord.getMinZ(), chunkCoord.getMaxX(), chunkCoord.getMaxY(), chunkCoord.getMaxZ()
      );
      Int2ReferenceOpenHashMap<BlockEntity> blockEntities = null;
      WrappedIterator<Entry<BlockPos, BlockEntity>> it = WrappedIterator.create(chunk.getBlockEntities().entrySet());

      while (it.hasNext()) {
         Entry<BlockPos, BlockEntity> entry = it.next();
         BlockPos pos = entry.getKey();
         BlockEntity entity = entry.getValue();
         if (box.contains(pos)) {
            if (blockEntities == null) {
               blockEntities = new Int2ReferenceOpenHashMap();
            }

            blockEntities.put(LevelSlice.getLocalBlockIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
         }
      }

      if (blockEntities != null) {
         blockEntities.trim();
      }

      return blockEntities;
   }

   @Nullable
   private static Int2ReferenceMap<Object> copyBlockEntityRenderData(World level, Int2ReferenceMap<BlockEntity> blockEntities) {
      Int2ReferenceOpenHashMap<Object> blockEntityRenderDataMap = null;
      ObjectIterator var3 = Int2ReferenceMaps.fastIterable(blockEntities).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.ints.Int2ReferenceMap.Entry<BlockEntity> entry = (it.unimi.dsi.fastutil.ints.Int2ReferenceMap.Entry<BlockEntity>)var3.next();
         Object data = PlatformLevelAccess.getInstance().getBlockEntityData((BlockEntity)entry.getValue());
         if (data != null) {
            if (blockEntityRenderDataMap == null) {
               blockEntityRenderDataMap = new Int2ReferenceOpenHashMap();
            }

            blockEntityRenderDataMap.put(entry.getIntKey(), data);
         }
      }

      if (blockEntityRenderDataMap != null) {
         blockEntityRenderDataMap.trim();
      }

      return blockEntityRenderDataMap;
   }

   public ChunkSectionPos getPosition() {
      return this.pos;
   }

   @Nullable
   public ReadableContainer<BlockState> getBlockData() {
      return this.blockData;
   }

   @Nullable
   public ReadableContainer<RegistryEntry<Biome>> getBiomeData() {
      return this.biomeData;
   }

   @Nullable
   public Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
      return this.blockEntityMap;
   }

   @Nullable
   public Int2ReferenceMap<Object> getBlockEntityRenderDataMap() {
      return this.blockEntityRenderDataMap;
   }

   public SodiumModelDataContainer getModelMap() {
      return this.modelMap;
   }

   @Nullable
   public ChunkNibbleArray getLightArray(LightType lightType) {
      return this.lightDataArrays[lightType.ordinal()];
   }

   public long getLastUsedTimestamp() {
      return this.lastUsedTimestamp;
   }

   public void setLastUsedTimestamp(long timestamp) {
      this.lastUsedTimestamp = timestamp;
   }

   public SodiumAuxiliaryLightManager getAuxLightManager() {
      return this.auxLightManager;
   }
}
