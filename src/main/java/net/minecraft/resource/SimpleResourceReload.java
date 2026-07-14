package net.minecraft.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;

public class SimpleResourceReload<S> implements ResourceReload {
   private static final int FIRST_PREPARE_APPLY_WEIGHT = 2;
   private static final int SECOND_PREPARE_APPLY_WEIGHT = 2;
   private static final int RELOADER_WEIGHT = 1;
   protected final CompletableFuture<Unit> prepareStageFuture = new CompletableFuture<>();
   protected CompletableFuture<List<S>> applyStageFuture;
   final Set<ResourceReloader> waitingReloaders;
   private final int reloaderCount;
   private int toApplyCount;
   private int appliedCount;
   private final AtomicInteger toPrepareCount = new AtomicInteger();
   private final AtomicInteger preparedCount = new AtomicInteger();

   public static SimpleResourceReload<Void> create(
      ResourceManager manager, List<ResourceReloader> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage
   ) {
      return new SimpleResourceReload<>(
         prepareExecutor,
         applyExecutor,
         manager,
         reloaders,
         (synchronizer, resourceManager, reloader, prepare, apply) -> reloader.reload(synchronizer, resourceManager, prepareExecutor, apply),
         initialStage
      );
   }

   protected SimpleResourceReload(
      Executor prepareExecutor,
      Executor applyExecutor,
      ResourceManager manager,
      List<ResourceReloader> reloaders,
      SimpleResourceReload.Factory<S> factory,
      CompletableFuture<Unit> initialStage
   ) {
      this.reloaderCount = reloaders.size();
      this.toPrepareCount.incrementAndGet();
      initialStage.thenRun(this.preparedCount::incrementAndGet);
      List<CompletableFuture<S>> list = Lists.newArrayList();
      CompletableFuture<?> completableFuture = initialStage;
      this.waitingReloaders = Sets.newHashSet(reloaders);

      for (final ResourceReloader resourceReloader : reloaders) {
         final CompletableFuture<?> completableFuture2 = completableFuture;
         CompletableFuture<S> completableFuture3 = factory.create(
            new ResourceReloader.Synchronizer() {
               @Override
               public <T> CompletableFuture<T> whenPrepared(T preparedObject) {
                  applyExecutor.execute(() -> {
                     SimpleResourceReload.this.waitingReloaders.remove(resourceReloader);
                     if (SimpleResourceReload.this.waitingReloaders.isEmpty()) {
                        SimpleResourceReload.this.prepareStageFuture.complete(Unit.INSTANCE);
                     }
                  });
                  return SimpleResourceReload.this.prepareStageFuture
                     .thenCombine((CompletionStage<? extends T>)completableFuture2, (unit, object2) -> preparedObject);
               }
            },
            manager,
            resourceReloader,
            preparation -> {
               this.toPrepareCount.incrementAndGet();
               prepareExecutor.execute(() -> {
                  preparation.run();
                  this.preparedCount.incrementAndGet();
               });
            },
            application -> {
               this.toApplyCount++;
               applyExecutor.execute(() -> {
                  application.run();
                  this.appliedCount++;
               });
            }
         );
         list.add(completableFuture3);
         completableFuture = completableFuture3;
      }

      this.applyStageFuture = Util.combine(list);
   }

   @Override
   public CompletableFuture<?> whenComplete() {
      return this.applyStageFuture;
   }

   @Override
   public float getProgress() {
      int i = this.reloaderCount - this.waitingReloaders.size();
      float f = this.preparedCount.get() * 2 + this.appliedCount * 2 + i * 1;
      float g = this.toPrepareCount.get() * 2 + this.toApplyCount * 2 + this.reloaderCount * 1;
      return f / g;
   }

   public static ResourceReload start(
      ResourceManager manager,
      List<ResourceReloader> reloaders,
      Executor prepareExecutor,
      Executor applyExecutor,
      CompletableFuture<Unit> initialStage,
      boolean profiled
   ) {
      return profiled
         ? new ProfiledResourceReload(manager, reloaders, prepareExecutor, applyExecutor, initialStage)
         : create(manager, reloaders, prepareExecutor, applyExecutor, initialStage);
   }

   protected interface Factory<S> {
      CompletableFuture<S> create(
         ResourceReloader.Synchronizer synchronizer, ResourceManager manager, ResourceReloader reloader, Executor prepareExecutor, Executor applyExecutor
      );
   }
}
