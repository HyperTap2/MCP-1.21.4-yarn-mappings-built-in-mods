package net.minecraft.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.caffeinemc.mods.sodium.client.world.BitStorageExtension;
import net.caffeinemc.mods.sodium.client.world.PalettedContainerROExtension;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.collection.EmptyPaletteStorage;
import net.minecraft.util.collection.IndexedIterable;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import net.caffeinemc.mods.lithium.common.world.chunk.LithiumHashPalette;
import net.caffeinemc.mods.lithium.common.world.chunk.CompactingPackedIntegerArray;
import org.jetbrains.annotations.Nullable;

public class PalettedContainer<T> implements PaletteResizeListener<T>, ReadableContainer<T>, PalettedContainerROExtension<T> {
   private static final int field_34557 = 0;
   private static final ThreadLocal<short[]> LITHIUM_CACHED_ARRAY_4096 = ThreadLocal.withInitial(() -> new short[4096]);
   private static final ThreadLocal<short[]> LITHIUM_CACHED_ARRAY_64 = ThreadLocal.withInitial(() -> new short[64]);
   private final PaletteResizeListener<T> dummyListener = (newSize, added) -> 0;
   private final IndexedIterable<T> idList;
   private volatile PalettedContainer.Data<T> data;
   private final PalettedContainer.PaletteProvider paletteProvider;
   public void lock() {
   }

   public void unlock() {
   }

   public static <T> Codec<PalettedContainer<T>> createPalettedContainerCodec(
      IndexedIterable<T> idList, Codec<T> entryCodec, PalettedContainer.PaletteProvider paletteProvider, T defaultValue
   ) {
      ReadableContainer.Reader<T, PalettedContainer<T>> reader = PalettedContainer::read;
      return createCodec(idList, entryCodec, paletteProvider, defaultValue, reader);
   }

   public static <T> Codec<ReadableContainer<T>> createReadableContainerCodec(
      IndexedIterable<T> idList, Codec<T> entryCodec, PalettedContainer.PaletteProvider paletteProvider, T defaultValue
   ) {
      ReadableContainer.Reader<T, ReadableContainer<T>> reader = (idListx, paletteProviderx, serialized) -> read(idListx, paletteProviderx, serialized)
         .map(result -> result);
      return createCodec(idList, entryCodec, paletteProvider, defaultValue, reader);
   }

   private static <T, C extends ReadableContainer<T>> Codec<C> createCodec(
      IndexedIterable<T> idList, Codec<T> entryCodec, PalettedContainer.PaletteProvider provider, T defaultValue, ReadableContainer.Reader<T, C> reader
   ) {
      return RecordCodecBuilder.<ReadableContainer.Serialized>create(
                      p_318428_2_ -> p_318428_2_.group(
                                      entryCodec.mapResult(Codecs.orElsePartial(defaultValue))
                                              .listOf()
                                              .fieldOf("palette")
                                              .forGetter(ReadableContainer.Serialized::paletteEntries),
                                      Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(ReadableContainer.Serialized::storage)
                              )
                              .apply(p_318428_2_, ReadableContainer.Serialized::new)
              )
              .comapFlatMap(
                      serialized -> reader.read(idList, provider, (ReadableContainer.Serialized<T>) serialized),
                      container -> container.serialize(idList, provider)
              );
   }

   public PalettedContainer(
      IndexedIterable<T> idList,
      PalettedContainer.PaletteProvider paletteProvider,
      PalettedContainer.DataProvider<T> dataProvider,
      PaletteStorage storage,
      List<T> paletteEntries
   ) {
      this.idList = idList;
      this.paletteProvider = paletteProvider;
      this.data = new PalettedContainer.Data<>(dataProvider, storage, dataProvider.factory().create(dataProvider.bits(), idList, this, paletteEntries));
   }

   private PalettedContainer(IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider, PalettedContainer.Data<T> data) {
      this.idList = idList;
      this.paletteProvider = paletteProvider;
      this.data = data;
   }

   private PalettedContainer(PalettedContainer<T> container) {
      this.idList = container.idList;
      this.paletteProvider = container.paletteProvider;
      this.data = container.data.copy(this);
   }

   public PalettedContainer(IndexedIterable<T> idList, T object, PalettedContainer.PaletteProvider paletteProvider) {
      this.paletteProvider = paletteProvider;
      this.idList = idList;
      this.data = this.getCompatibleData(null, 0);
      this.data.palette.index(object);
   }

