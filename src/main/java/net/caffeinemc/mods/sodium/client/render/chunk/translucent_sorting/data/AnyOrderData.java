package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;

public class AnyOrderData extends SplitDirectionData {
   private Sorter sorterOnce;

   AnyOrderData(ChunkSectionPos sectionPos, int[] vertexCounts, int quadCount) {
      super(sectionPos, vertexCounts, quadCount);
   }

   @Override
   public SortType getSortType() {
      return SortType.NONE;
   }

   @Override
   public Sorter getSorter() {
      Sorter sorter = this.sorterOnce;
      if (sorter == null) {
         throw new IllegalStateException("Sorter already used!");
      } else {
         this.sorterOnce = null;
         return sorter;
      }
   }

   public static AnyOrderData fromMesh(int[] vertexCounts, TQuad[] quads, ChunkSectionPos sectionPos) {
      AnyOrderData anyOrderData = new AnyOrderData(sectionPos, vertexCounts, quads.length);
      StaticSorter sorter = new StaticSorter(quads.length);
      anyOrderData.sorterOnce = sorter;
      IntBuffer indexBuffer = sorter.getIntBuffer();

      for (int vertexCount : vertexCounts) {
         if (vertexCount > 0) {
            int count = TranslucentData.vertexCountToQuadCount(vertexCount);

            for (int i = 0; i < count; i++) {
               TranslucentData.writeQuadVertexIndexes(indexBuffer, i);
            }
         }
      }

      return anyOrderData;
   }
}
