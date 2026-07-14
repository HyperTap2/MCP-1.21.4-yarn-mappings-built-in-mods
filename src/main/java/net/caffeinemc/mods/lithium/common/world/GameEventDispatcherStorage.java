package net.caffeinemc.mods.lithium.common.world;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.event.listener.GameEventDispatcher;

public final class GameEventDispatcherStorage {
   private final Long2ReferenceOpenHashMap<Int2ObjectMap<GameEventDispatcher>> storage = new Long2ReferenceOpenHashMap<>();
   private final LongOpenHashSet loadedChunks = new LongOpenHashSet();

   public void addChunk(long pos, Int2ObjectMap<GameEventDispatcher> dispatchers) {
      if (dispatchers != null) this.storage.put(pos, dispatchers);
      this.loadedChunks.add(pos);
   }

   public void removeChunk(long pos) {
      this.storage.remove(pos);
      this.loadedChunks.remove(pos);
   }

   public void replace(long pos, Int2ObjectMap<GameEventDispatcher> dispatchers) {
      if (!this.loadedChunks.contains(pos)) return;
      if (dispatchers == null) this.storage.remove(pos);
      else this.storage.put(pos, dispatchers);
   }

   public Int2ObjectMap<GameEventDispatcher> get(long pos) {
      return this.storage.get(pos);
   }
}
