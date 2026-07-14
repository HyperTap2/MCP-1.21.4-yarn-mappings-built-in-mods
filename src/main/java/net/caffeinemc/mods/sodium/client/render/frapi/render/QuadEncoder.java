package net.caffeinemc.mods.sodium.client.render.frapi.render;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public class QuadEncoder {
   public static void writeQuadVertices(
      MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal
   ) {
      VertexBufferWriter writer = VertexConsumerUtils.convertOrLog(vertexConsumer);
      if (writer != null) {
         writeQuadVertices(quad, writer, overlay, matPosition, trustedNormals, matNormal);
      } else {
         writeQuadVerticesSlow(quad, vertexConsumer, overlay, matPosition, trustedNormals, matNormal);
      }
   }

   public static void writeQuadVertices(
      MutableQuadViewImpl quad, VertexBufferWriter writer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         long buffer = stack.nmalloc(144);
         long ptr = buffer;
         boolean useNormals = quad.hasVertexNormals();
         int normal = 0;
         if (useNormals) {
            quad.populateMissingNormals();
         } else {
            normal = MatrixHelper.transformNormal(matNormal, trustedNormals, quad.packedFaceNormal());
         }

         for (int i = 0; i < 4; i++) {
            float x = quad.x(i);
            float y = quad.y(i);
            float z = quad.z(i);
            float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
            float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
            float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
            if (useNormals) {
               normal = MatrixHelper.transformNormal(matNormal, trustedNormals, quad.packedNormal(i));
            }

            EntityVertex.write(ptr, xt, yt, zt, ColorARGB.toABGR(quad.color(i)), quad.u(i), quad.v(i), overlay, quad.lightmap(i), normal);
            ptr += 36L;
         }

         writer.push(stack, buffer, 4, EntityVertex.FORMAT);
      } catch (Throwable var21) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var20) {
               var21.addSuppressed(var20);
            }
         }

         throw var21;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void writeQuadVerticesSlow(
      MutableQuadViewImpl quad, VertexConsumer vertexConsumer, int overlay, Matrix4f matPosition, boolean trustedNormals, Matrix3f matNormal
   ) {
      boolean useNormals = quad.hasVertexNormals();
      float nxt = 0.0F;
      float nyt = 0.0F;
      float nzt = 0.0F;
      if (useNormals) {
         quad.populateMissingNormals();
      } else {
         Vector3f faceNormal = quad.faceNormal();
         float nx = faceNormal.x;
         float ny = faceNormal.y;
         float nz = faceNormal.z;
         nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
         nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
         nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);
         if (!trustedNormals) {
            float scalar = Math.invsqrt(Math.fma(nxt, nxt, Math.fma(nyt, nyt, nzt * nzt)));
            nxt *= scalar;
            nyt *= scalar;
            nzt *= scalar;
         }
      }

      for (int i = 0; i < 4; i++) {
         float x = quad.x(i);
         float y = quad.y(i);
         float z = quad.z(i);
         float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
         float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
         float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);
         vertexConsumer.vertex(xt, yt, zt);
         vertexConsumer.color(quad.color(i));
         vertexConsumer.texture(quad.u(i), quad.v(i));
         vertexConsumer.overlay(overlay);
         vertexConsumer.light(quad.lightmap(i));
         if (useNormals) {
            int packedNormal = quad.packedNormal(i);
            float nxx = NormI8.unpackX(packedNormal);
            float nyx = NormI8.unpackY(packedNormal);
            float nzx = NormI8.unpackZ(packedNormal);
            nxt = MatrixHelper.transformNormalX(matNormal, nxx, nyx, nzx);
            nyt = MatrixHelper.transformNormalY(matNormal, nxx, nyx, nzx);
            nzt = MatrixHelper.transformNormalZ(matNormal, nxx, nyx, nzx);
            if (!trustedNormals) {
               float scalar = Math.invsqrt(Math.fma(nxt, nxt, Math.fma(nyt, nyt, nzt * nzt)));
               nxt *= scalar;
               nyt *= scalar;
               nzt *= scalar;
            }
         }

         vertexConsumer.normal(nxt, nyt, nzt);
      }
   }
}
