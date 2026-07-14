package net.minecraft.world.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.AbstractIterator;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.caffeinemc.mods.lithium.common.util.Pos;
import net.caffeinemc.mods.lithium.common.util.collections.ListeningLong2ObjectOpenHashMap;
import net.caffeinemc.mods.lithium.common.world.interests.RegionBasedStorageSectionExtended;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class SerializingRegionBasedStorage<R, P> implements AutoCloseable, RegionBasedStorageSectionExtended<R> {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String SECTIONS_KEY = "Sections";
   private final ChunkPosKeyedStorage storageAccess;
   private final Long2ObjectMap<Optional<R>> loadedElements;
   private final Long2ObjectOpenHashMap<BitSet> lithium$columns = new Long2ObjectOpenHashMap<>();
   private final LongLinkedOpenHashSet unsavedElements = new LongLinkedOpenHashSet();
   private final Codec<P> codec;
   private final Function<R, P> serializer;
   private final BiFunction<P, Runnable, R> deserializer;
   private final Function<Runnable, R> factory;
   private final DynamicRegistryManager registryManager;
   private final ChunkErrorHandler errorHandler;
   protected final HeightLimitView world;
   private final LongSet loadedChunks = new LongOpenHashSet();
   private final Long2ObjectMap<CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>>> pendingLoads = new Long2ObjectOpenHashMap();
   private final Object lock = new Object();

   public SerializingRegionBasedStorage(
      ChunkPosKeyedStorage storageAccess,
      Codec<P> codec,
      Function<R, P> serializer,
      BiFunction<P, Runnable, R> deserializer,
      Function<Runnable, R> factory,
      DynamicRegistryManager registryManager,
      ChunkErrorHandler errorHandler,
      HeightLimitView world
   ) {
      this.storageAccess = storageAccess;
      this.codec = codec;
      this.serializer = serializer;
      this.deserializer = deserializer;
      this.factory = factory;
      this.registryManager = registryManager;
      this.errorHandler = errorHandler;
      this.world = world;
      this.loadedElements = new ListeningLong2ObjectOpenHashMap<>(this::lithium$onEntryAdded, this::lithium$onEntryRemoved);
   }

   private void lithium$onEntryRemoved(long key, Optional<R> value) {
      int y = Pos.SectionYIndex.fromSectionCoord(this.world, ChunkSectionPos.unpackY(key));
      if (y >= 0 && y < Pos.SectionYIndex.getNumYSections(this.world)) {
         long columnPos = ChunkPos.toLong(ChunkSectionPos.unpackX(key), ChunkSectionPos.unpackZ(key));
         BitSet flags = this.lithium$columns.get(columnPos);
         if (flags != null) {
            flags.clear(y);
            if (flags.isEmpty()) {
               this.lithium$columns.remove(columnPos);
            }
         }
      }
   }

   private void lithium$onEntryAdded(long key, Optional<R> value) {
      int y = Pos.SectionYIndex.fromSectionCoord(this.world, ChunkSectionPos.unpackY(key));
      if (y >= 0 && y < Pos.SectionYIndex.getNumYSections(this.world)) {
         long columnPos = ChunkPos.toLong(ChunkSectionPos.unpackX(key), ChunkSectionPos.unpackZ(key));
         BitSet flags = this.lithium$columns.get(columnPos);
         if (flags == null) {
            flags = new BitSet(Pos.SectionYIndex.getNumYSections(this.world));
            this.lithium$columns.put(columnPos, flags);
         }
         flags.set(y, value.isPresent());
      }
   }

   @Override
   public Stream<R> lithium$getWithinChunkColumn(int chunkX, int chunkZ) {
      BitSet sections = this.lithium$getNonEmptyPoiSections(chunkX, chunkZ);
      if (sections.isEmpty()) {
         return Stream.empty();
      }
      List<R> values = new ArrayList<>();
      int minSection = Pos.SectionYCoord.getMinYSection(this.world);
      for (int index = sections.nextSetBit(0); index != -1; index = sections.nextSetBit(index + 1)) {
         Optional<R> value = this.loadedElements.get(ChunkSectionPos.asLong(chunkX, index + minSection, chunkZ));
         if (value != null) {
            value.ifPresent(values::add);
         }
      }
      return values.stream();
   }

   @Override
   public Iterable<R> lithium$getInChunkColumn(int chunkX, int chunkZ) {
      BitSet sections = this.lithium$getNonEmptyPoiSections(chunkX, chunkZ);
      if (sections.isEmpty()) {
         return Collections::emptyIterator;
      }
      Long2ObjectMap<Optional<R>> loaded = this.loadedElements;
      HeightLimitView height = this.world;
      return () -> new AbstractIterator<R>() {
         private int nextBit = sections.nextSetBit(0);

         @Override
         protected R computeNext() {
            while (this.nextBit >= 0) {
               Optional<R> next = loaded.get(ChunkSectionPos.asLong(chunkX, Pos.SectionYCoord.fromSectionIndex(height, this.nextBit), chunkZ));
               this.nextBit = sections.nextSetBit(this.nextBit + 1);
               if (next != null && next.isPresent()) {
                  return next.get();
               }
            }
            return this.endOfData();
         }
      };
   }

   private BitSet lithium$getNonEmptyPoiSections(int chunkX, int chunkZ) {
      long pos = ChunkPos.toLong(chunkX, chunkZ);
      BitSet flags = this.lithium$getNonEmptySections(pos, false);
      if (flags != null) {
         return flags;
      }
      this.loadAndWait(new ChunkPos(pos));
      return this.lithium$getNonEmptySections(pos, true);
   }

   private BitSet lithium$getNonEmptySections(long pos, boolean required) {
      BitSet flags = this.lithium$columns.get(pos);
      if (flags == null && required) {
         throw new NullPointerException("No data is present for column: " + new ChunkPos(pos));
      }
      return flags;
   }

   protected void tick(BooleanSupplier shouldKeepTicking) {
      LongIterator longIterator = this.unsavedElements.iterator();

      while (longIterator.hasNext() && shouldKeepTicking.getAsBoolean()) {
         ChunkPos chunkPos = new ChunkPos(longIterator.nextLong());
         longIterator.remove();
         this.save(chunkPos);
      }

      this.tickPendingLoads();
   }

   private void tickPendingLoads() {
      synchronized (this.lock) {
         Iterator<Entry<CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>>>> iterator = Long2ObjectMaps.fastIterator(this.pendingLoads);

         while (iterator.hasNext()) {
            Entry<CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>>> entry = iterator.next();
            Optional<SerializingRegionBasedStorage.LoadResult<P>> optional = (Optional<SerializingRegionBasedStorage.LoadResult<P>>)((CompletableFuture)entry.getValue())
               .getNow(null);
            if (optional != null) {
               long l = entry.getLongKey();
               this.onLoad(new ChunkPos(l), optional.orElse(null));
               iterator.remove();
               this.loadedChunks.add(l);
            }
         }
      }
   }

   public void save() {
      if (!this.unsavedElements.isEmpty()) {
         this.unsavedElements.forEach(chunkPos -> this.save(new ChunkPos(chunkPos)));
         this.unsavedElements.clear();
      }
   }

   public boolean hasUnsavedElements() {
      return !this.unsavedElements.isEmpty();
   }

   @Nullable
   protected Optional<R> getIfLoaded(long pos) {
      return (Optional<R>)this.loadedElements.get(pos);
   }

   protected Optional<R> get(long pos) {
      if (this.isPosInvalid(pos)) {
         return Optional.empty();
      } else {
         Optional<R> optional = this.getIfLoaded(pos);
         if (optional != null) {
            return optional;
         } else {
            this.loadAndWait(ChunkSectionPos.from(pos).toChunkPos());
            optional = this.getIfLoaded(pos);
            if (optional == null) {
               throw (IllegalStateException)Util.getFatalOrPause(new IllegalStateException());
            } else {
               return optional;
            }
         }
      }
   }

   protected boolean isPosInvalid(long pos) {
      int i = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(pos));
      return this.world.isOutOfHeightLimit(i);
   }

   protected R getOrCreate(long pos) {
      if (this.isPosInvalid(pos)) {
         throw (IllegalArgumentException)Util.getFatalOrPause(new IllegalArgumentException("sectionPos out of bounds"));
      }

      Optional<R> optional = this.get(pos);
      if (optional.isPresent()) {
         return optional.get();
      }

      R object = this.factory.apply(() -> this.onUpdate(pos));
      this.loadedElements.put(pos, Optional.of(object));
      return object;
   }

   public CompletableFuture<?> load(ChunkPos chunkPos) {
      synchronized (this.lock) {
         long l = chunkPos.toLong();
         return this.loadedChunks.contains(l)
            ? CompletableFuture.completedFuture(null)
            : (CompletableFuture)this.pendingLoads.computeIfAbsent(l, pos -> this.loadNbt(chunkPos));
      }
   }

   private void loadAndWait(ChunkPos chunkPos) {
      long l = chunkPos.toLong();
      CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>> completableFuture;
      synchronized (this.lock) {
         if (!this.loadedChunks.add(l)) {
            return;
         }

         completableFuture = (CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>>)this.pendingLoads
            .computeIfAbsent(l, pos -> this.loadNbt(chunkPos));
      }

      this.onLoad(chunkPos, completableFuture.join().orElse(null));
      synchronized (this.lock) {
         this.pendingLoads.remove(l);
      }
   }

   private CompletableFuture<Optional<SerializingRegionBasedStorage.LoadResult<P>>> loadNbt(ChunkPos chunkPos) {
      RegistryOps<NbtElement> registryOps = this.registryManager.getOps(NbtOps.INSTANCE);
      return this.storageAccess
         .read(chunkPos)
         .thenApplyAsync(
            chunkNbt -> chunkNbt.map(nbt -> SerializingRegionBasedStorage.LoadResult.fromNbt(this.codec, registryOps, nbt, this.storageAccess, this.world)),
            Util.getMainWorkerExecutor().named("parseSection")
         )
         .exceptionally(throwable -> {
            if (throwable instanceof CompletionException) {
               throwable = throwable.getCause();
            }

            if (throwable instanceof IOException iOException) {
               LOGGER.error("Error reading chunk {} data from disk", chunkPos, iOException);
               this.errorHandler.onChunkLoadFailure(iOException, this.storageAccess.getStorageKey(), chunkPos);
               return Optional.empty();
            } else {
               throw new CompletionException(throwable);
            }
         });
   }

   private void onLoad(ChunkPos chunkPos, @Nullable SerializingRegionBasedStorage.LoadResult<P> result) {
      if (result == null) {
         for (int i = this.world.getBottomSectionCoord(); i <= this.world.getTopSectionCoord(); i++) {
            this.loadedElements.put(chunkSectionPosAsLong(chunkPos, i), Optional.empty());
         }
      } else {
         boolean bl = result.versionChanged();

         for (int j = this.world.getBottomSectionCoord(); j <= this.world.getTopSectionCoord(); j++) {
            long l = chunkSectionPosAsLong(chunkPos, j);
            Optional<R> optional = Optional.ofNullable(result.sectionsByY.get(j)).map(section -> this.deserializer.apply((P)section, () -> this.onUpdate(l)));
            this.loadedElements.put(l, optional);
            optional.ifPresent(object -> {
               this.onLoad(l);
               if (bl) {
                  this.onUpdate(l);
               }
            });
         }
      }
   }

   private void save(ChunkPos pos) {
      RegistryOps<NbtElement> registryOps = this.registryManager.getOps(NbtOps.INSTANCE);
      Dynamic<NbtElement> dynamic = this.serialize(pos, registryOps);
      NbtElement nbtElement = (NbtElement)dynamic.getValue();
      if (nbtElement instanceof NbtCompound) {
         this.storageAccess.set(pos, (NbtCompound)nbtElement).exceptionally(throwable -> {
            this.errorHandler.onChunkSaveFailure(throwable, this.storageAccess.getStorageKey(), pos);
            return null;
         });
      } else {
         LOGGER.error("Expected compound tag, got {}", nbtElement);
      }
   }

   private <T> Dynamic<T> serialize(ChunkPos chunkPos, DynamicOps<T> ops) {
      Map<T, T> map = Maps.newHashMap();

      for (int i = this.world.getBottomSectionCoord(); i <= this.world.getTopSectionCoord(); i++) {
         long l = chunkSectionPosAsLong(chunkPos, i);
         Optional<R> optional = (Optional<R>)this.loadedElements.get(l);
         if (optional != null && !optional.isEmpty()) {
            DataResult<T> dataResult = this.codec.encodeStart(ops, this.serializer.apply(optional.get()));
            String string = Integer.toString(i);
            dataResult.resultOrPartial(LOGGER::error).ifPresent(value -> map.put((T)ops.createString(string), (T)value));
         }
      }

      return new Dynamic(
         ops,
         ops.createMap(
            ImmutableMap.of(
               ops.createString("Sections"),
               ops.createMap(map),
               ops.createString("DataVersion"),
               ops.createInt(SharedConstants.getGameVersion().getSaveVersion().getId())
            )
         )
      );
   }

   private static long chunkSectionPosAsLong(ChunkPos chunkPos, int y) {
      return ChunkSectionPos.asLong(chunkPos.x, y, chunkPos.z);
   }

   protected void onLoad(long pos) {
   }

   protected void onUpdate(long pos) {
      Optional<R> optional = (Optional<R>)this.loadedElements.get(pos);
      if (optional != null && !optional.isEmpty()) {
         this.unsavedElements.add(ChunkPos.toLong(ChunkSectionPos.unpackX(pos), ChunkSectionPos.unpackZ(pos)));
      } else {
         LOGGER.warn("No data for position: {}", ChunkSectionPos.from(pos));
      }
   }

   static int getDataVersion(Dynamic<?> dynamic) {
      return dynamic.get("DataVersion").asInt(1945);
   }

   public void saveChunk(ChunkPos pos) {
      if (this.unsavedElements.remove(pos.toLong())) {
         this.save(pos);
      }
   }

   @Override
   public void close() throws IOException {
      this.storageAccess.close();
   }

   record LoadResult<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {

      public static <T> SerializingRegionBasedStorage.LoadResult<T> fromNbt(
         Codec<T> sectionCodec, DynamicOps<NbtElement> ops, NbtElement nbt, ChunkPosKeyedStorage storage, HeightLimitView world
      ) {
         Dynamic<NbtElement> dynamic = new Dynamic(ops, nbt);
         int i = SerializingRegionBasedStorage.getDataVersion(dynamic);
         int j = SharedConstants.getGameVersion().getSaveVersion().getId();
         boolean bl = i != j;
         Dynamic<NbtElement> dynamic2 = storage.update(dynamic, i);
         OptionalDynamic<NbtElement> optionalDynamic = dynamic2.get("Sections");
         Int2ObjectMap<T> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int k = world.getBottomSectionCoord(); k <= world.getTopSectionCoord(); k++) {
            Optional<T> optional = optionalDynamic.get(Integer.toString(k))
               .result()
               .flatMap(section -> sectionCodec.parse(section).resultOrPartial(SerializingRegionBasedStorage.LOGGER::error));
            if (optional.isPresent()) {
               int2ObjectMap.put(k, optional.get());
            }
         }

         return new SerializingRegionBasedStorage.LoadResult<>(int2ObjectMap, bl);
      }
   }
}
