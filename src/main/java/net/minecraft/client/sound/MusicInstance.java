package net.minecraft.client.sound;

import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

public record MusicInstance(@Nullable MusicSound music, float volume) {
   public MusicInstance(MusicSound music) {
      this(music, 1.0F);
   }

   public boolean shouldReplace(SoundInstance sound) {
      return this.music == null ? false : this.music.shouldReplaceCurrentMusic() && !((SoundEvent)this.music.getSound().value()).id().equals(sound.getId());
   }
}
