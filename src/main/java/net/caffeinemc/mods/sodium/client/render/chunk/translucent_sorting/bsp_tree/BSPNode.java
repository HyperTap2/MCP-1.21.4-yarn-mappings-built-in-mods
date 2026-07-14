package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TopoGraphSorting;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3fc;

public abstract class BSPNode {
   abstract void collectSortedQuads(BSPSortState var1, Vector3fc var2);

   public void collectSortedQuads(NativeBuffer nativeBuffer, Vector3fc cameraPos) {
      this.collectSortedQuads(new BSPSortState(nativeBuffer), cameraPos);
   }

   public static BSPResult buildBSP(TQuad[] quads, ChunkSectionPos sectionPos, BSPNode oldRoot, boolean prepareNodeReuse) {
      InnerPartitionBSPNode.validateQuadCount(quads.length);
      BSPWorkspace workspace = new BSPWorkspace(quads, sectionPos, prepareNodeReuse);
      int[] initialIndexes = new int[quads.length];
      int i = 0;

      while (i < quads.length) {
         initialIndexes[i] = i++;
      }

      IntArrayList allIndexes = new IntArrayList(initialIndexes);
      BSPNode rootNode = build(workspace, allIndexes, -1, oldRoot);
      BSPResult result = workspace.result;
      result.setRootNode(rootNode);
      return result;
   }

   private static boolean doubleLeafPossible(TQuad quadA, TQuad quadB) {
      ModelQuadFacing facingA = quadA.getFacing();
      ModelQuadFacing facingB = quadB.getFacing();
      if (facingA.isAligned() && facingB.isAligned()) {
         if (quadA.getExtents()[facingA.ordinal()] == quadB.getExtents()[facingB.ordinal()]) {
            return true;
         } else {
            return facingA == facingB.getOpposite()
               ? true
               : !TopoGraphSorting.orthogonalQuadVisibleThrough(quadA, quadB) && !TopoGraphSorting.orthogonalQuadVisibleThrough(quadB, quadA);
         }
      } else {
         int packedNormalA = quadA.getPackedNormal();
         int packedNormalB = quadB.getPackedNormal();
         return NormI8.isOpposite(packedNormalA, packedNormalB)
            || packedNormalA == packedNormalB && quadA.getAccurateDotProduct() == quadB.getAccurateDotProduct();
      }
   }

   static BSPNode build(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
      depth++;
      if (indexes.isEmpty()) {
         return null;
      } else if (indexes.size() == 1) {
         return new LeafSingleBSPNode(indexes.getInt(0));
      } else {
         if (indexes.size() == 2) {
            int quadIndexA = indexes.getInt(0);
            int quadIndexB = indexes.getInt(1);
            TQuad quadA = workspace.quads[quadIndexA];
            TQuad quadB = workspace.quads[quadIndexB];
            if (doubleLeafPossible(quadA, quadB)) {
               return new LeafDoubleBSPNode(quadIndexA, quadIndexB);
            }
         }

         return InnerPartitionBSPNode.build(workspace, indexes, depth, oldNode);
      }
   }
}
