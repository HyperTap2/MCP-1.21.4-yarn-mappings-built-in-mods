package net.irisshaders.iris.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;
import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.mixinterface.ExtendedBiome;
import net.irisshaders.iris.parsing.BiomeCategories;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;

public class BiomeUniforms {
   private static final Object2IntMap<RegistryKey<Biome>> biomeMap = new Object2IntOpenHashMap();

   public static Object2IntMap<RegistryKey<Biome>> getBiomeMap() {
      return biomeMap;
   }

   public static void addBiomeUniforms(UniformHolder uniforms) {
      uniforms.uniform1i(
            UniformUpdateFrequency.PER_TICK,
            "biome",
            playerI(player -> biomeMap.getInt(player.getWorld().getBiome(player.getBlockPos()).getKey().orElse(null)))
         )
         .uniform1i(UniformUpdateFrequency.PER_TICK, "biome_category", playerI(player -> {
            RegistryEntry<Biome> holder = player.getWorld().getBiome(player.getBlockPos());
            ExtendedBiome extendedBiome = (ExtendedBiome)holder.value();
            if (extendedBiome.getBiomeCategory() == -1) {
               extendedBiome.setBiomeCategory(getBiomeCategory(holder).ordinal());
               return extendedBiome.getBiomeCategory();
            } else {
               return extendedBiome.getBiomeCategory();
            }
         }))
         .uniform1i(
            UniformUpdateFrequency.PER_TICK,
            "biome_precipitation",
            playerI(
               player -> {
                  Precipitation precipitation = ((Biome)player.getWorld().getBiome(player.getBlockPos()).value())
                     .getPrecipitation(player.getBlockPos(), player.getWorld().getSeaLevel());

                  return switch (precipitation) {
                     case NONE -> 0;
                     case RAIN -> 1;
                     case SNOW -> 2;
                     default -> throw new MatchException(null, null);
                  };
               }
            )
         )
         .uniform1f(
            UniformUpdateFrequency.PER_TICK,
            "rainfall",
            playerF(player -> ((ExtendedBiome)player.getWorld().getBiome(player.getBlockPos()).value()).getDownfall())
         )
         .uniform1f(
            UniformUpdateFrequency.PER_TICK,
            "temperature",
            playerF(player -> ((Biome)player.getWorld().getBiome(player.getBlockPos()).value()).getTemperature())
         );
   }

   private static BiomeCategories getBiomeCategory(RegistryEntry<Biome> holder) {
      if (holder.isIn(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
         return BiomeCategories.NONE;
      } else if (holder.isIn(BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE)) {
         return BiomeCategories.ICY;
      } else if (holder.isIn(BiomeTags.IS_HILL)) {
         return BiomeCategories.EXTREME_HILLS;
      } else if (holder.isIn(BiomeTags.IS_TAIGA)) {
         return BiomeCategories.TAIGA;
      } else if (holder.isIn(BiomeTags.IS_OCEAN)) {
         return BiomeCategories.OCEAN;
      } else if (holder.isIn(BiomeTags.IS_JUNGLE)) {
         return BiomeCategories.JUNGLE;
      } else if (holder.isIn(BiomeTags.IS_FOREST)) {
         return BiomeCategories.FOREST;
      } else if (holder.isIn(BiomeTags.IS_BADLANDS)) {
         return BiomeCategories.MESA;
      } else if (holder.isIn(BiomeTags.IS_NETHER)) {
         return BiomeCategories.NETHER;
      } else if (holder.isIn(BiomeTags.IS_END)) {
         return BiomeCategories.THE_END;
      } else if (holder.isIn(BiomeTags.IS_BEACH)) {
         return BiomeCategories.BEACH;
      } else if (holder.isIn(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE)) {
         return BiomeCategories.DESERT;
      } else if (holder.isIn(BiomeTags.IS_RIVER)) {
         return BiomeCategories.RIVER;
      } else if (holder.isIn(BiomeTags.HAS_CLOSER_WATER_FOG)) {
         return BiomeCategories.SWAMP;
      } else if (holder.isIn(BiomeTags.PLAYS_UNDERWATER_MUSIC)) {
         return BiomeCategories.UNDERGROUND;
      } else if (holder.isIn(BiomeTags.WITHOUT_ZOMBIE_SIEGES)) {
         return BiomeCategories.MUSHROOM;
      } else {
         return holder.isIn(BiomeTags.IS_MOUNTAIN) ? BiomeCategories.MOUNTAIN : BiomeCategories.PLAINS;
      }
   }

   static IntSupplier playerI(ToIntFunction<ClientPlayerEntity> function) {
      return () -> {
         ClientPlayerEntity player = MinecraftClient.getInstance().player;
         return player == null ? 0 : function.applyAsInt(player);
      };
   }

   static FloatSupplier playerF(BiomeUniforms.ToFloatFunction<ClientPlayerEntity> function) {
      return () -> {
         ClientPlayerEntity player = MinecraftClient.getInstance().player;
         return player == null ? 0.0F : function.applyAsFloat(player);
      };
   }

   @FunctionalInterface
   public interface ToFloatFunction<T> {
      float applyAsFloat(T var1);
   }
}
