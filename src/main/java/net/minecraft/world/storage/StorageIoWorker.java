package net.minecraft.world.storage;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.nbt.scanner.NbtScanner;
import net.minecraft.nbt.scanner.SelectiveNbtCollector;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.PrioritizedConsecutiveExecutor;
import net.minecraft.util.thread.TaskQueue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class StorageIoWorker implements NbtScannable, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicBoolean closed = new AtomicBoolean();
   private final PrioritizedConsecutiveExecutor executor;
   private final RegionBasedStorage storage;
   private final SequencedMap<ChunkPos, StorageIoWorker.Result> results = new LinkedHashMap<>();
   private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> blendingStatusCaches = new Long2ObjectLinkedOpenHashMap();
   private static final int MAX_CACHE_SIZE = 1024;

   protected StorageIoWorker(StorageKey storageKey, Path directory, boolean dsync) {
      this.storage = new RegionBasedStorage(storageKey, directory, dsync);
      this.executor = new PrioritizedConsecutiveExecutor(StorageIoWorker.Priority.values().length, Util.getIoWorkerExecutor(), "IOWorker-" + storageKey.type());
   }

   public boolean needsBlending(ChunkPos chunkPos, int checkRadius) {
      ChunkPos chunkPos2 = new ChunkPos(chunkPos.x - checkRadius, chunkPos.z - checkRadius);
      ChunkPos chunkPos3 = new ChunkPos(chunkPos.x + checkRadius, chunkPos.z + checkRadius);

      for (int i = chunkPos2.getRegionX(); i <= chunkPos3.getRegionX(); i++) {
         for (int j = chunkPos2.getRegionZ(); j <= chunkPos3.getRegionZ(); j++) {
            BitSet bitSet = this.getOrComputeBlendingStatus(i, j).join();
            if (!bitSet.isEmpty()) {
               ChunkPos chunkPos4 = ChunkPos.fromRegion(i, j);
               int k = Math.max(chunkPos2.x - chunkPos4.x, 0);
               int l = Math.max(chunkPos2.z - chunkPos4.z, 0);
               int m = Math.min(chunkPos3.x - chunkPos4.x, 31);
               int n = Math.min(chunkPos3.z - chunkPos4.z, 31);

               for (int o = k; o <= m; o++) {
                  for (int p = l; p <= n; p++) {
                     int q = p * 32 + o;
                     if (bitSet.get(q)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private CompletableFuture<BitSet> getOrComputeBlendingStatus(int chunkX, int chunkZ) {
      long l = ChunkPos.toLong(chunkX, chunkZ);
      synchronized (this.blendingStatusCaches) {
         CompletableFuture<BitSet> completableFuture = (CompletableFuture<BitSet>)this.blendingStatusCaches.getAndMoveToFirst(l);
         if (completableFuture == null) {
            completableFuture = this.computeBlendingStatus(chunkX, chunkZ);
            this.blendingStatusCaches.putAndMoveToFirst(l, completableFuture);
            if (this.blendingStatusCaches.size() > 1024) {
               this.blendingStatusCaches.removeLast();
            }
         }

         return completableFuture;
      }
   }

   private CompletableFuture<BitSet> computeBlendingStatus(int chunkX, int chunkZ) {
      return CompletableFuture.supplyAsync(
         () -> {
            ChunkPos chunkPos = ChunkPos.fromRegion(chunkX, chunkZ);
            ChunkPos chunkPos2 = ChunkPos.fromRegionCenter(chunkX, chunkZ);
            BitSet bitSet = new BitSet();
            ChunkPos.stream(chunkPos, chunkPos2)
               .forEach(
                  chunkPosx -> {
                     SelectiveNbtCollector selectiveNbtCollector = new SelectiveNbtCollector(
                        new NbtScanQuery(NbtInt.TYPE, "DataVersion"), new NbtScanQuery(NbtCompound.TYPE, "blending_data")
                     );

                     try {
                        this.scanChunk(chunkPosx, selectiveNbtCollector).join();
                     } catch (Exception exception) {
                        LOGGER.warn("Failed to scan chunk {}", chunkPosx, exception);
                        return;
                     }

                     if (selectiveNbtCollector.getRoot() instanceof NbtCompound nbtCompound && this.needsBlending(nbtCompound)) {
                        int ix = chunkPosx.getRegionRelativeZ() * 32 + chunkPosx.getRegionRelativeX();
                        bitSet.set(ix);
                     }
                  }
               );
            return bitSet;
         },
         Util.getMainWorkerExecutor()
      );
   }

   private boolean needsBlending(NbtCompound nbt) {
      return nbt.contains("DataVersion", 99) && nbt.getInt("DataVersion") >= 4185 ? nbt.contains("blending_data", 10) : true;
   }

   public CompletableFuture<Void> setResult(ChunkPos pos, @Nullable NbtCompound nbt) {
      return this.setResult(pos, () -> nbt);
   }

   public CompletableFuture<Void> setResult(ChunkPos pos, Supplier<NbtCompound> nbtSupplier) {
      return this.run((Callable<CompletableFuture<Void>>) () -> {
         NbtCompound nbtCompound = nbtSupplier.get();
         Result result = this.results.computeIfAbsent(pos, pos2 -> new Result(nbtCompound));
         result.nbt = nbtCompound;
         return result.future;
      }).thenCompose(Function.identity());
   }

   public CompletableFuture<Optional<NbtCompound>> readChunkData(ChunkPos pos) {
      return this.run((Callable<Optional<NbtCompound>>) () -> {
         StorageIoWorker.Result result = this.results.get(pos);
         if (result != null) {
            return Optional.ofNullable(result.copyNbt());
         }

         try {
            NbtCompound nbtCompound = this.storage.getTagAt(pos);
            return Optional.ofNullable(nbtCompound);
         } catch (Exception exception) {
            LOGGER.warn("Failed to read chunk {}", pos, exception);
            throw exception;
         }
      });
   }

   public CompletableFuture<Void> completeAll(boolean sync) {
      CompletableFuture<Void> completableFuture = this.run(
                      (Callable<CompletableFuture<Void>>) () -> CompletableFuture.allOf(this.results.values().stream().map(result -> result.future).toArray(CompletableFuture[]::new))
              )
         .thenCompose(Function.identity());
      return sync ? completableFuture.thenCompose(void_ -> this.run((Callable<Void>) () -> {
         try {
            this.storage.sync();
            return null;
         } catch (Exception exception) {
            LOGGER.warn("Failed to synchronize chunks", exception);
            throw exception;
         }
      })) : completableFuture.thenCompose(void_ -> this.run((Callable<Void>) () -> null));
   }

   @Override
   public CompletableFuture<Void> scanChunk(ChunkPos pos, NbtScanner scanner) {
      return this.run((Callable<Void>) () -> {
         try {
            Result result = this.results.get(pos);
            if (result != null) {
               if (result.nbt != null) {
                  result.nbt.accept(scanner);
               }
            } else {
               this.storage.scanChunk(pos, scanner);
            }

            return null;
         } catch (Exception exception) {
            LOGGER.warn("Failed to bulk scan chunk {}", pos, exception);
            throw exception;
         }
      });
   }

   private <T> CompletableFuture<T> run(StorageIoWorker.Callable<T> task) {
      return this.executor.executeAsync(StorageIoWorker.Priority.FOREGROUND.ordinal(), future -> {
         if (!this.closed.get()) {
            try {
               future.complete(task.get());
            } catch (Exception exception) {
               future.completeExceptionally(exception);
            }
         }

         this.writeRemainingResults();
      });
   }

   private <T> CompletableFuture<T> run(Supplier<T> task) {
      return this.executor.executeAsync(StorageIoWorker.Priority.FOREGROUND.ordinal(), completableFuture -> {
         if (!this.closed.get()) {
            completableFuture.complete(task.get());
         }

         this.writeRemainingResults();
      });
   }

   private void writeResult() {
      Entry<ChunkPos, StorageIoWorker.Result> entry = this.results.pollFirstEntry();
      if (entry != null) {
         this.write(entry.getKey(), entry.getValue());
         this.writeRemainingResults();
      }
   }

   private void writeRemainingResults() {
      this.executor.send(new TaskQueue.PrioritizedTask(StorageIoWorker.Priority.BACKGROUND.ordinal(), this::writeResult));
   }

   private void write(ChunkPos pos, StorageIoWorker.Result result) {
      try {
         this.storage.write(pos, result.nbt);
         result.future.complete(null);
      } catch (Exception exception) {
         LOGGER.error("Failed to store chunk {}", pos, exception);
         result.future.completeExceptionally(exception);
      }
   }

   @Override
   public void close() throws IOException {
      if (this.closed.compareAndSet(false, true)) {
         this.runRemainingTasks();
         this.executor.close();

         try {
            this.storage.close();
         } catch (Exception exception) {
            LOGGER.error("Failed to close storage", exception);
         }
      }
   }

   private void runRemainingTasks() {
      this.executor.executeAsync(StorageIoWorker.Priority.SHUTDOWN.ordinal(), future -> future.complete(Unit.INSTANCE)).join();
   }

   public StorageKey getStorageKey() {
      return this.storage.getStorageKey();
   }

   @FunctionalInterface
   interface Callable<T> {
      @Nullable
      T get() throws Exception;
   }

   enum Priority {
      FOREGROUND,
      BACKGROUND,
      SHUTDOWN;
   }

   static class Result {
      @Nullable
      NbtCompound nbt;
      final CompletableFuture<Void> future = new CompletableFuture<>();

      public Result(@Nullable NbtCompound nbt) {
         this.nbt = nbt;
      }

      @Nullable
      NbtCompound copyNbt() {
         NbtCompound nbtCompound = this.nbt;
         return nbtCompound == null ? null : nbtCompound.copy();
      }
   }
}
