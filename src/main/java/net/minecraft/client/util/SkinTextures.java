package net.minecraft.client.util;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record SkinTextures(
   Identifier texture,
   @Nullable String textureUrl,
   @Nullable Identifier capeTexture,
   @Nullable Identifier elytraTexture,
   SkinTextures.Model model,
   boolean secure
) {
   public enum Model {
      SLIM("slim"),
      WIDE("default");

      private final String name;

      Model(final String name) {
         this.name = name;
      }

      public static SkinTextures.Model fromName(@Nullable String name) {
         if (name == null) {
            return WIDE;
         }

         return switch (name) {
            case "slim" -> SLIM;
            default -> WIDE;
         };
      }

      public String getName() {
         return this.name;
      }
   }
}
