package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.function.IntConsumer;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.util.collections.BitArray;
import org.joml.Vector3fc;

public class TopoGraphSorting {
   private static final float HALF_SPACE_EPSILON = 0.001F;

   private TopoGraphSorting() {
   }

   private static boolean pointOutsideHalfSpace(float planeDistance, Vector3fc planeNormal, Vector3fc point) {
      return planeNormal.dot(point) > planeDistance;
   }

   private static boolean pointInsideHalfSpaceEpsilon(float planeDistance, Vector3fc planeNormal, float x, float y, float z) {
      return planeNormal.dot(x, y, z) + 0.001F < planeDistance;
   }

   private static boolean pointOutsideHalfSpaceEpsilon(float planeDistance, Vector3fc planeNormal, float x, float y, float z) {
      return planeNormal.dot(x, y, z) - 0.001F > planeDistance;
   }

   public static boolean orthogonalQuadVisibleThrough(TQuad quadA, TQuad quadB) {
      int aDirection = quadA.getFacing().ordinal();
      int aOpposite = quadA.getFacing().getOpposite().ordinal();
      int bDirection = quadB.getFacing().ordinal();
      int aSign = quadA.getFacing().getSign();
      int bSign = quadB.getFacing().getSign();
      float[] aExtents = quadA.getExtents();
      float[] bExtents = quadB.getExtents();
      float BIntoADescent = aSign * aExtents[aDirection] - aSign * bExtents[aOpposite];
      float AOutsideBAscent = bSign * aExtents[bDirection] - bSign * bExtents[bDirection];
      boolean vis = BIntoADescent > 0.0F && AOutsideBAscent > 0.0F;
      return vis && TQuad.extentsIntersect(aExtents, bExtents) ? BIntoADescent + AOutsideBAscent > 1.0F : vis;
   }

   private static boolean testSeparatorRange(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal, Vector3fc normal, float start, float end) {
      float[] distances = (float[])distancesByNormal.get(normal);
      return distances == null ? false : AlignableNormal.queryRange(distances, start, end);
   }

