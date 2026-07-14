package net.minecraft.client.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAccelerationHolder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

public interface VertexConsumer extends IAccelerationHolder, IAcceleratedVertexConsumer {
   VertexConsumer vertex(float x, float y, float z);

   VertexConsumer color(int red, int green, int blue, int alpha);

   VertexConsumer texture(float u, float v);

   VertexConsumer overlay(int u, int v);

   VertexConsumer light(int u, int v);

   VertexConsumer normal(float x, float y, float z);

   default void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      this.vertex(x, y, z);
      this.color(color);
      this.texture(u, v);
      this.overlay(overlay);
      this.light(light);
      this.normal(normalX, normalY, normalZ);
   }

   default VertexConsumer color(float red, float green, float blue, float alpha) {
      return this.color((int)(red * 255.0F), (int)(green * 255.0F), (int)(blue * 255.0F), (int)(alpha * 255.0F));
   }

   default VertexConsumer color(int argb) {
      return this.color(ColorHelper.getRed(argb), ColorHelper.getGreen(argb), ColorHelper.getBlue(argb), ColorHelper.getAlpha(argb));
   }

   default VertexConsumer colorRgb(int rgb) {
      return this.color(ColorHelper.withAlpha(rgb, -1));
   }

   default VertexConsumer light(int uv) {
      return this.light(uv & 65535, uv >> 16 & 65535);
   }

   default VertexConsumer overlay(int uv) {
      return this.overlay(uv & 65535, uv >> 16 & 65535);
   }

   default void quad(MatrixStack.Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float f, int i, int j) {
      this.quad(matrixEntry, quad, new float[]{1.0F, 1.0F, 1.0F, 1.0F}, red, green, blue, f, new int[]{i, i, i, i}, j, false);
   }

   default void quad(
      MatrixStack.Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float f, int[] is, int i, boolean bl
   ) {
      if (AcceleratedRendering.isAvailable()) {
         this.acceleratedRendering$quad(matrixEntry, quad, brightnesses, red, green, blue, f, is, i, bl);
         return;
      }

      int[] js = quad.getVertexData();
      Vec3i vec3i = quad.getFace().getVector();
      Matrix4f matrix4f = matrixEntry.getPositionMatrix();
      Vector3f vector3f = matrixEntry.transformNormal(vec3i.getX(), vec3i.getY(), vec3i.getZ(), new Vector3f());
      int j = 8;
      int k = js.length / 8;
      int l = (int)(f * 255.0F);
      int m = quad.getLightEmission();
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         ByteBuffer byteBuffer = memoryStack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSizeByte());
         IntBuffer intBuffer = byteBuffer.asIntBuffer();

         for (int n = 0; n < k; n++) {
            intBuffer.clear();
            intBuffer.put(js, n * 8, 8);
            float g = byteBuffer.getFloat(0);
            float h = byteBuffer.getFloat(4);
            float o = byteBuffer.getFloat(8);
            float s;
            float t;
            float u;
            if (bl) {
               float p = byteBuffer.get(12) & 0xFF;
               float q = byteBuffer.get(13) & 0xFF;
               float r = byteBuffer.get(14) & 0xFF;
               s = p * brightnesses[n] * red;
               t = q * brightnesses[n] * green;
               u = r * brightnesses[n] * blue;
            } else {
               s = brightnesses[n] * red * 255.0F;
               t = brightnesses[n] * green * 255.0F;
               u = brightnesses[n] * blue * 255.0F;
            }

            int v = ColorHelper.getArgb(l, (int)s, (int)t, (int)u);
            int w = LightmapTextureManager.applyEmission(is[n], m);
            float r = byteBuffer.getFloat(16);
            float x = byteBuffer.getFloat(20);
            Vector3f vector3f2 = matrix4f.transformPosition(g, h, o, new Vector3f());
            this.vertex(vector3f2.x(), vector3f2.y(), vector3f2.z(), v, r, x, i, w, vector3f.x(), vector3f.y(), vector3f.z());
         }
      } catch (Throwable var35) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var34) {
               var35.addSuppressed(var34);
            }
         }

         throw var35;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }
   }

   private void acceleratedRendering$quad(
      MatrixStack.Entry matrixEntry,
      BakedQuad quad,
      float[] brightnesses,
      float red,
      float green,
      float blue,
      float alpha,
      int[] lights,
      int overlay,
      boolean useQuadColor
   ) {
      Vec3i face = quad.getFace().getVector();
      Matrix4f transform = matrixEntry.getPositionMatrix();
      Vector3f normal = matrixEntry.transformNormal(face.getX(), face.getY(), face.getZ(), new Vector3f());
      Vector3f position = new Vector3f();
      int vertexCount = quad.getVertexData().length / 8;
      int packedAlpha = (int)(alpha * 255.0F);

      for (int vertex = 0; vertex < vertexCount; vertex++) {
         float vertexRed;
         float vertexGreen;
         float vertexBlue;
         if (useQuadColor) {
            int bakedColor = quad.getColor(vertex);
            vertexRed = ColorABGR.unpackRed(bakedColor) * brightnesses[vertex] * red;
            vertexGreen = ColorABGR.unpackGreen(bakedColor) * brightnesses[vertex] * green;
            vertexBlue = ColorABGR.unpackBlue(bakedColor) * brightnesses[vertex] * blue;
         } else {
            vertexRed = brightnesses[vertex] * red * 255.0F;
            vertexGreen = brightnesses[vertex] * green * 255.0F;
            vertexBlue = brightnesses[vertex] * blue * 255.0F;
         }

         int color = ColorHelper.getArgb(packedAlpha, (int)vertexRed, (int)vertexGreen, (int)vertexBlue);
         int light = LightmapTextureManager.applyEmission(lights[vertex], quad.getLightEmission());
         transform.transformPosition(quad.getX(vertex), quad.getY(vertex), quad.getZ(vertex), position);
         this.vertex(
            position.x(),
            position.y(),
            position.z(),
            color,
            quad.getTexU(vertex),
            quad.getTexV(vertex),
            overlay,
            light,
            normal.x(),
            normal.y(),
            normal.z()
         );
      }
   }

   default VertexConsumer vertex(Vector3f vec) {
      return this.vertex(vec.x(), vec.y(), vec.z());
   }

   default VertexConsumer vertex(MatrixStack.Entry matrix, Vector3f vec) {
      return this.vertex(matrix, vec.x(), vec.y(), vec.z());
   }

   default VertexConsumer vertex(MatrixStack.Entry matrix, float x, float y, float z) {
      return this.vertex(matrix.getPositionMatrix(), x, y, z);
   }

   default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
      Vector3f vector3f = matrix.transformPosition(x, y, z, new Vector3f());
      return this.vertex(vector3f.x(), vector3f.y(), vector3f.z());
   }

   default VertexConsumer normal(MatrixStack.Entry matrix, float x, float y, float z) {
      Vector3f vector3f = matrix.transformNormal(x, y, z, new Vector3f());
      return this.normal(vector3f.x(), vector3f.y(), vector3f.z());
   }

   default VertexConsumer normal(MatrixStack.Entry matrix, Vector3f vec) {
      return this.normal(matrix, vec.x(), vec.y(), vec.z());
   }
}
