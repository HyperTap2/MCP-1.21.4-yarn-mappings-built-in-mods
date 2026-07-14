package net.minecraft.world.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

public interface Palette<T> {
   int index(T object);

   boolean hasAny(Predicate<T> predicate);

   T get(int id);

   void readPacket(PacketByteBuf buf);

   void writePacket(PacketByteBuf buf);

   int getPacketSize();

   int getSize();

   Palette<T> copy(PaletteResizeListener<T> resizeListener);

   interface Factory {
      <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> list);
   }
}
