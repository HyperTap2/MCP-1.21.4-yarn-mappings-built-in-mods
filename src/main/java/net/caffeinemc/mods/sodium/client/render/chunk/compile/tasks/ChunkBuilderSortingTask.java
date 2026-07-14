package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.Sorter;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Vector3dc;

public class ChunkBuilderSortingTask extends ChunkBuilderTask<ChunkSortOutput> {
   private final Sorter sorter;

   public ChunkBuilderSortingTask(RenderSection render, int frame, Vector3dc absoluteCameraPos, Sorter sorter) {
      super(render, frame, absoluteCameraPos);
      this.sorter = sorter;
   }

   public ChunkSortOutput execute(ChunkBuildContext context, CancellationToken cancellationToken) {
      if (cancellationToken.isCancelled()) {
         return null;
      } else {
         Profiler profiler = Profilers.get();
         profiler.push("translucency sorting");
         this.sorter.writeIndexBuffer(this, false);
         profiler.pop();
         return new ChunkSortOutput(this.render, this.submitTime, this.sorter);
      }
   }

   public static ChunkBuilderSortingTask createTask(RenderSection render, int frame, Vector3dc absoluteCameraPos) {
      return render.getTranslucentData() instanceof DynamicData dynamicData
         ? new ChunkBuilderSortingTask(render, frame, absoluteCameraPos, dynamicData.getSorter())
         : null;
   }

   @Override
   public int getEffort() {
      return 1;
   }
}
