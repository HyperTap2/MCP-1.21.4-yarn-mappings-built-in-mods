package net.caffeinemc.mods.sodium.api.math;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Direction;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MatrixHelper {
   public static int transformNormal(Matrix3f mat, boolean skipNormalization, float x, float y, float z) {
      float nxt = transformNormalX(mat, x, y, z);
      float nyt = transformNormalY(mat, x, y, z);
      float nzt = transformNormalZ(mat, x, y, z);
      if (!skipNormalization) {
         float scalar = Math.invsqrt(Math.fma(nxt, nxt, Math.fma(nyt, nyt, nzt * nzt)));
         nxt *= scalar;
         nyt *= scalar;
         nzt *= scalar;
      }

      return NormI8.pack(nxt, nyt, nzt);
   }

   public static int transformSafeNormal(Matrix3f mat, float x, float y, float z) {
      float nxt = transformNormalX(mat, x, y, z);
      float nyt = transformNormalY(mat, x, y, z);
      float nzt = transformNormalZ(mat, x, y, z);
      return NormI8.pack(nxt, nyt, nzt);
   }

   public static int transformNormal(Matrix3f mat, boolean skipNormalization, int norm) {
      float x = NormI8.unpackX(norm);
      float y = NormI8.unpackY(norm);
      float z = NormI8.unpackZ(norm);
      return transformNormal(mat, skipNormalization, x, y, z);
   }

   public static float transformNormalX(Matrix3f mat, float x, float y, float z) {
      return mat.m00() * x + (mat.m10() * y + mat.m20() * z);
   }

   public static float transformNormalY(Matrix3f mat, float x, float y, float z) {
      return mat.m01() * x + (mat.m11() * y + mat.m21() * z);
   }

   public static float transformNormalZ(Matrix3f mat, float x, float y, float z) {
      return mat.m02() * x + (mat.m12() * y + mat.m22() * z);
   }

   public static float transformPositionX(Matrix4f mat, float x, float y, float z) {
      return mat.m00() * x + (mat.m10() * y + (mat.m20() * z + mat.m30()));
   }

   public static float transformPositionY(Matrix4f mat, float x, float y, float z) {
      return mat.m01() * x + (mat.m11() * y + (mat.m21() * z + mat.m31()));
   }

   public static float transformPositionZ(Matrix4f mat, float x, float y, float z) {
      return mat.m02() * x + (mat.m12() * y + (mat.m22() * z + mat.m32()));
   }

   public static void rotateZYX(Entry matrices, float angleZ, float angleY, float angleX) {
      matrices.getPositionMatrix().rotateZYX(angleZ, angleY, angleX);
      matrices.getNormalMatrix().rotateZYX(angleZ, angleY, angleX);
   }

   public static int transformNormal(Matrix3f matrix, boolean skipNormalization, Direction direction) {
      float x;
      float y;
      float z;
      if (direction == Direction.DOWN) {
         x = -matrix.m10;
         y = -matrix.m11;
         z = -matrix.m12;
      } else if (direction == Direction.UP) {
         x = matrix.m10;
         y = matrix.m11;
         z = matrix.m12;
      } else if (direction == Direction.NORTH) {
         x = -matrix.m20;
         y = -matrix.m21;
         z = -matrix.m22;
      } else if (direction == Direction.SOUTH) {
         x = matrix.m20;
         y = matrix.m21;
         z = matrix.m22;
      } else if (direction == Direction.WEST) {
         x = -matrix.m00;
         y = -matrix.m01;
         z = -matrix.m02;
      } else {
         if (direction != Direction.EAST) {
            throw new IllegalArgumentException("An incorrect direction enum was provided..");
         }

         x = matrix.m00;
         y = matrix.m01;
         z = matrix.m02;
      }

      if (!skipNormalization) {
         float scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)));
         x *= scalar;
         y *= scalar;
         z *= scalar;
      }

      return NormI8.pack(x, y, z);
   }
}