   private static boolean visibilityWithSeparator(
      TQuad quadA, TQuad quadB, Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal, Vector3fc cameraPos
   ) {
      for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
         ModelQuadFacing facing = ModelQuadFacing.VALUES[direction];
         ModelQuadFacing oppositeFacing = facing.getOpposite();
         int oppositeDirection = oppositeFacing.ordinal();
         int sign = facing.getSign();
         float separatorRangeStart = sign * quadB.getExtents()[direction];
         float separatorRangeEnd = sign * quadA.getExtents()[oppositeDirection];
         if (!(separatorRangeStart > separatorRangeEnd)) {
            Vector3fc facingNormal = ModelQuadFacing.ALIGNED_NORMALS[direction];
            float cameraDistance = facingNormal.dot(cameraPos);
            if (!(cameraDistance > separatorRangeEnd) && testSeparatorRange(distancesByNormal, facingNormal, cameraDistance, separatorRangeEnd)) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean quadVisibleThrough(TQuad quad, TQuad other, Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal, Vector3fc cameraPos) {
      if (quad == other) {
         return false;
      } else {
         ModelQuadFacing quadFacing = quad.getFacing();
         ModelQuadFacing otherFacing = other.getFacing();
         boolean result = false;
         if (quadFacing != ModelQuadFacing.UNASSIGNED && otherFacing != ModelQuadFacing.UNASSIGNED) {
            if (quadFacing.getOpposite() == otherFacing) {
               return false;
            }

            if (quadFacing == otherFacing) {
               int sign = quadFacing.getSign();
               int direction = quadFacing.ordinal();
               result = sign * quad.getExtents()[direction] > sign * other.getExtents()[direction];
            } else {
               result = orthogonalQuadVisibleThrough(quad, other);
            }
         } else {
            float quadDot = quad.getAccurateDotProduct();
            Vector3fc quadNormal = quad.getAccurateNormal();
            float[] otherVertexPositions = other.getVertexPositions();
            boolean otherInsideQuad = false;
            int i = 0;

            for (int itemIndex = 0; i < 4; i++) {
               if (pointInsideHalfSpaceEpsilon(
                  quadDot, quadNormal, otherVertexPositions[itemIndex++], otherVertexPositions[itemIndex++], otherVertexPositions[itemIndex++]
               )) {
                  otherInsideQuad = true;
                  break;
               }
            }

            if (otherInsideQuad) {
               float otherDot = other.getAccurateDotProduct();
               Vector3fc otherNormal = other.getAccurateNormal();
               float[] quadVertexPositions = quad.getVertexPositions();
               boolean quadNotFullyInsideOther = false;
               int ix = 0;

               for (int itemIndexx = 0; ix < 4; ix++) {
                  if (pointOutsideHalfSpaceEpsilon(
                     otherDot, otherNormal, quadVertexPositions[itemIndexx++], quadVertexPositions[itemIndexx++], quadVertexPositions[itemIndexx++]
                  )) {
                     quadNotFullyInsideOther = true;
                     break;
                  }
               }

               result = quadNotFullyInsideOther;
            }
         }

         return result && distancesByNormal != null ? visibilityWithSeparator(quad, other, distancesByNormal, cameraPos) : result;
      }
   }

   public static boolean topoGraphSort(
      IntConsumer indexConsumer, TQuad[] allQuads, Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal, Vector3fc cameraPos
   ) {
      int[] activeToRealIndex = null;
      int quadCount = 0;
      TQuad[] quads;
      if (cameraPos != null) {
         quads = new TQuad[allQuads.length];
         activeToRealIndex = new int[allQuads.length];

         for (int i = 0; i < allQuads.length; i++) {
            TQuad quad = allQuads[i];
            if (pointOutsideHalfSpace(quad.getAccurateDotProduct(), quad.getAccurateNormal(), cameraPos)) {
               activeToRealIndex[quadCount] = i;
               quads[quadCount] = quad;
               quadCount++;
            } else {
               indexConsumer.accept(i);
            }
         }
      } else {
         quads = allQuads;
         quadCount = allQuads.length;
      }

      return topoGraphSort(indexConsumer, quads, quadCount, activeToRealIndex, distancesByNormal, cameraPos);
   }

   public static boolean topoGraphSort(
      IntConsumer indexConsumer,
      TQuad[] quads,
      int quadCount,
      int[] activeToRealIndex,
      Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal,
      Vector3fc cameraPos
   ) {
      if (quadCount == 0) {
         return true;
      } else if (quadCount == 1) {
         if (activeToRealIndex != null) {
            indexConsumer.accept(activeToRealIndex[0]);
         } else {
            indexConsumer.accept(0);
         }

         return true;
      } else if (quadCount == 2) {
         int a = 0;
         int b = 1;
         if (quadVisibleThrough(quads[a], quads[b], null, null)) {
            a = 1;
            b = 0;
         }

         if (activeToRealIndex != null) {
            indexConsumer.accept(activeToRealIndex[a]);
            indexConsumer.accept(activeToRealIndex[b]);
         } else {
            indexConsumer.accept(a);
            indexConsumer.accept(b);
         }

         return true;
      } else {
         BitArray unvisited = new BitArray(quadCount);
         unvisited.set(0, quadCount);
         int visitedCount = 0;
         BitArray onStack = new BitArray(quadCount);
         int[] stack = new int[quadCount];
         int[] nextEdge = new int[quadCount];

         while (visitedCount < quadCount) {
            int stackPos = 0;
            int root = unvisited.nextSetBit(0);
            stack[stackPos] = root;
            onStack.set(root);
            nextEdge[stackPos] = 0;

            while (stackPos >= 0) {
               int currentQuadIndex = stack[stackPos];
               int nextEdgeTest = unvisited.nextSetBit(nextEdge[stackPos]);
               if (nextEdgeTest != -1) {
                  if (currentQuadIndex != nextEdgeTest) {
                     TQuad currentQuad = quads[currentQuadIndex];
                     TQuad nextQuad = quads[nextEdgeTest];
                     if (quadVisibleThrough(currentQuad, nextQuad, distancesByNormal, cameraPos)) {
                        if (onStack.getAndSet(nextEdgeTest)) {
                           return false;
                        }

                        nextEdge[stackPos] = nextEdgeTest + 1;
                        stack[++stackPos] = nextEdgeTest;
                        nextEdge[stackPos] = 0;
                        continue;
                     }
                  }

                  if (++nextEdgeTest < quadCount) {
                     nextEdge[stackPos] = nextEdgeTest;
                     continue;
                  }
               }

               onStack.unset(currentQuadIndex);
               visitedCount++;
               unvisited.unset(currentQuadIndex);
               stackPos--;
               if (activeToRealIndex != null) {
                  indexConsumer.accept(activeToRealIndex[currentQuadIndex]);
               } else {
                  indexConsumer.accept(currentQuadIndex);
               }
            }
         }

         return true;
      }
   }
}
