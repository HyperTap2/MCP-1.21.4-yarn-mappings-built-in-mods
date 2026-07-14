package net.minecraft.client.render;

import java.util.function.IntFunction;
import net.minecraft.util.TranslatableOption;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.function.ValueLists.OutOfBoundsHandling;

public enum ChunkBuilderMode implements TranslatableOption {
   NONE(0, "options.prioritizeChunkUpdates.none"),
   PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
   NEARBY(2, "options.prioritizeChunkUpdates.nearby");

   private static final IntFunction<ChunkBuilderMode> BY_ID = ValueLists.createIdToValueFunction(ChunkBuilderMode::getId, values(), OutOfBoundsHandling.WRAP);
   private final int id;
   private final String name;

   ChunkBuilderMode(final int id, final String name) {
      this.id = id;
      this.name = name;
   }

   public int getId() {
      return this.id;
   }

   public String getTranslationKey() {
      return this.name;
   }

   public static ChunkBuilderMode get(int id) {
      return BY_ID.apply(id);
   }
}
