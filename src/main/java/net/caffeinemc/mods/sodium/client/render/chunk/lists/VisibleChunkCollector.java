package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateType;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.util.math.ChunkSectionPos;

public class VisibleChunkCollector implements OcclusionCuller.Visitor {
   private final ObjectArrayList<ChunkRenderList> sortedRenderLists;
   private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> sortedRebuildLists;
   private final int frame;
   private static int[] sortItems = new int[256];

   public VisibleChunkCollector(int frame) {
      this.frame = frame;
      this.sortedRenderLists = new ObjectArrayList();
      this.sortedRebuildLists = new EnumMap<>(ChunkUpdateType.class);

      for (ChunkUpdateType type : ChunkUpdateType.values()) {
         this.sortedRebuildLists.put(type, new ArrayDeque<>());
      }
   }

   @Override
   public void visit(RenderSection section) {
      if (section.getFlags() != 0) {
         RenderRegion region = section.getRegion();
         ChunkRenderList renderList = region.getRenderList();
         if (renderList.getLastVisibleFrame() != this.frame) {
            renderList.reset(this.frame);
            this.sortedRenderLists.add(renderList);
         }

         renderList.add(section);
      }

      this.addToRebuildLists(section);
   }

   private void addToRebuildLists(RenderSection section) {
      ChunkUpdateType type = section.getPendingUpdate();
      if (type != null && section.getTaskCancellationToken() == null) {
         Queue<RenderSection> queue = this.sortedRebuildLists.get(type);
         if (queue.size() < type.getMaximumQueueSize()) {
            queue.add(section);
         }
      }
   }

   public SortedRenderLists createRenderLists(Viewport viewport) {
      ChunkSectionPos sectionPos = viewport.getChunkCoord();
      int cameraX = sectionPos.getX() >> RenderRegion.REGION_WIDTH_SH;
      int cameraY = sectionPos.getY() >> RenderRegion.REGION_HEIGHT_SH;
      int cameraZ = sectionPos.getZ() >> RenderRegion.REGION_LENGTH_SH;
      int size = this.sortedRenderLists.size();
      if (sortItems.length < size) {
         sortItems = new int[size];
      }

      for (int i = 0; i < size; i++) {
         RenderRegion region = ((ChunkRenderList)this.sortedRenderLists.get(i)).getRegion();
         int x = Math.abs(region.getX() - cameraX);
         int y = Math.abs(region.getY() - cameraY);
         int z = Math.abs(region.getZ() - cameraZ);
         sortItems[i] = x + y + z << 16 | i;
      }

      IntArrays.unstableSort(sortItems, 0, size);
      ObjectArrayList<ChunkRenderList> sorted = new ObjectArrayList(size);

      for (int i = 0; i < size; i++) {
         int key = sortItems[i];
         ChunkRenderList renderList = (ChunkRenderList)this.sortedRenderLists.get(key & 65535);
         sorted.add(renderList);
      }

      ObjectListIterator var14 = sorted.iterator();

      while (var14.hasNext()) {
         ChunkRenderList list = (ChunkRenderList)var14.next();
         list.sortSections(sectionPos, sortItems);
      }

      return new SortedRenderLists(sorted);
   }

   public Map<ChunkUpdateType, ArrayDeque<RenderSection>> getRebuildLists() {
      return this.sortedRebuildLists;
   }
}
