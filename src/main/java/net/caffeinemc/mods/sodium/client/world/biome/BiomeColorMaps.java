package net.caffeinemc.mods.sodium.client.world.biome;

import net.minecraft.world.biome.FoliageColors;
import net.minecraft.world.biome.GrassColors;

public class BiomeColorMaps {
   private static final int WIDTH = 256;
   private static final int HEIGHT = 256;
   private static final int INVALID_INDEX = -1;

   public static int getGrassColor(int index) {
      return index != -1 && index < GrassColors.colorMap.length ? GrassColors.colorMap[index] : GrassColors.getDefaultColor();
   }

   public static int getFoliageColor(int index) {
      return index != -1 && index < FoliageColors.colorMap.length ? FoliageColors.colorMap[index] : -12012264;
   }

   public static int getIndex(double temperature, double humidity) {
      humidity *= temperature;
      int x = (int)((1.0 - temperature) * 255.0);
      int y = (int)((1.0 - humidity) * 255.0);
      if (x < 0 || x >= 256) {
         return -1;
      } else {
         return y >= 0 && y < 256 ? y << 8 | x : -1;
      }
   }
}
