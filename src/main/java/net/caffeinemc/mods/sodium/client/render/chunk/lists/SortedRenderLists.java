package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.util.iterator.ReversibleObjectArrayIterator;

public class SortedRenderLists implements ChunkRenderListIterable {
   private static final SortedRenderLists EMPTY = new SortedRenderLists(ObjectArrayList.of());
   private final ObjectArrayList<ChunkRenderList> lists;

   SortedRenderLists(ObjectArrayList<ChunkRenderList> lists) {
      this.lists = lists;
   }

   public ReversibleObjectArrayIterator<ChunkRenderList> iterator(boolean reverse) {
      return new ReversibleObjectArrayIterator<>(this.lists, reverse);
   }

   public static SortedRenderLists empty() {
      return EMPTY;
   }
}
