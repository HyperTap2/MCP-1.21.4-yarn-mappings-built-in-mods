package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public abstract class Sorter implements PresentSortData {
   private NativeBuffer indexBuffer;

   public abstract void writeIndexBuffer(CombinedCameraPos var1, boolean var2);

   @Override
   public NativeBuffer getIndexBuffer() {
      return this.indexBuffer;
   }

   void initBufferWithQuadLength(int quadCount) {
      this.indexBuffer = new NativeBuffer(TranslucentData.quadCountToIndexBytes(quadCount));
   }
}
