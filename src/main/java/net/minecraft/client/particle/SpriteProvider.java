package net.minecraft.client.particle;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.random.Random;

public interface SpriteProvider {
   Sprite getSprite(int age, int maxAge);

   Sprite getSprite(Random random);
}
