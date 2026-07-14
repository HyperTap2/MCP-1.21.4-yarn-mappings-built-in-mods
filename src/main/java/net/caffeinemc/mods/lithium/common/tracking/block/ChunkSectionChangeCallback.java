package net.caffeinemc.mods.lithium.common.tracking.block;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import java.util.ArrayList;
import net.caffeinemc.mods.lithium.common.block.BlockListeningSection;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.caffeinemc.mods.lithium.common.world.chunk.ChunkStatusTracker;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

public final class ChunkSectionChangeCallback {
   private ArrayList<SectionedBlockChangeTracker> trackers;

   public static void init() {
      ChunkStatusTracker.registerUnloadCallback((world, chunkPos) -> {
         Long2ReferenceOpenHashMap<ChunkSectionChangeCallback> callbacks = ((LithiumData)world).lithium$getData().chunkSectionChangeCallbacks();
         for (int y = Pos.SectionYCoord.getMinYSection(world); y <= Pos.SectionYCoord.getMaxYSectionInclusive(world); y++) {
            ChunkSectionPos sectionPos = ChunkSectionPos.from(chunkPos.x, y, chunkPos.z);
            ChunkSectionChangeCallback callback = callbacks.remove(sectionPos.asLong());
            if (callback != null) {
               callback.onChunkSectionInvalidated(sectionPos);
            }
         }
      });
   }

   public static ChunkSectionChangeCallback create(long sectionPos, World world) {
      ChunkSectionChangeCallback callback = new ChunkSectionChangeCallback();
      Long2ReferenceOpenHashMap<ChunkSectionChangeCallback> callbacks = ((LithiumData)world).lithium$getData().chunkSectionChangeCallbacks();
      ChunkSectionChangeCallback previous = callbacks.put(sectionPos, callback);
      if (previous != null) {
         previous.onChunkSectionInvalidated(ChunkSectionPos.from(sectionPos));
      }
      return callback;
   }

   public void onBlockChange(BlockListeningSection section) {
      ArrayList<SectionedBlockChangeTracker> current = this.trackers;
      this.trackers = null;
      if (current != null) {
         for (SectionedBlockChangeTracker tracker : current) {
            tracker.setChanged(section);
         }
      }
   }

   public void addTracker(SectionedBlockChangeTracker tracker) {
      if (this.trackers == null) {
         this.trackers = new ArrayList<>();
      }
      this.trackers.add(tracker);
   }

   public void removeTracker(SectionedBlockChangeTracker tracker) {
      if (this.trackers != null) {
         this.trackers.remove(tracker);
      }
   }

   private void onChunkSectionInvalidated(ChunkSectionPos sectionPos) {
      ArrayList<SectionedBlockChangeTracker> current = this.trackers;
      this.trackers = null;
      if (current != null) {
         for (SectionedBlockChangeTracker tracker : current) {
            tracker.onChunkSectionInvalidated(sectionPos);
         }
      }
   }
}
