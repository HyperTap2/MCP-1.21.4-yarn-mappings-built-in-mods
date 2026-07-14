package net.caffeinemc.mods.lithium.common.tracking.block;

import java.util.ArrayList;
import java.util.Objects;
import net.caffeinemc.mods.lithium.common.block.BlockListeningSection;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.caffeinemc.mods.lithium.common.util.deduplication.LithiumInterner;
import net.caffeinemc.mods.lithium.common.util.tuples.WorldSectionBox;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;

public final class SectionedBlockChangeTracker {
   public final WorldSectionBox trackedWorldSections;
   private long maxChangeTime;
   private int timesRegistered;
   private boolean listeningToAll;
   private ArrayList<ChunkSectionPos> sectionsNotListeningTo;
   private ArrayList<BlockListeningSection> sectionsUnsubscribed;

   private SectionedBlockChangeTracker(WorldSectionBox trackedWorldSections) {
      this.trackedWorldSections = trackedWorldSections;
   }

   public boolean matchesMovedBox(Box box) {
      return this.trackedWorldSections.matchesRelevantBlocksBox(box);
   }

   public static SectionedBlockChangeTracker registerAt(World world, Box box) {
      SectionedBlockChangeTracker tracker = new SectionedBlockChangeTracker(WorldSectionBox.relevantExpandedBlocksBox(world, box));
      LithiumInterner<SectionedBlockChangeTracker> trackers = ((LithiumData)world).lithium$getData().blockChangeTrackers();
      tracker = trackers.getCanonical(tracker);
      tracker.register();
      return tracker;
   }

   public void register() {
      if (this.timesRegistered++ != 0) {
         return;
      }

      WorldSectionBox box = this.trackedWorldSections;
      for (int x = box.chunkX1(); x < box.chunkX2(); x++) {
         for (int z = box.chunkZ1(); z < box.chunkZ2(); z++) {
            World world = box.world();
            Chunk chunk = world.getChunk(x, z, ChunkStatus.FULL, false);
            ChunkSection[] sections = chunk == null ? null : chunk.getSectionArray();
            for (int y = box.chunkY1(); y < box.chunkY2(); y++) {
               if (y < Pos.SectionYCoord.getMinYSection(world) || y >= Pos.SectionYCoord.getMaxYSectionExclusive(world)) {
                  continue;
               }
               ChunkSectionPos sectionPos = ChunkSectionPos.from(x, y, z);
               if (sections == null) {
                  if (this.sectionsNotListeningTo == null) {
                     this.sectionsNotListeningTo = new ArrayList<>();
                  }
                  this.sectionsNotListeningTo.add(sectionPos);
               } else {
                  ((BlockListeningSection)sections[Pos.SectionYIndex.fromSectionCoord(world, y)])
                     .lithium$addToCallback(this, sectionPos.asLong(), world);
               }
            }
         }
      }

      this.listeningToAll = (this.sectionsNotListeningTo == null || this.sectionsNotListeningTo.isEmpty())
         && (this.sectionsUnsubscribed == null || this.sectionsUnsubscribed.isEmpty());
      this.setChanged(this.getWorldTime());
   }

   public void unregister() {
      if (--this.timesRegistered > 0) {
         return;
      }

      WorldSectionBox box = this.trackedWorldSections;
      World world = box.world();
      for (int x = box.chunkX1(); x < box.chunkX2(); x++) {
         for (int z = box.chunkZ1(); z < box.chunkZ2(); z++) {
            Chunk chunk = world.getChunk(x, z, ChunkStatus.FULL, false);
            ChunkSection[] sections = chunk == null ? null : chunk.getSectionArray();
            if (sections == null) {
               continue;
            }
            for (int y = box.chunkY1(); y < box.chunkY2(); y++) {
               if (y >= Pos.SectionYCoord.getMinYSection(world) && y < Pos.SectionYCoord.getMaxYSectionExclusive(world)) {
                  ((BlockListeningSection)sections[Pos.SectionYIndex.fromSectionCoord(world, y)]).lithium$removeFromCallback(this);
               }
            }
         }
      }

      this.sectionsNotListeningTo = null;
      ((LithiumData)world).lithium$getData().blockChangeTrackers().deleteCanonical(this);
   }

   public boolean isUnchangedSince(long lastCheckedTime) {
      if (lastCheckedTime <= this.maxChangeTime) {
         return false;
      }
      if (!this.listeningToAll) {
         this.listenToAllSections();
      }
      return this.listeningToAll && lastCheckedTime > this.maxChangeTime;
   }

   private void listenToAllSections() {
      boolean changed = false;
      if (this.sectionsNotListeningTo != null) {
         for (int i = this.sectionsNotListeningTo.size() - 1; i >= 0; i--) {
            ChunkSectionPos pos = this.sectionsNotListeningTo.get(i);
            World world = this.trackedWorldSections.world();
            Chunk chunk = world.getChunk(pos.getX(), pos.getZ(), ChunkStatus.FULL, false);
            if (chunk == null) {
               return;
            }
            changed = true;
            this.sectionsNotListeningTo.remove(i);
            ((BlockListeningSection)chunk.getSectionArray()[Pos.SectionYIndex.fromSectionCoord(world, pos.getY())])
               .lithium$addToCallback(this, pos.asLong(), world);
         }
      }
      if (this.sectionsUnsubscribed != null) {
         for (int i = this.sectionsUnsubscribed.size() - 1; i >= 0; i--) {
            changed = true;
            this.sectionsUnsubscribed.remove(i).lithium$addToCallback(this, Long.MIN_VALUE, null);
         }
      }
      this.listeningToAll = true;
      if (changed) {
         this.setChanged(this.getWorldTime());
      }
   }

   public void setChanged(BlockListeningSection section) {
      if (this.sectionsUnsubscribed == null) {
         this.sectionsUnsubscribed = new ArrayList<>();
      }
      this.sectionsUnsubscribed.add(section);
      this.listeningToAll = false;
      this.setChanged(this.getWorldTime());
   }

   private void setChanged(long time) {
      this.maxChangeTime = Math.max(this.maxChangeTime, time);
   }

   private long getWorldTime() {
      return this.trackedWorldSections.world().getTime();
   }

   public void onChunkSectionInvalidated(ChunkSectionPos pos) {
      if (this.sectionsNotListeningTo == null) {
         this.sectionsNotListeningTo = new ArrayList<>();
      }
      this.sectionsNotListeningTo.add(pos);
      this.listeningToAll = false;
      this.setChanged(this.getWorldTime());
   }

   @Override
   public boolean equals(Object object) {
      return object == this || object instanceof SectionedBlockChangeTracker other
         && Objects.equals(this.trackedWorldSections, other.trackedWorldSections);
   }

   @Override
   public int hashCode() {
      return SectionedBlockChangeTracker.class.hashCode() ^ this.trackedWorldSections.hashCode();
   }
}
