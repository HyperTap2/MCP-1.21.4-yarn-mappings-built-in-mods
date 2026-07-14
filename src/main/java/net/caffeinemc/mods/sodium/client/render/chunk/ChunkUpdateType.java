package net.caffeinemc.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
   SORT(Integer.MAX_VALUE, 1),
   INITIAL_BUILD(128, 10),
   REBUILD(Integer.MAX_VALUE, 10),
   IMPORTANT_REBUILD(Integer.MAX_VALUE, 10),
   IMPORTANT_SORT(Integer.MAX_VALUE, 1);

   private final int maximumQueueSize;
   private final int taskEffort;

   private ChunkUpdateType(int maximumQueueSize, int taskEffort) {
      this.maximumQueueSize = maximumQueueSize;
      this.taskEffort = taskEffort;
   }

   public static ChunkUpdateType getPromotionUpdateType(ChunkUpdateType prev, ChunkUpdateType next) {
      if (prev != null && prev != SORT && prev != next) {
         return next != IMPORTANT_REBUILD && (prev != IMPORTANT_SORT || next != REBUILD) && (prev != REBUILD || next != IMPORTANT_SORT)
            ? null
            : IMPORTANT_REBUILD;
      } else {
         return next;
      }
   }

   public int getMaximumQueueSize() {
      return this.maximumQueueSize;
   }

   public boolean isImportant() {
      return this == IMPORTANT_REBUILD || this == IMPORTANT_SORT;
   }

   public int getTaskEffort() {
      return this.taskEffort;
   }
}
