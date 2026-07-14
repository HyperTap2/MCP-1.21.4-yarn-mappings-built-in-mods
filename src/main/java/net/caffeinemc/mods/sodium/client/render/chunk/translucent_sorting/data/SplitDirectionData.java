package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.minecraft.util.math.ChunkSectionPos;

public abstract class SplitDirectionData extends PresentTranslucentData {
   private final int[] vertexCounts;

   public SplitDirectionData(ChunkSectionPos sectionPos, int[] vertexCounts, int quadCount) {
      super(sectionPos, quadCount);
      this.vertexCounts = vertexCounts;
   }

   @Override
   public int[] getVertexCounts() {
      return this.vertexCounts;
   }
}
