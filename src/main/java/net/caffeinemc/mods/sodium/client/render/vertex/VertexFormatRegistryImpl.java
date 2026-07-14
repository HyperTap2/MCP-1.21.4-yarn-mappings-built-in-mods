package net.caffeinemc.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.concurrent.locks.StampedLock;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormat;

public class VertexFormatRegistryImpl implements VertexFormatRegistry {
   private static final int ABSENT_INDEX = -1;
   private final Reference2IntMap<VertexFormat> descriptions = new Reference2IntOpenHashMap();
   private final StampedLock lock = new StampedLock();

   public VertexFormatRegistryImpl() {
      this.descriptions.defaultReturnValue(-1);
   }

   @Override
   public int allocateGlobalId(VertexFormat format) {
      long stamp = this.lock.readLock();

      int id;
      try {
         id = this.descriptions.getInt(format);
      } finally {
         this.lock.unlockRead(stamp);
      }

      if (id == -1) {
         stamp = this.lock.writeLock();

         try {
            this.descriptions.put(format, id = this.descriptions.size());
         } finally {
            this.lock.unlockWrite(stamp);
         }
      }

      return id;
   }
}