   private PalettedContainer.Data<T> getCompatibleData(@Nullable PalettedContainer.Data<T> previousData, int bits) {
      PalettedContainer.DataProvider<T> dataProvider = this.paletteProvider.createDataProvider(this.idList, bits);
      return previousData != null && dataProvider.equals(previousData.configuration())
         ? previousData
         : dataProvider.createData(this.idList, this, this.paletteProvider.getContainerSize());
   }

   @Override
   public int onResize(int i, T object) {
      PalettedContainer.Data<T> data = this.data;
      PalettedContainer.Data<T> data2 = this.getCompatibleData(data, i);
      data2.importFrom(data.palette, data.storage);
      this.data = data2;
      return data2.palette.index(object);
   }

   public T swap(int x, int y, int z, T value) {
      this.lock();

      try {
         return this.swap(this.paletteProvider.computeIndex(x, y, z), value);
      } finally {
         this.unlock();
      }
   }

   public T swapUnsafe(int x, int y, int z, T value) {
      return this.swap(this.paletteProvider.computeIndex(x, y, z), value);
   }

   private T swap(int index, T value) {
      int i = this.data.palette.index(value);
      int j = this.data.storage.swap(index, i);
      return this.data.palette.get(j);
   }

   public void set(int x, int y, int z, T value) {
      this.lock();

      try {
         this.set(this.paletteProvider.computeIndex(x, y, z), value);
      } finally {
         this.unlock();
      }
   }

   private void set(int index, T value) {
      int i = this.data.palette.index(value);
      this.data.storage.set(index, i);
   }

   @Override
   public T get(int x, int y, int z) {
      return this.get(this.paletteProvider.computeIndex(x, y, z));
   }

   protected T get(int index) {
      PalettedContainer.Data<T> data = this.data;
      return data.palette.get(data.storage.get(index));
   }

   @Override
   public void forEachValue(Consumer<T> action) {
      Palette<T> palette = this.data.palette();
      IntSet intSet = new IntArraySet();
      this.data.storage.forEach(intSet::add);
      intSet.forEach(id -> action.accept(palette.get(id)));
   }

   public void readPacket(PacketByteBuf buf) {
      this.lock();

      try {
         int i = buf.readByte();
         PalettedContainer.Data<T> data = this.getCompatibleData(this.data, i);
         data.palette.readPacket(buf);
         buf.readLongArray(data.storage.getData());
         this.data = data;
      } finally {
         this.unlock();
      }
   }

   @Override
   public void writePacket(PacketByteBuf buf) {
      this.lock();

      try {
         this.data.writePacket(buf);
      } finally {
         this.unlock();
      }
   }

