package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

public enum SortBehavior {
   OFF("OFF", SortBehavior.SortMode.NONE),
   STATIC("S", SortBehavior.SortMode.STATIC),
   DYNAMIC_DEFER_ALWAYS("DF", SortBehavior.PriorityMode.NONE, SortBehavior.DeferMode.ALWAYS),
   DYNAMIC_DEFER_NEARBY_ONE_FRAME("N1", SortBehavior.PriorityMode.NEARBY, SortBehavior.DeferMode.ONE_FRAME),
   DYNAMIC_DEFER_NEARBY_ZERO_FRAMES("N0", SortBehavior.PriorityMode.NEARBY, SortBehavior.DeferMode.ZERO_FRAMES),
   DYNAMIC_DEFER_ALL_ONE_FRAME("A1", SortBehavior.PriorityMode.ALL, SortBehavior.DeferMode.ONE_FRAME),
   DYNAMIC_DEFER_ALL_ZERO_FRAMES("A0", SortBehavior.PriorityMode.ALL, SortBehavior.DeferMode.ZERO_FRAMES);

   private final String shortName;
   private final SortBehavior.SortMode sortMode;
   private final SortBehavior.PriorityMode priorityMode;
   private final SortBehavior.DeferMode deferMode;

   private SortBehavior(String shortName, SortBehavior.SortMode sortMode, SortBehavior.PriorityMode priorityMode, SortBehavior.DeferMode deferMode) {
      this.shortName = shortName;
      this.sortMode = sortMode;
      this.priorityMode = priorityMode;
      this.deferMode = deferMode;
   }

   private SortBehavior(String shortName, SortBehavior.SortMode sortMode) {
      this(shortName, sortMode, null, null);
   }

   private SortBehavior(String shortName, SortBehavior.PriorityMode priorityMode, SortBehavior.DeferMode deferMode) {
      this(shortName, SortBehavior.SortMode.DYNAMIC, priorityMode, deferMode);
   }

   public String getShortName() {
      return this.shortName;
   }

   public SortBehavior.SortMode getSortMode() {
      return this.sortMode;
   }

   public SortBehavior.PriorityMode getPriorityMode() {
      return this.priorityMode;
   }

   public SortBehavior.DeferMode getDeferMode() {
      return this.deferMode;
   }

   public static enum DeferMode {
      ALWAYS,
      ONE_FRAME,
      ZERO_FRAMES;
   }

   public static enum PriorityMode {
      NONE,
      NEARBY,
      ALL;
   }

   public static enum SortMode {
      NONE,
      STATIC,
      DYNAMIC;
   }
}
