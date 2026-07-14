package net.caffeinemc.mods.lithium.common.tracking.entity;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import net.caffeinemc.mods.lithium.common.util.tuples.WorldSectionBox;
import net.caffeinemc.mods.lithium.common.world.LithiumData;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;

public abstract class SectionedEntityMovementTracker<E extends Entity> {
   final WorldSectionBox trackedWorldSections;
   final Object clazz;
   private final int trackedIndex;
   protected ArrayList<EntityTrackingSection<Entity>> sortedSections;
   protected boolean[] sectionVisible;
   private int timesRegistered;
   private final ArrayList<EntityMovementTrackerSection> sectionsNotListeningTo = new ArrayList<>();
   private long maxChangeTime;
   private ReferenceOpenHashSet<SectionedEntityMovementListener> movementListeners;

   protected SectionedEntityMovementTracker(WorldSectionBox trackedWorldSections, Object entityType) {
      this.clazz = entityType;
      this.trackedWorldSections = trackedWorldSections;
      this.trackedIndex = MovementTrackerHelper.getTrackerIndex(entityType);
      if (this.trackedIndex == -1) {
         throw new IllegalArgumentException("Entity class is not registered for movement notifications: " + entityType);
      }
   }

   @Override
   public int hashCode() {
      return HashCommon.mix(this.trackedWorldSections.hashCode()) ^ HashCommon.mix(this.trackedIndex) ^ this.getClass().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return obj != null
         && obj.getClass() == this.getClass()
         && this.clazz == ((SectionedEntityMovementTracker<?>)obj).clazz
         && this.trackedWorldSections.equals(((SectionedEntityMovementTracker<?>)obj).trackedWorldSections);
   }

   public boolean isUnchangedSince(long lastCheckedTime) {
      if (lastCheckedTime <= this.maxChangeTime) {
         return false;
      }
      if (!this.sectionsNotListeningTo.isEmpty()) {
         this.setChanged(this.listenToAllSectionsAndGetMaxChangeTime());
      }
      return lastCheckedTime > this.maxChangeTime;
   }

   private long listenToAllSectionsAndGetMaxChangeTime() {
      long maxChangeTime = Long.MIN_VALUE;
      for (int i = this.sectionsNotListeningTo.size() - 1; i >= 0; i--) {
         EntityMovementTrackerSection section = this.sectionsNotListeningTo.remove(i);
         section.lithium$listenToMovementOnce(this, this.trackedIndex);
         maxChangeTime = Math.max(maxChangeTime, section.lithium$getChangeTime(this.trackedIndex));
      }
      return maxChangeTime;
   }

   public void register(ServerWorld world) {
      if (world != this.trackedWorldSections.world()) {
         throw new IllegalArgumentException("Tracker registered in a different world");
      }
      if (this.timesRegistered++ != 0) {
         return;
      }

      SectionedEntityCache<Entity> cache = world.getEntityManager().getCache();
      WorldSectionBox tracked = this.trackedWorldSections;
      this.sortedSections = new ArrayList<>(tracked.numSections());
      this.sectionVisible = new boolean[tracked.numSections()];
      for (int x = tracked.chunkX1(); x < tracked.chunkX2(); x++) {
         for (int z = tracked.chunkZ1(); z < tracked.chunkZ2(); z++) {
            for (int y = tracked.chunkY1(); y < tracked.chunkY2(); y++) {
               EntityTrackingSection<Entity> section = cache.getTrackingSection(ChunkSectionPos.asLong(x, y, z));
               this.sortedSections.add(section);
               section.lithium$addListener(this);
            }
         }
      }
      this.setChanged(world.getTime());
   }

   public void unRegister(ServerWorld world) {
      if (--this.timesRegistered > 0) {
         return;
      }
      if (this.timesRegistered < 0) {
         throw new IllegalStateException("Movement tracker unregistered too often");
      }

      SectionedEntityCache<Entity> cache = world.getEntityManager().getCache();
      ((LithiumData)world).lithium$getData().entityMovementTrackers().deleteCanonical(this);
      for (int i = this.sortedSections.size() - 1; i >= 0; i--) {
         EntityTrackingSection<Entity> section = this.sortedSections.get(i);
         section.lithium$removeListener(cache, this);
         if (!this.sectionsNotListeningTo.remove(section)) {
            section.lithium$removeListenToMovementOnce(this, this.trackedIndex);
         }
      }
      this.setChanged(world.getTime());
   }

   public void onSectionEnteredRange(EntityMovementTrackerSection section) {
      this.setChanged(this.trackedWorldSections.world().getTime());
      int sectionIndex = this.sortedSections.lastIndexOf(section);
      this.sectionVisible[sectionIndex] = true;
      this.sectionsNotListeningTo.add(section);
      this.notifyAllListeners();
   }

   public void onSectionLeftRange(EntityMovementTrackerSection section) {
      this.setChanged(this.trackedWorldSections.world().getTime());
      int sectionIndex = this.sortedSections.lastIndexOf(section);
      this.sectionVisible[sectionIndex] = false;
      if (!this.sectionsNotListeningTo.remove(section)) {
         section.lithium$removeListenToMovementOnce(this, this.trackedIndex);
      }
      this.notifyAllListeners();
   }

   public void listenToEntityMovementOnce(SectionedEntityMovementListener listener) {
      if (this.movementListeners == null) {
         this.movementListeners = new ReferenceOpenHashSet<>();
      }
      this.movementListeners.add(listener);
      if (!this.sectionsNotListeningTo.isEmpty()) {
         this.setChanged(this.listenToAllSectionsAndGetMaxChangeTime());
      }
   }

   public void emitEntityMovement(int classMask, EntityMovementTrackerSection section) {
      if ((classMask & 1 << this.trackedIndex) != 0) {
         this.notifyAllListeners();
         this.sectionsNotListeningTo.add(section);
      }
   }

   private void setChanged(long atTime) {
      this.maxChangeTime = Math.max(this.maxChangeTime, atTime);
   }

   private void notifyAllListeners() {
      if (this.movementListeners == null || this.movementListeners.isEmpty()) {
         return;
      }
      for (SectionedEntityMovementListener listener : this.movementListeners) {
         listener.lithium$handleEntityMovement(this.clazz);
      }
      this.movementListeners.clear();
   }
}
