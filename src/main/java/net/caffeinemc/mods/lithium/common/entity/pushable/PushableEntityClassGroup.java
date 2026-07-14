package net.caffeinemc.mods.lithium.common.entity.pushable;

import java.lang.reflect.Method;
import net.caffeinemc.mods.lithium.common.entity.EntityClassGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class PushableEntityClassGroup {
   public static final EntityClassGroup CACHABLE_UNPUSHABILITY = new EntityClassGroup((entityClass, entityType) -> {
      if (!LivingEntity.class.isAssignableFrom(entityClass)
         || PlayerEntity.class.isAssignableFrom(entityClass)
         || overridesBefore(entityClass, LivingEntity.class, "isClimbing")) {
         return false;
      }
      Class<?> pushableBase = CreakingEntity.class.isAssignableFrom(entityClass)
         ? CreakingEntity.class
         : WardenEntity.class.isAssignableFrom(entityClass) ? WardenEntity.class : LivingEntity.class;
      return !overridesBefore(entityClass, pushableBase, "isPushable");
   });

   public static final EntityClassGroup MAYBE_PUSHABLE = new EntityClassGroup((entityClass, entityType) -> {
      if (!overridesBefore(entityClass, Entity.class, "isPushable")) {
         return PlayerEntity.class.isAssignableFrom(entityClass);
      }
      if (EnderDragonEntity.class.isAssignableFrom(entityClass)) {
         return false;
      }
      if (ArmorStandEntity.class.isAssignableFrom(entityClass)) {
         return overridesBefore(entityClass, ArmorStandEntity.class, "isPushable");
      }
      return !BatEntity.class.isAssignableFrom(entityClass) || overridesBefore(entityClass, BatEntity.class, "isPushable");
   });

   private PushableEntityClassGroup() {
   }

   private static boolean overridesBefore(Class<?> entityClass, Class<?> baseClass, String methodName, Class<?>... parameterTypes) {
      Class<?> current = entityClass;
      while (current != null && current != baseClass && baseClass.isAssignableFrom(current)) {
         try {
            Method ignored = current.getDeclaredMethod(methodName, parameterTypes);
            return true;
         } catch (NoSuchMethodException exception) {
            current = current.getSuperclass();
         } catch (LinkageError | SecurityException exception) {
            return true;
         }
      }
      return false;
   }
}
