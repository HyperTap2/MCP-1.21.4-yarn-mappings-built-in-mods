package net.minecraft.world.chunk;

import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IndexedIterable;

public interface ReadableContainer<T> {
   T get(int x, int y, int z);

   void forEachValue(Consumer<T> action);

   void writePacket(PacketByteBuf buf);

   int getPacketSize();

   boolean hasAny(Predicate<T> predicate);

   void count(PalettedContainer.Counter<T> counter);

   PalettedContainer<T> copy();

   PalettedContainer<T> slice();

   ReadableContainer.Serialized<T> serialize(IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider);

   interface Reader<T, C extends ReadableContainer<T>> {
      DataResult<C> read(IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider, ReadableContainer.Serialized<T> serialize);
   }

   record Serialized<T>(List<T> paletteEntries, Optional<LongStream> storage) {
   }
}
