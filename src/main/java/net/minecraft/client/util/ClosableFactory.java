package net.minecraft.client.util;


public interface ClosableFactory<T> {
   T create();

   void close(T object);
}
