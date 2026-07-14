package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.CombinedCameraPos;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public abstract class ChunkBuilderTask<OUTPUT extends BuilderTaskOutput> implements CombinedCameraPos {
   protected final RenderSection render;
   protected final int submitTime;
   protected final Vector3dc absoluteCameraPos;
   protected final Vector3fc cameraPos;

   public ChunkBuilderTask(RenderSection render, int time, Vector3dc absoluteCameraPos) {
      this.render = render;
      this.submitTime = time;
      this.absoluteCameraPos = absoluteCameraPos;
      this.cameraPos = new Vector3f(
         (float)(absoluteCameraPos.x() - render.getOriginX()),
         (float)(absoluteCameraPos.y() - render.getOriginY()),
         (float)(absoluteCameraPos.z() - render.getOriginZ())
      );
   }

   public abstract OUTPUT execute(ChunkBuildContext var1, CancellationToken var2);

   public abstract int getEffort();

   @Override
   public Vector3fc getRelativeCameraPos() {
      return this.cameraPos;
   }

   @Override
   public Vector3dc getAbsoluteCameraPos() {
      return this.absoluteCameraPos;
   }
}
