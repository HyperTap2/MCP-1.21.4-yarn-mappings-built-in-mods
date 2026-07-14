package net.minecraft.client.model;

import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;

public interface SpriteGetter {
   Sprite get(SpriteIdentifier spriteId);

   Sprite getMissing(String textureId);
}
