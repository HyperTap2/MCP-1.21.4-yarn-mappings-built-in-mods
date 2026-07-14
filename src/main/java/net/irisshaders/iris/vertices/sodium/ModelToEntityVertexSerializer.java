package net.irisshaders.iris.vertices.sodium;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormalHelper;
import org.lwjgl.system.MemoryUtil;

public class ModelToEntityVertexSerializer implements VertexSerializer {
   public void serialize(long src, long dst, int vertexCount) {
      int quadCount = vertexCount / 4;

      for (int i = 0; i < quadCount; i++) {
         int normal = MemoryUtil.memGetInt(src + 32L);
         int tangent = NormalHelper.computeTangent(
            null,
            NormI8.unpackX(normal),
            NormI8.unpackY(normal),
            NormI8.unpackZ(normal),
            MemoryUtil.memGetFloat(src),
            MemoryUtil.memGetFloat(src + 4L),
            MemoryUtil.memGetFloat(src + 8L),
            MemoryUtil.memGetFloat(src + 16L),
            MemoryUtil.memGetFloat(src + 20L),
            MemoryUtil.memGetFloat(src + 36L),
            MemoryUtil.memGetFloat(src + 4L + 36L),
            MemoryUtil.memGetFloat(src + 8L + 36L),
            MemoryUtil.memGetFloat(src + 16L + 36L),
            MemoryUtil.memGetFloat(src + 20L + 36L),
            MemoryUtil.memGetFloat(src + 36L + 36L),
            MemoryUtil.memGetFloat(src + 4L + 36L + 36L),
            MemoryUtil.memGetFloat(src + 8L + 36L + 36L),
            MemoryUtil.memGetFloat(src + 16L + 36L + 36L),
            MemoryUtil.memGetFloat(src + 20L + 36L + 36L)
         );
         float midU = 0.0F;
         float midV = 0.0F;

         for (int vertex = 0; vertex < 4; vertex++) {
            midU += MemoryUtil.memGetFloat(src + 16L + 36 * vertex);
            midV += MemoryUtil.memGetFloat(src + 20L + 36 * vertex);
         }

         midU /= 4.0F;
         midV /= 4.0F;

         for (int j = 0; j < 4; j++) {
            MemoryIntrinsics.copyMemory(src, dst, 36);
            MemoryUtil.memPutShort(dst + 36L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
            MemoryUtil.memPutShort(dst + 38L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
            MemoryUtil.memPutShort(dst + 40L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
            MemoryUtil.memPutFloat(dst + 44L, midU);
            MemoryUtil.memPutFloat(dst + 48L, midV);
            MemoryUtil.memPutInt(dst + 52L, tangent);
            src += 36L;
            dst += IrisVertexFormats.ENTITY.getVertexSizeByte();
         }
      }
   }
}
