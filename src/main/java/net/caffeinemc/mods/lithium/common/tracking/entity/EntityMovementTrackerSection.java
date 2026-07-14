package net.caffeinemc.mods.lithium.common.tracking.entity;

import net.minecraft.entity.Entity;
import net.minecraft.world.entity.SectionedEntityCache;

public interface EntityMovementTrackerSection {
   void lithium$addListener(SectionedEntityMovementTracker<?> listener);

   void lithium$removeListener(SectionedEntityCache<?> cache, SectionedEntityMovementTracker<?> listener);

   void lithium$trackEntityMovement(int notificationMask, long time);

   long lithium$getChangeTime(int trackedClass);

   <E extends Entity> void lithium$listenToMovementOnce(SectionedEntityMovementTracker<E> listener, int trackedClass);

   <E extends Entity> void lithium$removeListenToMovementOnce(SectionedEntityMovementTracker<E> listener, int trackedClass);
}
