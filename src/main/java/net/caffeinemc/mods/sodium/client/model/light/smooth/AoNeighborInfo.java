package net.caffeinemc.mods.sodium.client.model.light.smooth;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

enum AoNeighborInfo {
   DOWN(new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}, 0.5F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(z, 0.0F, 1.0F);
         float v = MathHelper.clamp(1.0F - x, 0.0F, 1.0F);
         out[0] = v * u;
         out[1] = v * (1.0F - u);
         out[2] = (1.0F - v) * (1.0F - u);
         out[3] = (1.0F - v) * u;
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[0] = lm0[0];
         lm1[1] = lm0[1];
         lm1[2] = lm0[2];
         lm1[3] = lm0[3];
         ao1[0] = ao0[0];
         ao1[1] = ao0[1];
         ao1[2] = ao0[2];
         ao1[3] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return MathHelper.clamp(y, 0.0F, 1.0F);
      }
   },
   UP(new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH}, 1.0F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(z, 0.0F, 1.0F);
         float v = MathHelper.clamp(x, 0.0F, 1.0F);
         out[0] = v * u;
         out[1] = v * (1.0F - u);
         out[2] = (1.0F - v) * (1.0F - u);
         out[3] = (1.0F - v) * u;
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[2] = lm0[0];
         lm1[3] = lm0[1];
         lm1[0] = lm0[2];
         lm1[1] = lm0[3];
         ao1[2] = ao0[0];
         ao1[3] = ao0[1];
         ao1[0] = ao0[2];
         ao1[1] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return 1.0F - MathHelper.clamp(y, 0.0F, 1.0F);
      }
   },
   NORTH(new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}, 0.8F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(1.0F - x, 0.0F, 1.0F);
         float v = MathHelper.clamp(y, 0.0F, 1.0F);
         out[0] = v * u;
         out[1] = v * (1.0F - u);
         out[2] = (1.0F - v) * (1.0F - u);
         out[3] = (1.0F - v) * u;
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[3] = lm0[0];
         lm1[0] = lm0[1];
         lm1[1] = lm0[2];
         lm1[2] = lm0[3];
         ao1[3] = ao0[0];
         ao1[0] = ao0[1];
         ao1[1] = ao0[2];
         ao1[2] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return MathHelper.clamp(z, 0.0F, 1.0F);
      }
   },
   SOUTH(new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP}, 0.8F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(y, 0.0F, 1.0F);
         float v = MathHelper.clamp(1.0F - x, 0.0F, 1.0F);
         out[0] = u * v;
         out[1] = (1.0F - u) * v;
         out[2] = (1.0F - u) * (1.0F - v);
         out[3] = u * (1.0F - v);
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[0] = lm0[0];
         lm1[1] = lm0[1];
         lm1[2] = lm0[2];
         lm1[3] = lm0[3];
         ao1[0] = ao0[0];
         ao1[1] = ao0[1];
         ao1[2] = ao0[2];
         ao1[3] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return 1.0F - MathHelper.clamp(z, 0.0F, 1.0F);
      }
   },
   WEST(new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}, 0.6F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(z, 0.0F, 1.0F);
         float v = MathHelper.clamp(y, 0.0F, 1.0F);
         out[0] = v * u;
         out[1] = v * (1.0F - u);
         out[2] = (1.0F - v) * (1.0F - u);
         out[3] = (1.0F - v) * u;
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[3] = lm0[0];
         lm1[0] = lm0[1];
         lm1[1] = lm0[2];
         lm1[2] = lm0[3];
         ao1[3] = ao0[0];
         ao1[0] = ao0[1];
         ao1[1] = ao0[2];
         ao1[2] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return MathHelper.clamp(x, 0.0F, 1.0F);
      }
   },
   EAST(new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}, 0.6F) {
      @Override
      public void calculateCornerWeights(float x, float y, float z, float[] out) {
         float u = MathHelper.clamp(z, 0.0F, 1.0F);
         float v = MathHelper.clamp(1.0F - y, 0.0F, 1.0F);
         out[0] = v * u;
         out[1] = v * (1.0F - u);
         out[2] = (1.0F - v) * (1.0F - u);
         out[3] = (1.0F - v) * u;
      }

      @Override
      public void mapCorners(int[] lm0, float[] ao0, int[] lm1, float[] ao1) {
         lm1[1] = lm0[0];
         lm1[2] = lm0[1];
         lm1[3] = lm0[2];
         lm1[0] = lm0[3];
         ao1[1] = ao0[0];
         ao1[2] = ao0[1];
         ao1[3] = ao0[2];
         ao1[0] = ao0[3];
      }

      @Override
      public float getDepth(float x, float y, float z) {
         return 1.0F - MathHelper.clamp(x, 0.0F, 1.0F);
      }
   };

   public final Direction[] faces;
   public final float strength;
   private static final AoNeighborInfo[] VALUES = values();

   private AoNeighborInfo(Direction[] directions, float strength) {
      this.faces = directions;
      this.strength = strength;
   }

   public abstract void calculateCornerWeights(float var1, float var2, float var3, float[] var4);

   public abstract void mapCorners(int[] var1, float[] var2, int[] var3, float[] var4);

   public abstract float getDepth(float var1, float var2, float var3);

   public static AoNeighborInfo get(Direction direction) {
      return VALUES[direction.getId()];
   }
}
