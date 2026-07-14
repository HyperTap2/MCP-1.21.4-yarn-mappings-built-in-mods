package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.world.chunk.ReadableContainer;

public interface PalettedContainerROExtension<T> {
   static <T> PalettedContainerROExtension<T> of(ReadableContainer<T> container) {
      return (PalettedContainerROExtension<T>)container;
   }

   static <T> ReadableContainer<T> clone(ReadableContainer<T> container) {
      return container == null ? null : of(container).sodium$copy();
   }

   void sodium$unpack(T[] var1);

   void sodium$unpack(T[] var1, int var2, int var3, int var4, int var5, int var6, int var7);

   ReadableContainer<T> sodium$copy();
}
