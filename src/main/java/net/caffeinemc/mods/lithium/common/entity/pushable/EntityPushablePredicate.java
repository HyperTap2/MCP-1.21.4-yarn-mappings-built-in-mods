package net.caffeinemc.mods.lithium.common.entity.pushable;

import java.util.function.Predicate;

public abstract class EntityPushablePredicate<T> implements Predicate<T> {
   public static <T> Predicate<T> and(Predicate<? super T> first, Predicate<? super T> second) {
      return new EntityPushablePredicate<>() {
         @Override
         public boolean test(T entity) {
            return first.test(entity) && second.test(entity);
         }
      };
   }
}
