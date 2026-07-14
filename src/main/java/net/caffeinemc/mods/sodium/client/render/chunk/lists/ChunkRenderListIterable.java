package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import java.util.Iterator;

public interface ChunkRenderListIterable extends Iterable<ChunkRenderList> {
   Iterator<ChunkRenderList> iterator(boolean var1);

   default Iterator<ChunkRenderList> iterator() {
      return this.iterator(false);
   }
}
