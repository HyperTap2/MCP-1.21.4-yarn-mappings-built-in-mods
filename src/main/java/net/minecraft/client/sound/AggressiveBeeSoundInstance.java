package net.minecraft.client.sound;

import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class AggressiveBeeSoundInstance extends AbstractBeeSoundInstance {
   public AggressiveBeeSoundInstance(BeeEntity entity) {
      super(entity, SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE, SoundCategory.NEUTRAL);
      this.repeatDelay = 0;
   }

   @Override
   protected MovingSoundInstance getReplacement() {
      return new PassiveBeeSoundInstance(this.bee);
   }

   @Override
   protected boolean shouldReplace() {
      return !this.bee.hasAngerTime();
   }
}
