package net.caffeinemc.mods.lithium.common.world;

import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Collections;
import java.util.function.Predicate;
import net.caffeinemc.mods.lithium.common.entity.EntityClassGroup;
import net.caffeinemc.mods.lithium.common.entity.pushable.EntityPushablePredicate;
import net.caffeinemc.mods.lithium.common.world.chunk.ClassGroupFilterableList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.jetbrains.annotations.Nullable;

public final class WorldHelper {
   private WorldHelper() {
   }

   public static List<Entity> getEntitiesForCollision(EntityView view, Box box, @Nullable Entity collidingEntity) {
      if (view instanceof World world
         && (collidingEntity == null || !EntityClassGroup.CUSTOM_COLLIDE_LIKE_MINECART_BOAT_WINDCHARGE.contains(collidingEntity))) {
         SectionedEntityCache<Entity> cache = getEntityCacheOrNull(world);
         if (cache != null) {
            return getEntitiesOfEntityGroupWithoutDragonPieces(
               cache, collidingEntity, EntityClassGroup.NoDragonClassGroup.BOAT_SHULKER_LIKE_COLLISION, box, null
            );
         }
      }
      return view.getOtherEntities(collidingEntity, box);
   }

   public static List<Entity> getOtherEntitiesForCollision(
      EntityView view, Box box, @Nullable Entity collidingEntity, Predicate<? super Entity> filter
   ) {
      if (view instanceof World world
         && (collidingEntity == null || !EntityClassGroup.CUSTOM_COLLIDE_LIKE_MINECART_BOAT_WINDCHARGE.contains(collidingEntity))) {
         SectionedEntityCache<Entity> cache = getEntityCacheOrNull(world);
         if (cache != null) {
            return getEntitiesOfEntityGroupWithoutDragonPieces(
               cache, collidingEntity, EntityClassGroup.NoDragonClassGroup.BOAT_SHULKER_LIKE_COLLISION, box, filter
            );
         }
      }
      return view.getOtherEntities(collidingEntity, box, filter);
   }

   @Nullable
   public static SectionedEntityCache<Entity> getEntityCacheOrNull(World world) {
      return world.lithium$getEntityCache();
   }

   public static ArrayList<Entity> getEntitiesOfEntityGroupWithoutDragonPieces(
      SectionedEntityCache<Entity> cache,
      @Nullable Entity excludedEntity,
      EntityClassGroup entityClassGroup,
      Box box,
      @Nullable Predicate<? super Entity> filter
   ) {
      ArrayList<Entity> entities = new ArrayList<>();
      cache.forEachInBox(box, section -> {
         Collection<Entity> candidates = ((ClassGroupFilterableList<Entity>)section.getCollection()).lithium$getAllOfGroupType(entityClassGroup);
         for (Entity entity : candidates) {
            if (entity.getBoundingBox().intersects(box)
               && !entity.isSpectator()
               && entity != excludedEntity
               && (filter == null || filter.test(entity))) {
               entities.add(entity);
            }
         }
         return LazyIterationConsumer.NextIteration.CONTINUE;
      });
      return entities;
   }

   public static List<Entity> getEntitiesOfEntityGroupPlusDragonPieces(
      World world,
      SectionedEntityCache<Entity> cache,
      @Nullable Entity excludedEntity,
      EntityClassGroup entityClassGroup,
      Box box,
      @Nullable Predicate<? super Entity> filter
   ) {
      ArrayList<Entity> entities = getEntitiesOfEntityGroupWithoutDragonPieces(cache, excludedEntity, entityClassGroup, box, filter);
      for (EnderDragonPart part : world.getEnderDragonParts()) {
         if (part != excludedEntity
            && part.owner != excludedEntity
            && part.getBoundingBox().intersects(box)
            && (filter == null || filter.test(part))) {
            entities.add(part);
         }
      }
      return entities;
   }

   public static List<Entity> getPushableEntities(
      World world,
      SectionedEntityCache<Entity> cache,
      @Nullable Entity except,
      Box box,
      EntityPushablePredicate<? super Entity> predicate
   ) {
      ArrayList<Entity> entities = new ArrayList<>();
      cache.forEachInBox(
         box,
         section -> ((ClimbingMobCachingSection)section).lithium$collectPushableEntities(world, except, box, predicate, entities)
      );
      return entities;
   }

   public static List<Entity> getPushableEntities(
      World world, @Nullable Entity except, Box box, Predicate<? super Entity> predicate
   ) {
      if (predicate == Predicates.alwaysFalse()) {
         return Collections.emptyList();
      }
      if (predicate instanceof EntityPushablePredicate<?> pushablePredicate) {
         SectionedEntityCache<Entity> cache = getEntityCacheOrNull(world);
         if (cache != null) {
            return getPushableEntities(world, cache, except, box, (EntityPushablePredicate<? super Entity>)pushablePredicate);
         }
      }
      return world.getOtherEntities(except, box, predicate);
   }
}
