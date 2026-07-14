package net.minecraft.client.sound;


public interface TickableSoundInstance extends SoundInstance {
   boolean isDone();

   void tick();
}
