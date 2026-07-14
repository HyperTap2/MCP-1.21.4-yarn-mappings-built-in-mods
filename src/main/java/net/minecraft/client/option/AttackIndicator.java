package net.minecraft.client.option;

import java.util.function.IntFunction;
import net.minecraft.util.TranslatableOption;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.function.ValueLists.OutOfBoundsHandling;

public enum AttackIndicator implements TranslatableOption {
   OFF(0, "options.off"),
   CROSSHAIR(1, "options.attack.crosshair"),
   HOTBAR(2, "options.attack.hotbar");

   private static final IntFunction<AttackIndicator> BY_ID = ValueLists.createIdToValueFunction(AttackIndicator::getId, values(), OutOfBoundsHandling.WRAP);
   private final int id;
   private final String translationKey;

   AttackIndicator(final int id, final String translationKey) {
      this.id = id;
      this.translationKey = translationKey;
   }

   public int getId() {
      return this.id;
   }

   public String getTranslationKey() {
      return this.translationKey;
   }

   public static AttackIndicator byId(int id) {
      return BY_ID.apply(id);
   }
}
