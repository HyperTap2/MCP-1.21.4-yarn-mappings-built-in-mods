package net.caffeinemc.mods.lithium.common.entity;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceReferenceImmutablePair;
import java.lang.reflect.Method;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.jetbrains.annotations.Nullable;

public class EntityClassGroup {
   private static final byte UNKNOWN = 3;
   public static final EntityClassGroup CUSTOM_COLLIDE_LIKE_MINECART_BOAT_WINDCHARGE = new EntityClassGroup(
      (entityClass, entityType) -> overrides(entityClass, "collidesWith", Entity.class)
   );
   private final BiPredicate<Class<?>, Supplier<EntityType<?>>> evaluator;
   private volatile Reference2ByteOpenHashMap<Class<?>> classes;
   @Nullable
   private volatile ObjectOpenHashSet<ReferenceReferenceImmutablePair<Class<?>, EntityType<?>>> classAndTypePairs;

   public EntityClassGroup(BiPredicate<Class<?>, Supplier<EntityType<?>>> evaluator) {
      this.evaluator = evaluator;
      this.clear();
   }

   public void clear() {
      this.classes = new Reference2ByteOpenHashMap<>();
      this.classes.defaultReturnValue(UNKNOWN);
      this.classAndTypePairs = null;
   }

   public boolean contains(Entity entity) {
      return this.contains(entity.getClass(), entity.getType());
   }

   public boolean contains(Class<?> entityClass, EntityType<?> entityType) {
      byte result = this.classes.getByte(entityClass);
      if (result < 2) {
         return result == 1;
      }
      if (result == UNKNOWN) {
         return this.testAndAddClass(entityClass, entityType);
      }
      ObjectOpenHashSet<ReferenceReferenceImmutablePair<Class<?>, EntityType<?>>> pairs = this.classAndTypePairs;
      return pairs != null && pairs.contains(ReferenceReferenceImmutablePair.of(entityClass, entityType));
   }

   private boolean testAndAddClass(Class<?> entityClass, EntityType<?> entityType) {
      synchronized (this) {
         if (this.classes.containsKey(entityClass)) {
            return this.contains(entityClass, entityType);
         }
         boolean[] accessedType = new boolean[1];
         boolean contains = this.evaluator.test(entityClass, () -> {
            accessedType[0] = true;
            return entityType;
         });
         Reference2ByteOpenHashMap<Class<?>> copy = this.classes.clone();
         copy.put(entityClass, (byte)(accessedType[0] ? 2 : contains ? 1 : 0));
         if (accessedType[0] && contains) {
            ObjectOpenHashSet<ReferenceReferenceImmutablePair<Class<?>, EntityType<?>>> pairs = this.classAndTypePairs;
            pairs = pairs == null ? new ObjectOpenHashSet<>() : pairs.clone();
            pairs.add(ReferenceReferenceImmutablePair.of(entityClass, entityType));
            this.classAndTypePairs = pairs;
         }
         this.classes = copy;
         return contains;
      }
   }

   private static boolean overrides(Class<?> entityClass, String methodName, Class<?>... parameterTypes) {
      Class<?> current = entityClass;
      while (current != null && current != Entity.class) {
         try {
            Method ignored = current.getDeclaredMethod(methodName, parameterTypes);
            return true;
         } catch (NoSuchMethodException ignored) {
            current = current.getSuperclass();
         }
      }
      return false;
   }

   public static final class NoDragonClassGroup extends EntityClassGroup {
      public static final NoDragonClassGroup BOAT_SHULKER_LIKE_COLLISION = new NoDragonClassGroup(
         (entityClass, entityType) -> !EnderDragonEntity.class.isAssignableFrom(entityClass) && overrides(entityClass, "isCollidable")
      );

      private NoDragonClassGroup(BiPredicate<Class<?>, Supplier<EntityType<?>>> evaluator) {
         super(evaluator);
      }
   }
}
