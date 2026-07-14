package net.caffeinemc.mods.sodium.client.render.chunk.compile.executor;

import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.SharedConstants;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkBuilder {
   public static final int HIGH_EFFORT = 10;
   public static final int LOW_EFFORT = 1;
   public static final int EFFORT_PER_THREAD_PER_FRAME = 11;
   private static final float HIGH_EFFORT_BUDGET_FACTOR = 0.90909094F;
   static final Logger LOGGER = LogManager.getLogger("ChunkBuilder");
   private final ChunkJobQueue queue = new ChunkJobQueue();
   private final List<Thread> threads = new ArrayList<>();
   private final AtomicInteger busyThreadCount = new AtomicInteger();
   private final ChunkBuildContext localContext;

   public ChunkBuilder(ClientWorld level, ChunkVertexType vertexType) {
      int count = getThreadCount();

      for (int i = 0; i < count; i++) {
         ChunkBuildContext context = new ChunkBuildContext(level, vertexType);
         ChunkBuilder.WorkerRunnable worker = new ChunkBuilder.WorkerRunnable("Chunk Render Task Executor #" + i, context);
         Thread thread = new Thread(worker, "Chunk Render Task Executor #" + i);
         thread.setPriority(Math.max(0, 3));
         thread.start();
         this.threads.add(thread);
      }

      LOGGER.info("Started {} worker threads", this.threads.size());
      this.localContext = new ChunkBuildContext(level, vertexType);
   }

   private int getTotalRemainingBudget() {
      return Math.max(0, this.threads.size() * 11 - this.queue.getEffortSum());
   }

   public int getHighEffortSchedulingBudget() {
      return Math.max(10, (int)(this.getTotalRemainingBudget() * 0.90909094F));
   }

   public int getLowEffortSchedulingBudget() {
      return Math.max(1, this.getTotalRemainingBudget() - this.getHighEffortSchedulingBudget());
   }

   public void shutdown() {
      if (!this.queue.isRunning()) {
         throw new IllegalStateException("Worker threads are not running");
      } else {
         for (ChunkJob job : this.queue.shutdown()) {
            job.setCancelled();
         }

         this.shutdownThreads();
      }
   }

   private void shutdownThreads() {
      LOGGER.info("Stopping worker threads");

      for (Thread thread : this.threads) {
         try {
            thread.join();
         } catch (InterruptedException var4) {
         }
      }

      this.threads.clear();
   }

   public <TASK extends ChunkBuilderTask<OUTPUT>, OUTPUT extends BuilderTaskOutput> ChunkJobTyped<TASK, OUTPUT> scheduleTask(
      TASK task, boolean important, Consumer<ChunkJobResult<OUTPUT>> consumer
   ) {
      Validate.notNull(task, "Task must be non-null", new Object[0]);
      if (!this.queue.isRunning()) {
         throw new IllegalStateException("Executor is stopped");
      } else {
         ChunkJobTyped<TASK, OUTPUT> job = new ChunkJobTyped<>(task, consumer);
         this.queue.add(job, important);
         return job;
      }
   }

   private static int getOptimalThreadCount() {
      return MathHelper.clamp(Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6), 1, 10);
   }

   private static int getThreadCount() {
      int requested = SodiumClientMod.options().performance.chunkBuilderThreads;
      return requested == 0 ? getOptimalThreadCount() : Math.min(requested, getMaxThreadCount());
   }

   private static int getMaxThreadCount() {
      return Runtime.getRuntime().availableProcessors();
   }

   public void tryStealTask(ChunkJob job) {
      if (this.queue.stealJob(job)) {
         ChunkBuildContext localContext = this.localContext;

         try {
            job.execute(localContext);
         } finally {
            localContext.cleanup();
         }
      }
   }

   public boolean isBuildQueueEmpty() {
      return this.queue.isEmpty();
   }

   public int getScheduledJobCount() {
      return this.queue.size();
   }

   public int getScheduledEffort() {
      return this.queue.getEffortSum();
   }

   public int getBusyThreadCount() {
      return this.busyThreadCount.get();
   }

   public int getTotalThreadCount() {
      return this.threads.size();
   }

   private class WorkerRunnable implements Runnable {
      private final String name;
      private final ChunkBuildContext context;

      public WorkerRunnable(String name, ChunkBuildContext context) {
         this.name = name;
         this.context = context;
      }

      @Override
      public void run() {
         while (ChunkBuilder.this.queue.isRunning()) {
            ChunkJob job;
            try {
               job = ChunkBuilder.this.queue.waitForNextJob();
            } catch (InterruptedException var7) {
               continue;
            }

            if (job != null) {
               ChunkBuilder.this.busyThreadCount.getAndIncrement();
               Zone zone = TracyClient.beginZone(this.name, SharedConstants.isDevelopment);

               try {
                  job.execute(this.context);
               } finally {
                  this.context.cleanup();
                  ChunkBuilder.this.busyThreadCount.decrementAndGet();
               }

               zone.close();
            }
         }
      }
   }
}
