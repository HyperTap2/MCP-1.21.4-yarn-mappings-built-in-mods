package net.minecraft.client.sound;

import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public interface SoundInstance {
   Identifier getId();

   @Nullable
   WeightedSoundSet getSoundSet(SoundManager soundManager);

   Sound getSound();

   SoundCategory getCategory();

   boolean isRepeatable();

   boolean isRelative();

   int getRepeatDelay();

   float getVolume();

   float getPitch();

   double getX();

   double getY();

   double getZ();

   SoundInstance.AttenuationType getAttenuationType();

   default boolean shouldAlwaysPlay() {
      return false;
   }

   default boolean canPlay() {
      return true;
   }

   static Random createRandom() {
      return Random.create();
   }

   enum AttenuationType {
      NONE,
      LINEAR;
   }
}
