package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.util.math.ChunkSectionPos;

public class StaticNormalRelativeData extends SplitDirectionData {
   private Sorter sorterOnce;

   public StaticNormalRelativeData(ChunkSectionPos sectionPos, int[] vertexCounts, int quadCount) {
      super(sectionPos, vertexCounts, quadCount);
   }

   @Override
   public SortType getSortType() {
      return SortType.STATIC_NORMAL_RELATIVE;
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

   private static StaticNormalRelativeData fromDoubleUnaligned(int[] vertexCounts, TQuad[] quads, ChunkSectionPos sectionPos) {
      StaticNormalRelativeData snrData = new StaticNormalRelativeData(sectionPos, vertexCounts, quads.length);
      StaticSorter sorter = new StaticSorter(quads.length);
      snrData.sorterOnce = sorter;
      IntBuffer indexBuffer = sorter.getIntBuffer();
      if (quads.length <= 1) {
         TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
      } else if (RadixSort.useRadixSort(quads.length)) {
         int[] keys = new int[quads.length];

         for (int q = 0; q < quads.length; q++) {
            keys[q] = MathUtil.floatToComparableInt(quads[q].getAccurateDotProduct());
         }

         int[] indices = RadixSort.sort(keys);

         for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
         }
      } else {
         long[] sortData = new long[quads.length];

         for (int q = 0; q < quads.length; q++) {
            int dotProductComponent = MathUtil.floatToComparableInt(quads[q].getAccurateDotProduct());
            sortData[q] = (long)dotProductComponent << 32 | q;
         }

         Arrays.sort(sortData);

         for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, (int)sortData[i]);
         }
      }

      return snrData;
   }

   private static StaticNormalRelativeData fromMixed(int[] vertexCounts, TQuad[] quads, ChunkSectionPos sectionPos) {
      StaticNormalRelativeData snrData = new StaticNormalRelativeData(sectionPos, vertexCounts, quads.length);
      StaticSorter sorter = new StaticSorter(quads.length);
      snrData.sorterOnce = sorter;
      IntBuffer indexBuffer = sorter.getIntBuffer();
      int maxQuadCount = 0;
      boolean anyNeedsSortData = false;

      for (int vertexCount : vertexCounts) {
         if (vertexCount != -1) {
            int quadCount = TranslucentData.vertexCountToQuadCount(vertexCount);
            maxQuadCount = Math.max(maxQuadCount, quadCount);
            anyNeedsSortData |= !RadixSort.useRadixSort(quadCount) && quadCount > 1;
         }
      }

      long[] sortData = null;
      if (anyNeedsSortData) {
         sortData = new long[maxQuadCount];
      }

      int quadIndex = 0;

      for (int vertexCountx : vertexCounts) {
         if (vertexCountx != -1 && vertexCountx != 0) {
            int count = TranslucentData.vertexCountToQuadCount(vertexCountx);
            if (count == 1) {
               TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
               quadIndex++;
            } else if (RadixSort.useRadixSort(count)) {
               int[] keys = new int[count];

               for (int q = 0; q < count; q++) {
                  keys[q] = MathUtil.floatToComparableInt(quads[quadIndex++].getAccurateDotProduct());
               }

               int[] indices = RadixSort.sort(keys);

               for (int i = 0; i < count; i++) {
                  TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
               }
            } else {
               for (int i = 0; i < count; i++) {
                  TQuad quad = quads[quadIndex++];
                  int dotProductComponent = MathUtil.floatToComparableInt(quad.getAccurateDotProduct());
                  sortData[i] = (long)dotProductComponent << 32 | i;
               }

               if (count > 1) {
                  Arrays.sort(sortData, 0, count);
               }

               for (int i = 0; i < count; i++) {
                  TranslucentData.writeQuadVertexIndexes(indexBuffer, (int)sortData[i]);
               }
            }
         }
      }

      return snrData;
   }

   public static StaticNormalRelativeData fromMesh(int[] vertexCounts, TQuad[] quads, ChunkSectionPos sectionPos, boolean isDoubleUnaligned) {
      return isDoubleUnaligned ? fromDoubleUnaligned(vertexCounts, quads, sectionPos) : fromMixed(vertexCounts, quads, sectionPos);
   }
}
