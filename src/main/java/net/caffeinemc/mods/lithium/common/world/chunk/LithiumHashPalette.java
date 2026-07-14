package net.caffeinemc.mods.lithium.common.world.chunk;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.world.chunk.EntryMissingException;
import net.minecraft.world.chunk.Palette;
import net.minecraft.world.chunk.PaletteResizeListener;

public final class LithiumHashPalette<T> implements Palette<T> {
   private final IndexedIterable<T> idList;
   private final PaletteResizeListener<T> resizeListener;
   private final int indexBits;
   private final Reference2IntOpenHashMap<T> table;
   private T[] entries;
   private int size;

   public LithiumHashPalette(IndexedIterable<T> idList, int bits, PaletteResizeListener<T> resizeListener, List<T> entries) {
      this(idList, bits, resizeListener);
      for (T entry : entries) this.addEntry(entry);
   }

   public LithiumHashPalette(IndexedIterable<T> idList, int bits, PaletteResizeListener<T> resizeListener) {
      this.idList = idList;
      this.resizeListener = resizeListener;
      this.indexBits = bits;
      this.entries = (T[])new Object[1 << bits];
      this.table = new Reference2IntOpenHashMap<>(1 << bits, 0.5F);
      this.table.defaultReturnValue(-1);
   }

   private LithiumHashPalette(
      IndexedIterable<T> idList, PaletteResizeListener<T> resizeListener, int bits,
      T[] entries, Reference2IntOpenHashMap<T> table, int size
   ) {
      this.idList = idList;
      this.resizeListener = resizeListener;
      this.indexBits = bits;
      this.entries = entries;
      this.table = table;
      this.size = size;
   }

   public static <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> entries) {
      return new LithiumHashPalette<>(idList, bits, listener, entries);
   }

   @Override
   public int index(T object) {
      int id = this.table.getInt(object);
      if (id != -1) return id;
      id = this.addEntry(object);
      return id < 1 << this.indexBits ? id : this.resizeListener.onResize(this.indexBits + 1, object);
   }

   private int addEntry(T object) {
      int id = this.size;
      if (id >= this.entries.length) {
         this.entries = Arrays.copyOf(this.entries, HashCommon.nextPowerOfTwo(id + 1));
      }
      this.table.put(object, id);
      this.entries[id] = object;
      this.size++;
      return id;
   }

   @Override
   public boolean hasAny(Predicate<T> predicate) {
      for (int i = 0; i < this.size; i++) if (predicate.test(this.entries[i])) return true;
      return false;
   }

   @Override
   public T get(int id) {
      T value = id >= 0 && id < this.size ? this.entries[id] : null;
      if (value == null) throw new EntryMissingException(id);
      return value;
   }

   @Override
   public void readPacket(PacketByteBuf buf) {
      Arrays.fill(this.entries, null);
      this.table.clear();
      this.size = 0;
      int count = buf.readVarInt();
      for (int i = 0; i < count; i++) this.addEntry(this.idList.getOrThrow(buf.readVarInt()));
   }

   @Override
   public void writePacket(PacketByteBuf buf) {
      buf.writeVarInt(this.size);
      for (int i = 0; i < this.size; i++) buf.writeVarInt(this.idList.getRawId(this.entries[i]));
   }

   @Override
   public int getPacketSize() {
      int bytes = VarInts.getSizeInBytes(this.size);
      for (int i = 0; i < this.size; i++) bytes += VarInts.getSizeInBytes(this.idList.getRawId(this.entries[i]));
      return bytes;
   }

   @Override public int getSize() { return this.size; }

   @Override
   public Palette<T> copy(PaletteResizeListener<T> resizeListener) {
      return new LithiumHashPalette<>(this.idList, resizeListener, this.indexBits, this.entries.clone(), this.table.clone(), this.size);
   }

   public List<T> getElements() {
      return Arrays.asList(Arrays.copyOf(this.entries, this.size));
   }
}
