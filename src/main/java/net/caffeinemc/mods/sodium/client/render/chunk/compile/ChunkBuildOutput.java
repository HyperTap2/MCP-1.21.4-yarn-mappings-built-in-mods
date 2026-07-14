package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;

public class ChunkBuildOutput extends ChunkSortOutput {
   public final BuiltSectionInfo info;
   public final TranslucentData translucentData;
   public final Map<TerrainRenderPass, BuiltSectionMeshParts> meshes;

   public ChunkBuildOutput(
      RenderSection render, int buildTime, TranslucentData translucentData, BuiltSectionInfo info, Map<TerrainRenderPass, BuiltSectionMeshParts> meshes
   ) {
      super(render, buildTime);
      this.info = info;
      this.translucentData = translucentData;
      this.meshes = meshes;
   }

   public BuiltSectionMeshParts getMesh(TerrainRenderPass pass) {
      return this.meshes.get(pass);
   }

   @Override
   public void destroy() {
      super.destroy();

      for (BuiltSectionMeshParts data : this.meshes.values()) {
         data.getVertexData().free();
      }
   }
}
