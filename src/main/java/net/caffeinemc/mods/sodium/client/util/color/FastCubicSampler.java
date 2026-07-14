package net.caffeinemc.mods.sodium.client.util.color;

import java.util.function.Function;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FastCubicSampler {
   private static final double[] DENSITY_CURVE = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};
   private static final int DIAMETER = 6;

   public static Vec3d sampleColor(Vec3d pos, FastCubicSampler.ColorFetcher colorFetcher, Function<Vec3d, Vec3d> transformer) {
      int intX = MathHelper.floor(pos.getX());
      int intY = MathHelper.floor(pos.getY());
      int intZ = MathHelper.floor(pos.getZ());
      int[] values = new int[216];

      for (int x = 0; x < 6; x++) {
         int blockX = intX - 2 + x;

         for (int y = 0; y < 6; y++) {
            int blockY = intY - 2 + y;

            for (int z = 0; z < 6; z++) {
               int blockZ = intZ - 2 + z;
               values[index(x, y, z)] = colorFetcher.fetch(blockX, blockY, blockZ);
            }
         }
      }

      if (isHomogenousArray(values)) {
         return transformer.apply(Vec3d.unpackRgb(values[0]));
      } else {
         double deltaX = pos.getX() - intX;
         double deltaY = pos.getY() - intY;
         double deltaZ = pos.getZ() - intZ;
         Vec3d sum = Vec3d.ZERO;
         double totalFactor = 0.0;

         for (int x = 0; x < 6; x++) {
            double densityX = MathHelper.lerp(deltaX, DENSITY_CURVE[x + 1], DENSITY_CURVE[x]);

            for (int y = 0; y < 6; y++) {
               double densityY = MathHelper.lerp(deltaY, DENSITY_CURVE[y + 1], DENSITY_CURVE[y]);

               for (int z = 0; z < 6; z++) {
                  double densityZ = MathHelper.lerp(deltaZ, DENSITY_CURVE[z + 1], DENSITY_CURVE[z]);
                  double factor = densityX * densityY * densityZ;
                  totalFactor += factor;
                  Vec3d color = transformer.apply(Vec3d.unpackRgb(values[index(x, y, z)]));
                  sum = sum.add(color.multiply(factor));
               }
            }
         }

         return sum.multiply(1.0 / totalFactor);
      }
   }

   private static int index(int x, int y, int z) {
      return 36 * z + 6 * y + x;
   }

   private static boolean isHomogenousArray(int[] arr) {
      int val = arr[0];

      for (int i = 1; i < arr.length; i++) {
         if (arr[i] != val) {
            return false;
         }
      }

      return true;
   }

   public interface ColorFetcher {
      int fetch(int var1, int var2, int var3);
   }
}
