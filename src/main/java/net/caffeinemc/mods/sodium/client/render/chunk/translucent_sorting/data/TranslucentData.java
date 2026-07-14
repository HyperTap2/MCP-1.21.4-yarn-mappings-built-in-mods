package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class TranslucentData {
   public static final int INDICES_PER_QUAD = 6;
   public static final int VERTICES_PER_QUAD = 4;
   public static final int BYTES_PER_INDEX = 4;
   public static final int BYTES_PER_QUAD = 24;
   public final ChunkSectionPos sectionPos;

   TranslucentData(ChunkSectionPos sectionPos) {
      this.sectionPos = sectionPos;
   }

   public abstract SortType getSortType();

   public void prepareTrigger(boolean isAngleTrigger) {
   }

   public static int vertexCountToQuadCount(int vertexCount) {
      return vertexCount / 4;
   }

   public static int quadCountToIndexBytes(int quadCount) {
      return quadCount * 24;
   }

   public static int indexBytesToQuadCount(int indexBytes) {
      return indexBytes / 24;
   }

   public static void writeQuadVertexIndexes(IntBuffer intBuffer, int quadIndex) {
      int vertexOffset = quadIndex * 4;
      intBuffer.put(vertexOffset + 0);
      intBuffer.put(vertexOffset + 1);
      intBuffer.put(vertexOffset + 2);
      intBuffer.put(vertexOffset + 2);
      intBuffer.put(vertexOffset + 3);
      intBuffer.put(vertexOffset + 0);
   }

   public static void writeQuadVertexIndexes(IntBuffer intBuffer, int[] quadIndexes) {
      for (int quadIndexPos = 0; quadIndexPos < quadIndexes.length; quadIndexPos++) {
         writeQuadVertexIndexes(intBuffer, quadIndexes[quadIndexPos]);
      }
   }
}
