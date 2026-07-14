package net.minecraft.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

public abstract class SinglePreparationResourceReloader<T> implements ResourceReloader {
   @Override
   public final CompletableFuture<Void> reload(
      ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor
   ) {
      return CompletableFuture.<T>supplyAsync(() -> this.prepare(manager, Profilers.get()), prepareExecutor)
         .thenCompose(synchronizer::whenPrepared)
         .thenAcceptAsync(prepared -> this.apply((T)prepared, manager, Profilers.get()), applyExecutor);
   }

   protected abstract T prepare(ResourceManager manager, Profiler profiler);

   protected abstract void apply(T prepared, ResourceManager manager, Profiler profiler);
}
