package net.minecraft.client.sound;

import net.minecraft.util.math.random.Random;

public interface SoundContainer<T> {
   int getWeight();

   T getSound(Random random);

   void preload(SoundSystem soundSystem);
}
