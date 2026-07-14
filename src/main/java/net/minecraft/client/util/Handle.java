package net.minecraft.client.util;


public interface Handle<T> {
   Handle<?> EMPTY = () -> {
      throw new IllegalStateException("Cannot dereference handle with no underlying resource");
   };

   static <T> Handle<T> empty() {
      return (Handle<T>)EMPTY;
   }

   T get();
}
