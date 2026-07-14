package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPNode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPResult;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3dc;

public class DynamicBSPData extends DynamicData {
   private static final int NODE_REUSE_MIN_GENERATION = 1;
   private final BSPNode rootNode;
   private final int generation;

   private DynamicBSPData(ChunkSectionPos sectionPos, int vertexCount, BSPResult result, Vector3dc initialCameraPos, TQuad[] quads, int generation) {
      super(sectionPos, vertexCount, quads.length, result, initialCameraPos);
      this.rootNode = result.getRootNode();
      this.generation = generation;
   }

   @Override
   public Sorter getSorter() {
      return new DynamicBSPData.DynamicBSPSorter(this.getQuadCount());
   }

   public static DynamicBSPData fromMesh(int vertexCount, CombinedCameraPos cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, TranslucentData oldData) {
      BSPNode oldRoot = null;
      int generation = 0;
      boolean prepareNodeReuse = false;
      if (oldData instanceof DynamicBSPData oldBSPData) {
         generation = oldBSPData.generation + 1;
         oldRoot = oldBSPData.rootNode;
         prepareNodeReuse = generation >= 1;
      }

      BSPResult result = BSPNode.buildBSP(quads, sectionPos, oldRoot, prepareNodeReuse);
      DynamicBSPData dynamicData = new DynamicBSPData(sectionPos, vertexCount, result, cameraPos.getAbsoluteCameraPos(), quads, generation);
      result.prepareIntegration();
      return dynamicData;
   }

   private class DynamicBSPSorter extends DynamicSorter {
      private DynamicBSPSorter(int quadCount) {
         super(quadCount);
      }

      @Override
      void writeSort(CombinedCameraPos cameraPos, boolean initial) {
         DynamicBSPData.this.rootNode.collectSortedQuads(this.getIndexBuffer(), cameraPos.getRelativeCameraPos());
      }
   }
}
