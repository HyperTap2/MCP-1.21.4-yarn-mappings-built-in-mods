package net.caffeinemc.mods.sodium.client.render.texture;

import java.util.Objects;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.NotNull;

public class SpriteUtilImpl implements net.caffeinemc.mods.sodium.api.texture.SpriteUtil {
   @Override
   public void markSpriteActive(@NotNull Sprite sprite) {
      Objects.requireNonNull(sprite);
      ((SpriteContentsExtension)sprite.getContents()).sodium$setActive(true);
   }

   @Override
   public boolean hasAnimation(@NotNull Sprite sprite) {
      Objects.requireNonNull(sprite);
      return ((SpriteContentsExtension)sprite.getContents()).sodium$hasAnimation();
   }
}
