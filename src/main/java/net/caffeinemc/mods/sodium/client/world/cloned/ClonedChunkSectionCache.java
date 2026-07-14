package net.caffeinemc.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClonedChunkSectionCache {
   private static final int MAX_CACHE_SIZE = 512;
   private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5L);
   private final World level;
   private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> positionToEntry = new Long2ReferenceLinkedOpenHashMap();
   private long time;

   public ClonedChunkSectionCache(World level) {
      this.level = level;
      this.time = getMonotonicTimeSource();
   }

   public void cleanup() {
      this.time = getMonotonicTimeSource();
      this.positionToEntry.values().removeIf(entry -> this.time > entry.getLastUsedTimestamp() + MAX_CACHE_DURATION);
   }

   @Nullable
   public ClonedChunkSection acquire(int x, int y, int z) {
      long pos = ChunkSectionPos.asLong(x, y, z);
      ClonedChunkSection section = (ClonedChunkSection)this.positionToEntry.getAndMoveToLast(pos);
      if (section == null) {
         section = this.clone(x, y, z);

         while (this.positionToEntry.size() >= 512) {
            this.positionToEntry.removeFirst();
         }

         this.positionToEntry.putAndMoveToLast(pos, section);
      }

      section.setLastUsedTimestamp(this.time);
      return section;
   }

   @NotNull
   private ClonedChunkSection clone(int x, int y, int z) {
      WorldChunk chunk = this.level.getChunk(x, z);
      if (chunk == null) {
         throw new RuntimeException("Chunk is not loaded at: " + ChunkSectionPos.asLong(x, y, z));
      } else {
         ChunkSection section = null;
         if (!this.level.isOutOfHeightLimit(ChunkSectionPos.getBlockCoord(y))) {
            section = chunk.getSectionArray()[this.level.sectionCoordToIndex(y)];
         }

         return new ClonedChunkSection(this.level, chunk, section, ChunkSectionPos.from(x, y, z));
      }
   }

   public void invalidate(int x, int y, int z) {
      this.positionToEntry.remove(ChunkSectionPos.asLong(x, y, z));
   }

   private static long getMonotonicTimeSource() {
      return System.nanoTime();
   }
}
