package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

abstract class DynamicSorter extends Sorter {
   private final int quadCount;

   DynamicSorter(int quadCount) {
      this.quadCount = quadCount;
   }

   abstract void writeSort(CombinedCameraPos var1, boolean var2);

   @Override
   public void writeIndexBuffer(CombinedCameraPos cameraPos, boolean initial) {
      this.initBufferWithQuadLength(this.quadCount);
      this.writeSort(cameraPos, initial);
   }
}
