package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

public enum SortType {
   EMPTY_SECTION(false),
   NO_TRANSLUCENT(false),
   NONE(false),
   STATIC_NORMAL_RELATIVE(false),
   STATIC_TOPO(true),
   DYNAMIC(true);

   public final boolean needsDirectionMixing;

   private SortType(boolean needsDirectionMixing) {
      this.needsDirectionMixing = needsDirectionMixing;
   }
}