   private static <T> DataResult<PalettedContainer<T>> read(
      IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider, ReadableContainer.Serialized<T> serialized
   ) {
      List<T> list = serialized.paletteEntries();
      int i = paletteProvider.getContainerSize();
      int j = paletteProvider.getBits(idList, list.size());
      PalettedContainer.DataProvider<T> dataProvider = paletteProvider.createDataProvider(idList, j);
      PaletteStorage paletteStorage;
      if (j == 0) {
         paletteStorage = new EmptyPaletteStorage(i);
      } else {
         Optional<LongStream> optional = serialized.storage();
         if (optional.isEmpty()) {
            return DataResult.error(() -> "Missing values for non-zero storage");
         }

         long[] ls = optional.get().toArray();

         try {
            if (dataProvider.factory() == PalettedContainer.PaletteProvider.ID_LIST) {
               Palette<T> palette = new BiMapPalette<>(idList, j, (id, value) -> 0, list);
               PackedIntegerArray packedIntegerArray = new PackedIntegerArray(j, i, ls);
               int[] is = new int[i];
               packedIntegerArray.writePaletteIndices(is);
               applyEach(is, id -> idList.getRawId(palette.get(id)));
               paletteStorage = new PackedIntegerArray(dataProvider.bits(), i, is);
            } else {
               paletteStorage = new PackedIntegerArray(dataProvider.bits(), i, ls);
            }
         } catch (PackedIntegerArray.InvalidLengthException invalidLengthException) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + invalidLengthException.getMessage());
         }
      }

      return DataResult.success(new PalettedContainer<>(idList, paletteProvider, dataProvider, paletteStorage, list));
   }

   @Override
   public ReadableContainer.Serialized<T> serialize(IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider) {
      this.lock();

      try {
         Palette<T> sourcePalette = this.data.palette;
         PaletteStorage sourceStorage = this.data.storage;
         if (sourceStorage instanceof EmptyPaletteStorage || sourcePalette.getSize() == 1) {
            return new ReadableContainer.Serialized<>(List.of(sourcePalette.get(0)), Optional.empty());
         }

         LithiumHashPalette<T> originalHashPalette = null;
         if (sourcePalette instanceof LithiumHashPalette<?> palette) {
            originalHashPalette = (LithiumHashPalette<T>)palette;
         }
         LithiumHashPalette<T> compactedPalette = new LithiumHashPalette<>(idList, sourceStorage.getElementBits(), this.dummyListener);
         short[] compactedIndices = lithium$getCachedArray(paletteProvider.getContainerSize());
         ((CompactingPackedIntegerArray)sourceStorage).lithium$compact(sourcePalette, compactedPalette, compactedIndices);

         Optional<LongStream> storage;
         List<T> paletteEntries;
         int serializedBits = paletteProvider.getBits(idList, compactedPalette.getSize());
         if (originalHashPalette != null
            && originalHashPalette.getSize() == compactedPalette.getSize()
            && sourceStorage.getElementBits() == serializedBits) {
            storage = Optional.of(Arrays.stream(sourceStorage.getData().clone()));
            paletteEntries = originalHashPalette.getElements();
         } else {
            if (serializedBits == 0) {
               storage = Optional.empty();
            } else {
               PackedIntegerArray packed = new PackedIntegerArray(serializedBits, compactedIndices.length);
               for (int index = 0; index < compactedIndices.length; index++) {
                  packed.set(index, compactedIndices[index]);
               }
               storage = Optional.of(Arrays.stream(packed.getData()));
            }
            paletteEntries = compactedPalette.getElements();
         }

         return new ReadableContainer.Serialized<>(paletteEntries, storage);
      } finally {
         this.unlock();
      }
   }

   private static <T> void applyEach(int[] is, IntUnaryOperator applier) {
      int i = -1;
      int j = -1;

      for (int k = 0; k < is.length; k++) {
         int l = is[k];
         if (l != i) {
            i = l;
            j = applier.applyAsInt(l);
         }

         is[k] = j;
      }
   }

   @Override
   public int getPacketSize() {
      return this.data.getPacketSize();
   }

   @Override
   public boolean hasAny(Predicate<T> predicate) {
      return this.data.palette.hasAny(predicate);
   }

   @Override
   public PalettedContainer<T> copy() {
      return new PalettedContainer<>(this);
   }

   @Override
   public void sodium$unpack(T[] values) {
      if (values.length != this.paletteProvider.getContainerSize()) {
         throw new IllegalArgumentException("Array is wrong size");
      }
      PalettedContainer.Data<T> currentData = Objects.requireNonNull(this.data, "PalettedContainer must have data");
      ((BitStorageExtension)currentData.storage()).sodium$unpack(values, currentData.palette());
   }

   @Override
   public void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      if (values.length != this.paletteProvider.getContainerSize()) {
         throw new IllegalArgumentException("Array is wrong size");
      }
      PalettedContainer.Data<T> currentData = Objects.requireNonNull(this.data, "PalettedContainer must have data");
      for (int y = minY; y <= maxY; y++) {
         for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
               int index = this.paletteProvider.computeIndex(x, y, z);
               values[index] = currentData.palette().get(currentData.storage().get(index));
            }
         }
      }
   }

   @Override
   public ReadableContainer<T> sodium$copy() {
      return this.copy();
   }

   @Override
   public PalettedContainer<T> slice() {
      return new PalettedContainer<>(this.idList, this.data.palette.get(0), this.paletteProvider);
   }

   @Override
   public void count(PalettedContainer.Counter<T> counter) {
      int paletteSize = this.data.palette.getSize();
      if (paletteSize <= 4096) {
         short[] counts = new short[paletteSize];
         this.data.storage.forEach(index -> counts[index]++);
         for (int index = 0; index < counts.length; index++) {
            T value = this.data.palette.get(index);
            if (value != null) {
               counter.accept(value, counts[index]);
            }
         }
         return;
      }

      Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
      this.data.storage.forEach(key -> counts.addTo(key, 1));
      counts.int2IntEntrySet().forEach(entry -> counter.accept(this.data.palette.get(entry.getIntKey()), entry.getIntValue()));
   }

   private static short[] lithium$getCachedArray(int size) {
      return switch (size) {
         case 64 -> LITHIUM_CACHED_ARRAY_64.get();
         case 4096 -> LITHIUM_CACHED_ARRAY_4096.get();
         default -> new short[size];
      };
   }

   @FunctionalInterface
   public interface Counter<T> {
      void accept(T object, int count);
   }

   record Data<T>(PalettedContainer.DataProvider<T> configuration, PaletteStorage storage, Palette<T> palette) {

      public void importFrom(Palette<T> palette, PaletteStorage storage) {
         for (int i = 0; i < storage.getSize(); i++) {
            T object = palette.get(storage.get(i));
            this.storage.set(i, this.palette.index(object));
         }
      }

      public int getPacketSize() {
         return 1 + this.palette.getPacketSize() + VarInts.getSizeInBytes(this.storage.getData().length) + this.storage.getData().length * 8;
      }

      public void writePacket(PacketByteBuf buf) {
         buf.writeByte(this.storage.getElementBits());
         this.palette.writePacket(buf);
         buf.writeLongArray(this.storage.getData());
      }

      public PalettedContainer.Data<T> copy(PaletteResizeListener<T> resizeListener) {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy(resizeListener));
      }
   }

   record DataProvider<T>(Palette.Factory factory, int bits) {
      public PalettedContainer.Data<T> createData(IndexedIterable<T> idList, PaletteResizeListener<T> listener, int size) {
         PaletteStorage paletteStorage = this.bits == 0 ? new EmptyPaletteStorage(size) : new PackedIntegerArray(this.bits, size);
         Palette<T> palette = this.factory.create(this.bits, idList, listener, List.of());
         return new PalettedContainer.Data<>(this, paletteStorage, palette);
      }
   }

   public abstract static class PaletteProvider {
      public static final Palette.Factory SINGULAR = SingularPalette::create;
      public static final Palette.Factory ARRAY = ArrayPalette::create;
      public static final Palette.Factory BI_MAP = BiMapPalette::create;
      public static final Palette.Factory LITHIUM_HASH = LithiumHashPalette::create;
      static final Palette.Factory ID_LIST = IdListPalette::create;
      private static final PalettedContainer.DataProvider<?>[] BLOCK_STATE_DATA_PROVIDERS = {
         new PalettedContainer.DataProvider<>(SINGULAR, 0),
         new PalettedContainer.DataProvider<>(ARRAY, 4),
         new PalettedContainer.DataProvider<>(ARRAY, 4),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 4),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 4),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 5),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 6),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 7),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 8)
      };
      private static final PalettedContainer.DataProvider<?>[] BIOME_DATA_PROVIDERS = {
         new PalettedContainer.DataProvider<>(SINGULAR, 0),
         new PalettedContainer.DataProvider<>(ARRAY, 1),
         new PalettedContainer.DataProvider<>(ARRAY, 2),
         new PalettedContainer.DataProvider<>(LITHIUM_HASH, 3)
      };
      public static final PalettedContainer.PaletteProvider BLOCK_STATE = new PalettedContainer.PaletteProvider(4) {
         @Override
         public <A> PalettedContainer.DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
            return bits >= 0 && bits < BLOCK_STATE_DATA_PROVIDERS.length
               ? (PalettedContainer.DataProvider<A>)BLOCK_STATE_DATA_PROVIDERS[bits]
               : new PalettedContainer.DataProvider<>(ID_LIST, MathHelper.ceilLog2(idList.size()));
         }
      };
      public static final PalettedContainer.PaletteProvider BIOME = new PalettedContainer.PaletteProvider(2) {
         @Override
         public <A> PalettedContainer.DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits) {
            return bits >= 0 && bits < BIOME_DATA_PROVIDERS.length
               ? (PalettedContainer.DataProvider<A>)BIOME_DATA_PROVIDERS[bits]
               : new PalettedContainer.DataProvider<>(ID_LIST, MathHelper.ceilLog2(idList.size()));
         }
      };
      private final int edgeBits;

      PaletteProvider(int edgeBits) {
         this.edgeBits = edgeBits;
      }

      public int getContainerSize() {
         return 1 << this.edgeBits * 3;
      }

      public int computeIndex(int x, int y, int z) {
         return (y << this.edgeBits | z) << this.edgeBits | x;
      }

      public abstract <A> PalettedContainer.DataProvider<A> createDataProvider(IndexedIterable<A> idList, int bits);

      <A> int getBits(IndexedIterable<A> idList, int size) {
         int i = MathHelper.ceilLog2(size);
         PalettedContainer.DataProvider<A> dataProvider = this.createDataProvider(idList, i);
         return dataProvider.factory() == ID_LIST ? i : dataProvider.bits();
      }
   }
}
