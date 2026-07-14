package net.minecraft.client.option;

import java.util.function.IntFunction;
import net.minecraft.text.Text;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.function.ValueLists.OutOfBoundsHandling;

public enum NarratorMode {
   OFF(0, "options.narrator.off"),
   ALL(1, "options.narrator.all"),
   CHAT(2, "options.narrator.chat"),
   SYSTEM(3, "options.narrator.system");

   private static final IntFunction<NarratorMode> BY_ID = ValueLists.createIdToValueFunction(NarratorMode::getId, values(), OutOfBoundsHandling.WRAP);
   private final int id;
   private final Text name;

   NarratorMode(final int id, final String name) {
      this.id = id;
      this.name = Text.translatable(name);
   }

   public int getId() {
      return this.id;
   }

   public Text getName() {
      return this.name;
   }

   public static NarratorMode byId(int id) {
      return BY_ID.apply(id);
   }

   public boolean shouldNarrateChat() {
      return this == ALL || this == CHAT;
   }

   public boolean shouldNarrateSystem() {
      return this == ALL || this == SYSTEM;
   }
}
