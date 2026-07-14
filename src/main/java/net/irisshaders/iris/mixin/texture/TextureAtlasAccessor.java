package net.irisshaders.iris.mixin.texture;

import java.util.Map;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

public interface TextureAtlasAccessor {
   Map<Identifier, Sprite> getTexturesByName();

   int getMipLevel();
}
