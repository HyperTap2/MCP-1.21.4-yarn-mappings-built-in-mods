package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Stream;
import net.caffeinemc.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import net.caffeinemc.mods.lithium.common.entity.pushable.FeetBlockCachingEntity;
import net.caffeinemc.mods.lithium.common.entity.pushable.PushableEntityClassGroup;
import net.caffeinemc.mods.lithium.common.entity.PositionedEntityTrackingSection;
import net.caffeinemc.mods.lithium.common.tracking.entity.EntityMovementTrackerSection;
import net.caffeinemc.mods.lithium.common.tracking.entity.MovementTrackerHelper;
import net.caffeinemc.mods.lithium.common.tracking.entity.SectionedEntityMovementTracker;
import net.caffeinemc.mods.lithium.common.util.collections.ReferenceMaskedList;
import net.caffeinemc.mods.lithium.common.world.ClimbingMobCachingSection;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;

public class EntityTrackingSection<T extends EntityLike>
   implements EntityMovementTrackerSection, PositionedEntityTrackingSection, ClimbingMobCachingSection {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final TypeFilterableList<T> collection;
   private EntityTrackingStatus status;
   private long pos;
   private final ReferenceOpenHashSet<SectionedEntityMovementTracker<?>> visibilityListeners = new ReferenceOpenHashSet<>();
   private final ArrayList<SectionedEntityMovementTracker<?>>[] movementListenersByType = new ArrayList[MovementTrackerHelper.NUM_MOVEMENT_NOTIFYING_CLASSES];
   private final long[] lastEntityMovementByType = new long[MovementTrackerHelper.NUM_MOVEMENT_NOTIFYING_CLASSES];
   private ReferenceMaskedList<Entity> pushableEntities;

   public EntityTrackingSection(Class<T> entityClass, EntityTrackingStatus status) {
      this.status = status;
      this.collection = new TypeFilterableList<>(entityClass);
   }

   public void add(T entity) {
      this.collection.add(entity);
      if (this.pushableEntities != null) {
         if (!this.status.shouldTrack()) {
            this.stopFilteringPushableEntities();
         } else {
            this.onStartClimbingCachingEntity((Entity)entity);
            if (this.pushableEntities.totalSize() > this.collection.size()) {
               this.stopFilteringPushableEntities();
            }
         }
      }
   }

   public boolean remove(T entity) {
      boolean removed = this.collection.remove(entity);
      if (removed && this.pushableEntities != null) {
         if (!this.status.shouldTrack()) {
            this.stopFilteringPushableEntities();
         } else {
            this.pushableEntities.remove((Entity)entity);
         }
      }
      return removed;
   }

   @Override
   public LazyIterationConsumer.NextIteration lithium$collectPushableEntities(
      World world, Entity except, Box box, EntityPushablePredicate<? super Entity> predicate, ArrayList<Entity> entities
   ) {
      Iterator<?> iterator = this.pushableEntities != null ? this.pushableEntities.iterator() : this.collection.iterator();
      int candidates = 0;
      int matches = 0;
      while (iterator.hasNext()) {
         Entity entity = (Entity)iterator.next();
         if (entity.getBoundingBox().intersects(box)
            && !entity.isSpectator()
            && entity != except
            && !(entity instanceof EnderDragonEntity)) {
            candidates++;
            if (predicate.test(entity)) {
               matches++;
               entities.add(entity);
            }
         }
      }

      if (this.pushableEntities == null && candidates >= 25 && candidates >= matches * 2) {
         this.startFilteringPushableEntities();
      }
      return LazyIterationConsumer.NextIteration.CONTINUE;
   }

   private void startFilteringPushableEntities() {
      this.pushableEntities = new ReferenceMaskedList<>();
      for (T entity : this.collection) {
         this.onStartClimbingCachingEntity((Entity)entity);
      }
   }

   private void stopFilteringPushableEntities() {
      this.pushableEntities = null;
   }

   @Override
   public void lithium$onEntityModifiedCachedBlock(FeetBlockCachingEntity entity, BlockState newBlockState) {
      if (this.pushableEntities == null) {
         entity.lithium$setClimbingMobCachingSectionUpdateBehavior(false);
      } else {
         this.pushableEntities.setVisible((Entity)entity, newBlockState == null || !newBlockState.isIn(BlockTags.CLIMBABLE));
      }
   }

   private void onStartClimbingCachingEntity(Entity entity) {
      if (!PushableEntityClassGroup.MAYBE_PUSHABLE.contains(entity)) {
         return;
      }
      this.pushableEntities.add(entity);
      if (PushableEntityClassGroup.CACHABLE_UNPUSHABILITY.contains(entity)) {
         FeetBlockCachingEntity cachingEntity = (FeetBlockCachingEntity)entity;
         this.pushableEntities.setVisible(entity, cachingEntity.lithium$getCachedFeetBlockState() == null
            || !cachingEntity.lithium$getCachedFeetBlockState().isIn(BlockTags.CLIMBABLE));
         cachingEntity.lithium$setClimbingMobCachingSectionUpdateBehavior(true);
      }
   }

   public LazyIterationConsumer.NextIteration forEach(Box box, LazyIterationConsumer<T> consumer) {
      for (T entityLike : this.collection.lithium$getAllElements()) {
         if (entityLike.getBoundingBox().intersects(box) && consumer.accept(entityLike).shouldAbort()) {
            return LazyIterationConsumer.NextIteration.ABORT;
         }
      }

      return LazyIterationConsumer.NextIteration.CONTINUE;
   }

   public <U extends T> LazyIterationConsumer.NextIteration forEach(TypeFilter<T, U> type, Box box, LazyIterationConsumer<? super U> consumer) {
      Collection<? extends T> collection = this.collection.getAllOfType(type.getBaseClass());
      if (collection.isEmpty()) {
         return LazyIterationConsumer.NextIteration.CONTINUE;
      }

      for (T entityLike : collection) {
         U entityLike2 = (U)type.downcast(entityLike);
         if (entityLike2 != null && entityLike.getBoundingBox().intersects(box) && consumer.accept(entityLike2).shouldAbort()) {
            return LazyIterationConsumer.NextIteration.ABORT;
         }
      }

      return LazyIterationConsumer.NextIteration.CONTINUE;
   }

   public boolean isEmpty() {
      return this.collection.isEmpty() && this.visibilityListeners.isEmpty();
   }

   public Stream<T> stream() {
      return this.collection.stream();
   }

   public EntityTrackingStatus getStatus() {
      return this.status;
   }

   public EntityTrackingStatus swapStatus(EntityTrackingStatus status) {
      EntityTrackingStatus entityTrackingStatus = this.status;
      if (entityTrackingStatus.shouldTrack() != status.shouldTrack()) {
         for (SectionedEntityMovementTracker<?> listener : this.visibilityListeners) {
            if (status.shouldTrack()) {
               listener.onSectionEnteredRange(this);
            } else {
               listener.onSectionLeftRange(this);
            }
         }
      }
      this.status = status;
      return entityTrackingStatus;
   }

   public TypeFilterableList<T> getCollection() {
      return this.collection;
   }

   @Override
   public void lithium$setPos(long pos) {
      this.pos = pos;
   }

   @Override
   public long lithium$getPos() {
      return this.pos;
   }

   @Override
   public void lithium$addListener(SectionedEntityMovementTracker<?> listener) {
      this.visibilityListeners.add(listener);
      if (this.status.shouldTrack()) {
         listener.onSectionEnteredRange(this);
      }
   }

   @Override
   public void lithium$removeListener(SectionedEntityCache<?> cache, SectionedEntityMovementTracker<?> listener) {
      boolean removed = this.visibilityListeners.remove(listener);
      if (removed && this.status.shouldTrack()) {
         listener.onSectionLeftRange(this);
      }
      if (this.isEmpty()) {
         cache.removeSection(this.pos);
      }
   }

   @Override
   public void lithium$trackEntityMovement(int notificationMask, long time) {
      int index = Integer.numberOfTrailingZeros(notificationMask);
      while (index < this.lastEntityMovementByType.length) {
         this.lastEntityMovementByType[index] = time;
         ArrayList<SectionedEntityMovementTracker<?>> listeners = this.movementListenersByType[index];
         if (listeners != null) {
            for (int i = listeners.size() - 1; i >= 0; i--) {
               listeners.remove(i).emitEntityMovement(notificationMask, this);
            }
         }
         notificationMask &= -2 << index;
         index = Integer.numberOfTrailingZeros(notificationMask);
      }
   }

   @Override
   public long lithium$getChangeTime(int trackedClass) {
      return this.lastEntityMovementByType[trackedClass];
   }

   @Override
   public <E extends Entity> void lithium$listenToMovementOnce(SectionedEntityMovementTracker<E> listener, int trackedClass) {
      ArrayList<SectionedEntityMovementTracker<?>> listeners = this.movementListenersByType[trackedClass];
      if (listeners == null) {
         listeners = this.movementListenersByType[trackedClass] = new ArrayList<>();
      }
      listeners.add(listener);
   }

   @Override
   public <E extends Entity> void lithium$removeListenToMovementOnce(SectionedEntityMovementTracker<E> listener, int trackedClass) {
      ArrayList<SectionedEntityMovementTracker<?>> listeners = this.movementListenersByType[trackedClass];
      if (listeners != null) {
         listeners.remove(listener);
      }
   }

   @Debug
   public int size() {
      return this.collection.size();
   }
}
