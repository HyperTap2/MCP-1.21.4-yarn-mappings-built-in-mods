package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;

public class StaticTopoData extends MixedDirectionData {
   private Sorter sorterOnce;

   StaticTopoData(ChunkSectionPos sectionPos, int vertexCount, int quadCount) {
      super(sectionPos, vertexCount, quadCount);
   }

   @Override
   public SortType getSortType() {
      return SortType.STATIC_TOPO;
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

   public static StaticTopoData fromMesh(int vertexCount, TQuad[] quads, ChunkSectionPos sectionPos) {
      StaticSorter sorter = new StaticSorter(quads.length);
      StaticTopoData.QuadIndexConsumerIntoBuffer indexWriter = new StaticTopoData.QuadIndexConsumerIntoBuffer(sorter.getIntBuffer());
      if (!TopoGraphSorting.topoGraphSort(indexWriter, quads, null, null)) {
         sorter.getIndexBuffer().free();
         return null;
      } else {
         StaticTopoData staticTopoData = new StaticTopoData(sectionPos, vertexCount, quads.length);
         staticTopoData.sorterOnce = sorter;
         return staticTopoData;
      }
   }

   private record QuadIndexConsumerIntoBuffer(IntBuffer buffer) implements IntConsumer {
      @Override
      public void accept(int value) {
         TranslucentData.writeQuadVertexIndexes(this.buffer, value);
      }
   }
}
