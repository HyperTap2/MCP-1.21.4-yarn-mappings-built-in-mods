package net.minecraft.resource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

public interface SynchronousResourceReloader extends ResourceReloader {
   @Override
   default CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
      return synchronizer.whenPrepared(Unit.INSTANCE).thenRunAsync(() -> {
         Profiler profiler = Profilers.get();
         profiler.push("listener");
         this.reload(manager);
         profiler.pop();
      }, applyExecutor);
   }

   void reload(ResourceManager manager);
}
