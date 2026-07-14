package net.minecraft.client.util;

import java.util.Locale;
import malte0811.ferritecore.FerriteCoreDeduplicator;
import net.minecraft.util.Identifier;

public record ModelIdentifier(Identifier id, String variant) {
   public ModelIdentifier {
      variant = FerriteCoreDeduplicator.deduplicateVariant(toLowerCase(variant));
   }

   private static String toLowerCase(String string) {
      return string.toLowerCase(Locale.ROOT);
   }

   public String getVariant() {
      return this.variant;
   }

   @Override
   public String toString() {
      return this.id + "#" + this.variant;
   }
}
