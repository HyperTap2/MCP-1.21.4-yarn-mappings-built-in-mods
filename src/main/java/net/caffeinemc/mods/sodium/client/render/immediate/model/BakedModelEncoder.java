package net.caffeinemc.mods.sodium.client.render.immediate.model;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.minecraft.client.util.math.MatrixStack.Entry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
   private static final boolean MULTIPLY_ALPHA = PlatformRuntimeInformation.getInstance().usesAlphaMultiplication();

   private static int mergeLighting(int stored, int calculated) {
      if (stored == 0) {
         return calculated;
      } else {
         int blockLight = Math.max(stored & 65535, calculated & 65535);
         int skyLight = Math.max(stored >> 16 & 65535, calculated >> 16 & 65535);
         return blockLight | skyLight << 16;
      }
   }

   public static void writeQuadVertices(VertexBufferWriter writer, Entry matrices, ModelQuadView quad, int color, int light, int overlay, boolean colorize) {
      Matrix3f matNormal = matrices.getNormalMatrix();
      Matrix4f matPosition = matrices.getPositionMatrix();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long buffer = stack.nmalloc(144);
         long ptr = buffer;

         for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);
            int newLight = mergeLighting(quad.getMaxLightQuad(i), light);
            int newColor = color;
            if (colorize) {
               newColor = ColorMixer.mulComponentWise(color, quad.getColor(i));
            }

            int normal = MatrixHelper.transformNormal(matNormal, matrices.canSkipNormalization, quad.getAccurateNormal(i));
            float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
            float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
            float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
            EntityVertex.write(ptr, xt, yt, zt, newColor, quad.getTexU(i), quad.getTexV(i), overlay, newLight, normal);
            ptr += 36L;
         }

         writer.push(stack, buffer, 4, EntityVertex.FORMAT);
      } catch (Throwable var25) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var24) {
               var25.addSuppressed(var24);
            }
         }

         throw var25;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public static void writeQuadVertices(
      VertexBufferWriter writer,
      Entry matrices,
      ModelQuadView quad,
      float r,
      float g,
      float b,
      float a,
      float[] brightnessTable,
      boolean colorize,
      int[] light,
      int overlay
   ) {
      Matrix3f matNormal = matrices.getNormalMatrix();
      Matrix4f matPosition = matrices.getPositionMatrix();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long buffer = stack.nmalloc(144);
         long ptr = buffer;

         for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);
            float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
            float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
            float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
            int normal = MatrixHelper.transformNormal(matNormal, matrices.canSkipNormalization, quad.getAccurateNormal(i));
            float brightness = brightnessTable[i];
            float fR;
            float fG;
            float fB;
            float fA;
            if (colorize) {
               int color = quad.getColor(i);
               float oR = ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color));
               float oG = ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color));
               float oB = ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color));
               fR = oR * brightness * r;
               fG = oG * brightness * g;
               fB = oB * brightness * b;
               if (MULTIPLY_ALPHA) {
                  float oA = ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(color));
                  fA = oA * a;
               } else {
                  fA = a;
               }
            } else {
               fR = brightness * r;
               fG = brightness * g;
               fB = brightness * b;
               fA = a;
            }

            int color = ColorABGR.pack(fR, fG, fB, fA);
            EntityVertex.write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, light[i], normal);
            ptr += 36L;
         }

         writer.push(stack, buffer, 4, EntityVertex.FORMAT);
      } catch (Throwable var37) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var36) {
               var37.addSuppressed(var36);
            }
         }

         throw var37;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public static boolean shouldMultiplyAlpha() {
      return MULTIPLY_ALPHA;
   }
}
