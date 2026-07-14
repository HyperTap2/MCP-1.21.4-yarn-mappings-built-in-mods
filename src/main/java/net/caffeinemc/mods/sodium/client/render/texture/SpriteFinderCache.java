package net.caffeinemc.mods.sodium.client.render.texture;

import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;

public class SpriteFinderCache {
   private static SpriteFinder blockAtlasSpriteFinder;

   public static SpriteFinder forBlockAtlas() {
      if (blockAtlasSpriteFinder == null) {
         blockAtlasSpriteFinder = SpriteFinder.get(MinecraftClient.getInstance().getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
      }

      return blockAtlasSpriteFinder;
   }

   public static void resetSpriteFinder() {
      blockAtlasSpriteFinder = null;
   }
}
