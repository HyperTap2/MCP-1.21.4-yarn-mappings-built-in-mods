package net.caffeinemc.mods.sodium.client.world.biome;

import net.caffeinemc.mods.sodium.client.world.BiomeSeedProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.SeedMixer;
import net.minecraft.world.chunk.ReadableContainer;

public class LevelBiomeSlice {
   private static final int SIZE = 12;
   private final RegistryEntry<Biome>[] biomes = new RegistryEntry[1728];
   private final boolean[] uniform = new boolean[1728];
   private final LevelBiomeSlice.BiasMap bias = new LevelBiomeSlice.BiasMap();
   private long biomeZoomSeed;
   private int blockX;
   private int blockY;
   private int blockZ;

   public void update(ClientWorld level, ChunkRenderContext context) {
      this.blockX = context.getOrigin().getMinX() - 16;
      this.blockY = context.getOrigin().getMinY() - 16;
      this.blockZ = context.getOrigin().getMinZ() - 16;
      this.biomeZoomSeed = BiomeSeedProvider.getBiomeZoomSeed(level);
      this.copyBiomeData(level, context);
      this.calculateBias();
      this.calculateUniform();
   }

   private void copyBiomeData(World level, ChunkRenderContext context) {
      Reference<Biome> defaultValue = level.getRegistryManager().getOrThrow(RegistryKeys.BIOME).getOrThrow(BiomeKeys.PLAINS);

      for (int sectionX = 0; sectionX < 3; sectionX++) {
         for (int sectionY = 0; sectionY < 3; sectionY++) {
            for (int sectionZ = 0; sectionZ < 3; sectionZ++) {
               this.copySectionBiomeData(context, sectionX, sectionY, sectionZ, defaultValue);
            }
         }
      }
   }

   private void copySectionBiomeData(ChunkRenderContext context, int sectionX, int sectionY, int sectionZ, RegistryEntry<Biome> defaultBiome) {
      ClonedChunkSection section = context.getSections()[LevelSlice.getLocalSectionIndex(sectionX, sectionY, sectionZ)];
      ReadableContainer<RegistryEntry<Biome>> biomeData = section.getBiomeData();

      for (int relCellX = 0; relCellX < 4; relCellX++) {
         for (int relCellY = 0; relCellY < 4; relCellY++) {
            for (int relCellZ = 0; relCellZ < 4; relCellZ++) {
               int cellX = sectionX * 4 + relCellX;
               int cellY = sectionY * 4 + relCellY;
               int cellZ = sectionZ * 4 + relCellZ;
               int idx = dataArrayIndex(cellX, cellY, cellZ);
               if (biomeData == null) {
                  this.biomes[idx] = defaultBiome;
               } else {
                  this.biomes[idx] = biomeData.get(relCellX, relCellY, relCellZ);
               }
            }
         }
      }
   }

   private void calculateUniform() {
      for (int cellX = 2; cellX < 10; cellX++) {
         for (int cellY = 2; cellY < 10; cellY++) {
            for (int cellZ = 2; cellZ < 10; cellZ++) {
               this.uniform[dataArrayIndex(cellX, cellY, cellZ)] = this.hasUniformNeighbors(cellX, cellY, cellZ);
            }
         }
      }
   }

   private void calculateBias() {
      int originX = this.blockX >> 2;
      int originY = this.blockY >> 2;
      int originZ = this.blockZ >> 2;
      long seed = this.biomeZoomSeed;

      for (int relCellX = 1; relCellX < 11; relCellX++) {
         int cellX = originX + relCellX;
         long seedX = SeedMixer.mixSeed(seed, cellX);

         for (int relCellY = 1; relCellY < 11; relCellY++) {
            int cellY = originY + relCellY;
            long seedXY = SeedMixer.mixSeed(seedX, cellY);

            for (int relCellZ = 1; relCellZ < 11; relCellZ++) {
               int cellZ = originZ + relCellZ;
               long seedXYZ = SeedMixer.mixSeed(seedXY, cellZ);
               this.calculateBias(dataArrayIndex(relCellX, relCellY, relCellZ), cellX, cellY, cellZ, seedXYZ);
            }
         }
      }
   }

   private void calculateBias(int cellIndex, int cellX, int cellY, int cellZ, long seed) {
      seed = SeedMixer.mixSeed(seed, cellX);
      seed = SeedMixer.mixSeed(seed, cellY);
      seed = SeedMixer.mixSeed(seed, cellZ);
      int gradX = getBias(seed);
      seed = SeedMixer.mixSeed(seed, this.biomeZoomSeed);
      int gradY = getBias(seed);
      seed = SeedMixer.mixSeed(seed, this.biomeZoomSeed);
      int gradZ = getBias(seed);
      this.bias.set(cellIndex, gradX, gradY, gradZ);
   }

