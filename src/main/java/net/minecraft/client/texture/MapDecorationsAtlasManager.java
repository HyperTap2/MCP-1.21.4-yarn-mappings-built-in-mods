package net.minecraft.client.texture;

import net.minecraft.item.map.MapDecoration;
import net.minecraft.util.Identifier;

public class MapDecorationsAtlasManager extends SpriteAtlasHolder {
   public MapDecorationsAtlasManager(TextureManager manager) {
      super(manager, Identifier.ofVanilla("textures/atlas/map_decorations.png"), Identifier.ofVanilla("map_decorations"));
   }

   public Sprite getSprite(MapDecoration decoration) {
      return this.getSprite(decoration.getAssetId());
   }
}
