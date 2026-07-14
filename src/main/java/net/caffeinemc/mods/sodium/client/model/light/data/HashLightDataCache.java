package net.caffeinemc.mods.sodium.client.model.light.data;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class HashLightDataCache extends LightDataAccess {
   private final Long2IntLinkedOpenHashMap map = new Long2IntLinkedOpenHashMap(1024, 0.5F);

   public HashLightDataCache(BlockRenderView level) {
      this.level = level;
   }

   @Override
   public int get(int x, int y, int z) {
      long key = BlockPos.asLong(x, y, z);
      int word = this.map.getAndMoveToFirst(key);
      if (word == 0) {
         if (this.map.size() > 1024) {
            this.map.removeLastInt();
         }

         this.map.put(key, word = this.compute(x, y, z));
      }

      return word;
   }

   public void clearCache() {
      this.map.clear();
   }
}
