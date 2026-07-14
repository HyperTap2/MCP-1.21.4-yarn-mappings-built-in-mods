package me.flashyreese.mods.sodiumextra.compat;

import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import org.lwjgl.system.MemoryUtil;

public class ModelVertexToTerrainSerializer implements VertexSerializer {
   public void serialize(long src, long dst, int vertexCount) {
      for (int i = 0; i < vertexCount; i++) {
         MemoryUtil.memCopy(src, dst, 24L);
         MemoryUtil.memCopy(src + 28L, dst + 24L, 8L);
         src += 36L;
         dst += IrisCompat.getTerrainFormat().getVertexSizeByte();
      }
   }
}
