package net.minecraft.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface ResourceReloader {
   CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor);

   default String getName() {
      return this.getClass().getSimpleName();
   }

   interface Synchronizer {
      <T> CompletableFuture<T> whenPrepared(T preparedObject);
   }
}
