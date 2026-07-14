package net.caffeinemc.mods.sodium.api.texture;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public interface SpriteUtil {
   SpriteUtil INSTANCE = DependencyInjection.load(SpriteUtil.class, "net.caffeinemc.mods.sodium.client.render.texture.SpriteUtilImpl");

   void markSpriteActive(@NotNull Sprite var1);

   boolean hasAnimation(@NotNull Sprite var1);
}
