package net.caffeinemc.mods.sodium.client.render.texture;

import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.Nullable;

@Deprecated(
   forRemoval = true
)
public class SpriteUtil {
   @Deprecated(
      forRemoval = true
   )
   public static void markSpriteActive(@Nullable Sprite sprite) {
      if (sprite != null) {
         net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.markSpriteActive(sprite);
      }
   }

   @Deprecated(
      forRemoval = true
   )
   public static boolean hasAnimation(@Nullable Sprite sprite) {
      return sprite != null ? net.caffeinemc.mods.sodium.api.texture.SpriteUtil.INSTANCE.hasAnimation(sprite) : false;
   }
}
