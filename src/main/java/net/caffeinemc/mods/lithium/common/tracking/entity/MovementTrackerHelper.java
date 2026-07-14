package net.caffeinemc.mods.lithium.common.tracking.entity;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.List;
import net.caffeinemc.mods.lithium.api.inventory.LithiumInventory;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.entity.EntityLike;

public final class MovementTrackerHelper {
   public static final List<Class<?>> MOVEMENT_NOTIFYING_ENTITY_CLASSES = LithiumInventory.class.isAssignableFrom(HopperBlockEntity.class)
      ? List.of(ItemEntity.class, Inventory.class)
      : List.of();
   public static final int NUM_MOVEMENT_NOTIFYING_CLASSES = MOVEMENT_NOTIFYING_ENTITY_CLASSES.size();
   private static volatile Reference2IntOpenHashMap<Class<? extends EntityLike>> classToNotifyMask = createMaskMap();

   private MovementTrackerHelper() {
   }

   public static int getNotificationMask(Entity entity) {
      int notificationMask = classToNotifyMask.getInt(entity.getClass());
      if (notificationMask != -1) {
         return notificationMask;
      }

      int mask = 0;
      Class<?> entityClass = entity.getClass();
      for (int i = 0; i < MOVEMENT_NOTIFYING_ENTITY_CLASSES.size(); i++) {
         if (MOVEMENT_NOTIFYING_ENTITY_CLASSES.get(i).isAssignableFrom(entityClass)) {
            mask |= 1 << i;
         }
      }

      Reference2IntOpenHashMap<Class<? extends EntityLike>> copy = classToNotifyMask.clone();
      copy.put(entity.getClass(), mask);
      classToNotifyMask = copy;
      return mask;
   }

   static int getTrackerIndex(Object entityClass) {
      return entityClass instanceof Class<?> ? MOVEMENT_NOTIFYING_ENTITY_CLASSES.indexOf(entityClass) : -1;
   }

   private static Reference2IntOpenHashMap<Class<? extends EntityLike>> createMaskMap() {
      Reference2IntOpenHashMap<Class<? extends EntityLike>> map = new Reference2IntOpenHashMap<>();
      map.defaultReturnValue(-1);
      return map;
   }
}
