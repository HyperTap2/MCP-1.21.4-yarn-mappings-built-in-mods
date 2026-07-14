package net.minecraft.client.option;

import java.util.function.IntFunction;
import net.minecraft.util.TranslatableOption;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.function.ValueLists.OutOfBoundsHandling;

public enum GraphicsMode implements TranslatableOption {
   FAST(0, "options.graphics.fast"),
   FANCY(1, "options.graphics.fancy"),
   FABULOUS(2, "options.graphics.fabulous");

   private static final IntFunction<GraphicsMode> BY_ID = ValueLists.createIdToValueFunction(GraphicsMode::getId, values(), OutOfBoundsHandling.WRAP);
   private final int id;
   private final String translationKey;

   GraphicsMode(final int id, final String translationKey) {
      this.id = id;
      this.translationKey = translationKey;
   }

   public int getId() {
      return this.id;
   }

   public String getTranslationKey() {
      return this.translationKey;
   }

   @Override
   public String toString() {
      return switch (this) {
         case FAST -> "fast";
         case FANCY -> "fancy";
         case FABULOUS -> "fabulous";
      };
   }

   public static GraphicsMode byId(int id) {
      return BY_ID.apply(id);
   }
}
