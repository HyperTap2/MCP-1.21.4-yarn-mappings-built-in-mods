package net.minecraft.client.sound;


public interface SoundInstanceListener {
   void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range);
}