   private boolean hasUniformNeighbors(int cellX, int cellY, int cellZ) {
      Biome biome = this.biomes[dataArrayIndex(cellX, cellY, cellZ)].value();
      int cellMinX = cellX - 1;
      int cellMaxX = cellX + 1;
      int cellMinY = cellY - 1;
      int cellMaxY = cellY + 1;
      int cellMinZ = cellZ - 1;
      int cellMaxZ = cellZ + 1;

      for (int adjCellX = cellMinX; adjCellX <= cellMaxX; adjCellX++) {
         for (int adjCellY = cellMinY; adjCellY <= cellMaxY; adjCellY++) {
            for (int adjCellZ = cellMinZ; adjCellZ <= cellMaxZ; adjCellZ++) {
               if (this.biomes[dataArrayIndex(adjCellX, adjCellY, adjCellZ)].value() != biome) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public RegistryEntry<Biome> getBiome(int blockX, int blockY, int blockZ) {
      int relBlockX = blockX - this.blockX;
      int relBlockY = blockY - this.blockY;
      int relBlockZ = blockZ - this.blockZ;
      int centerIndex = dataArrayIndex(BiomeCoords.fromBlock(relBlockX - 2), BiomeCoords.fromBlock(relBlockY - 2), BiomeCoords.fromBlock(relBlockZ - 2));
      return this.uniform[centerIndex] ? this.biomes[centerIndex] : this.getBiomeUsingVoronoi(relBlockX, relBlockY, relBlockZ);
   }

   private RegistryEntry<Biome> getBiomeUsingVoronoi(int blockX, int blockY, int blockZ) {
      int x = blockX - 2;
      int y = blockY - 2;
      int z = blockZ - 2;
      int originIntX = BiomeCoords.fromBlock(x);
      int originIntY = BiomeCoords.fromBlock(y);
      int originIntZ = BiomeCoords.fromBlock(z);
      float originFracX = BiomeCoords.method_39920(x) * 0.25F;
      float originFracY = BiomeCoords.method_39920(y) * 0.25F;
      float originFracZ = BiomeCoords.method_39920(z) * 0.25F;
      float closestDistance = Float.POSITIVE_INFINITY;
      int closestArrayIndex = 0;

      for (int index = 0; index < 8; index++) {
         boolean dirX = (index & 4) != 0;
         boolean dirY = (index & 2) != 0;
         boolean dirZ = (index & 1) != 0;
         int cellIntX = originIntX + (dirX ? 1 : 0);
         int cellIntY = originIntY + (dirY ? 1 : 0);
         int cellIntZ = originIntZ + (dirZ ? 1 : 0);
         float cellFracX = originFracX - (dirX ? 1.0F : 0.0F);
         float cellFracY = originFracY - (dirY ? 1.0F : 0.0F);
         float cellFracZ = originFracZ - (dirZ ? 1.0F : 0.0F);
         int biasIndex = dataArrayIndex(cellIntX, cellIntY, cellIntZ);
         float biasX = biasToVector(this.bias.getX(biasIndex));
         float biasY = biasToVector(this.bias.getY(biasIndex));
         float biasZ = biasToVector(this.bias.getZ(biasIndex));
         float distanceX = MathHelper.square(cellFracX + biasX);
         float distanceY = MathHelper.square(cellFracY + biasY);
         float distanceZ = MathHelper.square(cellFracZ + biasZ);
         float distance = distanceX + distanceY + distanceZ;
         if (closestDistance > distance) {
            closestArrayIndex = biasIndex;
            closestDistance = distance;
         }
      }

      return this.biomes[closestArrayIndex];
   }

   private static int dataArrayIndex(int cellX, int cellY, int cellZ) {
      return cellX * 12 * 12 + cellY * 12 + cellZ;
   }

   private static float biasToVector(int bias) {
      return bias * 9.765625E-4F * 0.9F;
   }

   private static int getBias(long l) {
      return (int)((l >> 24 & 1023L) - 512L);
   }

   public static class BiasMap {
      private final short[] data = new short[5184];

      public void set(int index, int x, int y, int z) {
         this.data[index * 3 + 0] = (short)x;
         this.data[index * 3 + 1] = (short)y;
         this.data[index * 3 + 2] = (short)z;
      }

      public int getX(int index) {
         return this.data[index * 3 + 0];
      }

      public int getY(int index) {
         return this.data[index * 3 + 1];
      }

      public int getZ(int index) {
         return this.data[index * 3 + 2];
      }
   }
}
