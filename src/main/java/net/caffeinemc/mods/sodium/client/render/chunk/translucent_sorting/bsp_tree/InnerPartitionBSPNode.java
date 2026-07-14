package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Arrays;
import java.util.Random;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TopoGraphSorting;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3fc;

abstract class InnerPartitionBSPNode extends BSPNode {
   private static final int NODE_REUSE_THRESHOLD = 30;
   private static final int MAX_INTERSECTION_ATTEMPTS = 500;
   final Vector3fc planeNormal;
   final int axis;
   int[] indexMap;
   int fixedIndexOffset = Integer.MIN_VALUE;
   final InnerPartitionBSPNode.NodeReuseData reuseData;
   private static final int INTERVAL_START = 2;
   private static final int INTERVAL_END = 0;
   private static final int INTERVAL_SIDE = 1;

   InnerPartitionBSPNode(InnerPartitionBSPNode.NodeReuseData reuseData, int axis) {
      this.planeNormal = ModelQuadFacing.ALIGNED_NORMALS[axis];
      this.axis = axis;
      this.reuseData = reuseData;
   }

   abstract void addPartitionPlanes(BSPWorkspace var1);

   static InnerPartitionBSPNode.NodeReuseData prepareNodeReuse(BSPWorkspace workspace, IntArrayList indexes, int depth) {
      if (workspace.prepareNodeReuse && depth == 1 && indexes.size() > 30) {
         float[][] quadExtents = new float[indexes.size()][];
         int maxIndex = -1;

         for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.getInt(i);
            TQuad quad = workspace.quads[index];
            float[] extents = quad.getExtents();
            quadExtents[i] = extents;
            maxIndex = Math.max(maxIndex, index);
         }

         return new InnerPartitionBSPNode.NodeReuseData(quadExtents, BSPSortState.compressIndexes(indexes, false), indexes.size(), maxIndex);
      } else {
         return null;
      }
   }

   static InnerPartitionBSPNode attemptNodeReuse(BSPWorkspace workspace, IntArrayList newIndexes, InnerPartitionBSPNode oldNode) {
      if (oldNode == null) {
         return null;
      } else {
         oldNode.indexMap = null;
         oldNode.fixedIndexOffset = Integer.MIN_VALUE;
         InnerPartitionBSPNode.NodeReuseData reuseData = oldNode.reuseData;
         if (reuseData == null) {
            return null;
         } else {
            float[][] oldExtents = reuseData.quadExtents;
            if (oldExtents.length != newIndexes.size()) {
               return null;
            } else {
               for (int i = 0; i < newIndexes.size(); i++) {
                  if (!workspace.quads[newIndexes.getInt(i)].extentsEqual(oldExtents[i])) {
                     return null;
                  }
               }

               InnerPartitionBSPNode.IndexRemapper remapper = new InnerPartitionBSPNode.IndexRemapper(reuseData.maxIndex + 1, newIndexes);
               BSPSortState.decompressOrRead(reuseData.indexes, remapper);
               if (remapper.hasFixedOffset()) {
                  oldNode.fixedIndexOffset = remapper.firstOffset;
               } else {
                  oldNode.indexMap = remapper.indexMap;
               }

               oldNode.addPartitionPlanes(workspace);
               return oldNode;
            }
         }
      }
   }

   private static long encodeIntervalPoint(float distance, int quadIndex, int type) {
      return (long)MathUtil.floatToComparableInt(distance) << 32 | (long)type << 30 | quadIndex;
   }

   private static float decodeDistance(long encoded) {
      return MathUtil.comparableIntToFloat((int)(encoded >>> 32));
   }

   private static int decodeQuadIndex(long encoded) {
      return (int)(encoded & 1073741823L);
   }

   private static int decodeType(long encoded) {
      return (int)(encoded >>> 30) & 3;
   }

   public static void validateQuadCount(int quadCount) {
      if (quadCount * 2 > 1073741823) {
         throw new IllegalArgumentException("Too many quads: " + quadCount);
      }
   }

   static BSPNode build(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
      if (oldNode instanceof InnerPartitionBSPNode oldInnerNode) {
         InnerPartitionBSPNode reusedNode = attemptNodeReuse(workspace, indexes, oldInnerNode);
         if (reusedNode != null) {
            return reusedNode;
         }
      }

      ReferenceArrayList<Partition> partitions = new ReferenceArrayList();
      LongArrayList points = new LongArrayList((int)(indexes.size() * 1.5));

      for (int axisCount = 0; axisCount < 3; axisCount++) {
         int axis = (axisCount + depth + 1) % 3;
         int oppositeDirection = axis + 3;
         int alignedFacingBitmap = 0;
         boolean onlyIntervalSide = true;
         points.clear();
         IntListIterator distance = indexes.iterator();

         while (distance.hasNext()) {
            int quadIndex = (Integer)distance.next();
            TQuad quad = workspace.quads[quadIndex];
            float[] extents = quad.getExtents();
            float posExtent = extents[axis];
            float negExtent = extents[oppositeDirection];
            if (posExtent == negExtent) {
               points.add(encodeIntervalPoint(posExtent, quadIndex, 1));
            } else {
               points.add(encodeIntervalPoint(posExtent, quadIndex, 0));
               points.add(encodeIntervalPoint(negExtent, quadIndex, 2));
               onlyIntervalSide = false;
            }

            alignedFacingBitmap |= 1 << quad.getFacing().ordinal();
         }

         if (!ModelQuadFacing.bitmapHasUnassigned(alignedFacingBitmap)) {
            int alignedNormalCount = Integer.bitCount(alignedFacingBitmap);
            if (alignedNormalCount == 1 || alignedNormalCount == 2 && ModelQuadFacing.bitmapIsOpposingAligned(alignedFacingBitmap)) {
               if (onlyIntervalSide) {
                  return buildSNRLeafNodeFromPoints(workspace, points);
               }

               return buildSNRLeafNodeFromQuads(workspace, indexes, points);
            }
         }

         Arrays.sort(points.elements(), 0, points.size());
         partitions.clear();
         float distancex = Float.NaN;
         IntArrayList quadsBefore = null;
         IntArrayList quadsOn = null;
         int thickness = 0;
         LongListIterator var29 = points.iterator();

         while (var29.hasNext()) {
            long point = (Long)var29.next();
            switch (decodeType(point)) {
               case 0:
                  thickness--;
                  if (quadsOn == null) {
                     distancex = decodeDistance(point);
                  }
                  break;
               case 1:
                  int pointQuadIndex = decodeQuadIndex(point);
                  if (thickness == 0) {
                     float pointDistance = decodeDistance(point);
                     if (quadsOn == null) {
                        quadsOn = new IntArrayList();
                        distancex = pointDistance;
                     } else if (distancex != pointDistance) {
                        partitions.add(new Partition(distancex, quadsBefore, quadsOn));
                        distancex = pointDistance;
                        quadsBefore = null;
                        quadsOn = new IntArrayList();
                     }

                     quadsOn.add(pointQuadIndex);
                  } else {
                     if (quadsBefore == null) {
                        throw new IllegalStateException("there must be started intervals here");
                     }

                     quadsBefore.add(pointQuadIndex);
                     if (quadsOn == null) {
                        distancex = decodeDistance(point);
                     }
                  }
                  break;
               case 2:
                  if (thickness == 0 && (quadsBefore != null || quadsOn != null)) {
                     partitions.add(new Partition(distancex, quadsBefore, quadsOn));
                     distancex = Float.NaN;
                     quadsBefore = null;
                     quadsOn = null;
                  }

                  thickness++;
                  if (quadsOn != null) {
                     if (Float.isNaN(distancex)) {
                        throw new IllegalStateException("distance not set");
                     }

                     partitions.add(new Partition(distancex, quadsBefore, quadsOn));
                     distancex = Float.NaN;
                     quadsOn = null;
                  }

                  if (quadsBefore == null) {
                     quadsBefore = new IntArrayList();
                  }

                  quadsBefore.add(decodeQuadIndex(point));
            }
         }

         if (quadsBefore == null || quadsBefore.size() != indexes.size()) {
            boolean endsWithPlane = quadsOn != null;
            if (quadsBefore != null || quadsOn != null) {
               partitions.add(new Partition(endsWithPlane ? distancex : Float.NaN, quadsBefore, quadsOn));
            }

            if (partitions.size() <= 2) {
               Partition inside = (Partition)partitions.get(0);
               Partition outside = partitions.size() == 2 ? (Partition)partitions.get(1) : null;
               if (outside == null || !endsWithPlane) {
                  return InnerBinaryPartitionBSPNode.buildFromPartitions(workspace, indexes, depth, oldNode, inside, outside, axis);
               }
            }

            return InnerMultiPartitionBSPNode.buildFromPartitions(workspace, indexes, depth, oldNode, partitions, axis, endsWithPlane);
         }
      }

      BSPNode intersectingHandling = handleIntersecting(workspace, indexes, depth, oldNode);
      if (intersectingHandling != null) {
         return intersectingHandling;
      } else {
         BSPNode multiLeafNode = buildTopoMultiLeafNode(workspace, indexes);
         if (multiLeafNode == null) {
            throw new BSPBuildFailureException("No partition found but not intersecting and can't be statically topo sorted");
         } else {
            return multiLeafNode;
         }
      }
   }

   private static BSPNode handleIntersecting(BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode) {
      Int2IntOpenHashMap intersectionCounts = null;
      IntOpenHashSet primaryIntersectorIndexes = null;
      int primaryIntersectorThreshold = MathHelper.clamp(indexes.size() / 2, 2, 4);
      int i = -1;
      int j = 0;
      int quadCount = indexes.size();
      int stepSize = Math.max(1, quadCount * (quadCount - 1) / 2 / 500);
      int variance = 0;
      Random random = null;
      if (stepSize > 1) {
         int half = stepSize / 2;
         stepSize = Math.max(1, stepSize - half);
         variance = stepSize;
         random = new Random();
      }

      while (true) {
         i += stepSize;
         if (variance > 0) {
            i += random.nextInt(variance);
         }

         while (i >= j) {
            i -= j;
            j++;
         }

         if (j >= indexes.size()) {
            if (primaryIntersectorIndexes != null) {
               IntArrayList nonPrimaryIntersectors = new IntArrayList(indexes.size() - primaryIntersectorIndexes.size());
               IntArrayList primaryIntersectorQuadIndexes = new IntArrayList(primaryIntersectorIndexes.size());

               for (int k = 0; k < indexes.size(); k++) {
                  if (primaryIntersectorIndexes.contains(k)) {
                     primaryIntersectorQuadIndexes.add(indexes.getInt(k));
                  } else {
                     nonPrimaryIntersectors.add(indexes.getInt(k));
                  }
               }

               return InnerFixedDoubleBSPNode.buildFromParts(workspace, indexes, depth, oldNode, nonPrimaryIntersectors, primaryIntersectorQuadIndexes);
            }

            return null;
         }

         TQuad quadA = workspace.quads[indexes.getInt(i)];
         TQuad quadB = workspace.quads[indexes.getInt(j)];
         if (TQuad.extentsIntersect(quadA, quadB)) {
            if (intersectionCounts == null) {
               intersectionCounts = new Int2IntOpenHashMap();
            }

            int aCount = intersectionCounts.get(i) + 1;
            intersectionCounts.put(i, aCount);
            int bCount = intersectionCounts.get(j) + 1;
            intersectionCounts.put(j, bCount);
            if (aCount >= primaryIntersectorThreshold) {
               if (primaryIntersectorIndexes == null) {
                  primaryIntersectorIndexes = new IntOpenHashSet(2);
               }

               primaryIntersectorIndexes.add(i);
            }

            if (bCount >= primaryIntersectorThreshold) {
               if (primaryIntersectorIndexes == null) {
                  primaryIntersectorIndexes = new IntOpenHashSet(2);
               }

               primaryIntersectorIndexes.add(j);
            }

            if (primaryIntersectorIndexes != null && primaryIntersectorIndexes.size() == indexes.size()) {
               return new LeafMultiBSPNode(BSPSortState.compressIndexes(indexes));
            }
         }
      }
   }

   private static BSPNode buildTopoMultiLeafNode(BSPWorkspace workspace, IntArrayList indexes) {
      int quadCount = indexes.size();
      if (quadCount > TranslucentGeometryCollector.STATIC_TOPO_UNKNOWN_FALLBACK_LIMIT) {
         return null;
      } else {
         TQuad[] quads = new TQuad[quadCount];
         int[] activeToRealIndex = new int[quadCount];

         for (int i = 0; i < indexes.size(); i++) {
            int quadIndex = indexes.getInt(i);
            quads[i] = workspace.quads[quadIndex];
            activeToRealIndex[i] = quadIndex;
         }

         InnerPartitionBSPNode.QuadIndexConsumerIntoArray indexWriter = new InnerPartitionBSPNode.QuadIndexConsumerIntoArray(quadCount);
         return !TopoGraphSorting.topoGraphSort(indexWriter, quads, quads.length, activeToRealIndex, null, null)
            ? null
            : new LeafMultiBSPNode(BSPSortState.compressIndexesInPlace(indexWriter.indexes, false));
      }
   }

   private static BSPNode buildSNRLeafNodeFromQuads(BSPWorkspace workspace, IntArrayList indexes, LongArrayList points) {
      int[] quadIndexes;
      if (RadixSort.useRadixSort(indexes.size())) {
         int[] keys = new int[indexes.size()];

         for (int i = 0; i < indexes.size(); i++) {
            int quadIndex = indexes.getInt(i);
            keys[i] = MathUtil.floatToComparableInt(workspace.quads[quadIndex].getAccurateDotProduct());
         }

         quadIndexes = RadixSort.sort(keys);

         for (int i = 0; i < indexes.size(); i++) {
            quadIndexes[i] = indexes.getInt(quadIndexes[i]);
         }
      } else {
         long[] sortData = points.elements();

         for (int i = 0; i < indexes.size(); i++) {
            int quadIndex = indexes.getInt(i);
            int dotProductComponent = MathUtil.floatToComparableInt(workspace.quads[quadIndex].getAccurateDotProduct());
            sortData[i] = (long)dotProductComponent << 32 | quadIndex;
         }

         Arrays.sort(sortData, 0, indexes.size());
         quadIndexes = new int[indexes.size()];

         for (int i = 0; i < indexes.size(); i++) {
            quadIndexes[i] = (int)sortData[i];
         }
      }

      return new LeafMultiBSPNode(BSPSortState.compressIndexes(IntArrayList.wrap(quadIndexes), false));
   }

   private static BSPNode buildSNRLeafNodeFromPoints(BSPWorkspace workspace, LongArrayList points) {
      Arrays.sort(points.elements(), 0, points.size());
      int[] quadIndexes = new int[points.size()];
      int forwards = 0;
      int backwards = quadIndexes.length - 1;

      for (int i = 0; i < points.size(); i++) {
         int quadIndex = decodeQuadIndex(points.getLong(i));
         if (workspace.quads[quadIndex].getFacing().getSign() == 1) {
            quadIndexes[forwards++] = quadIndex;
         } else {
            quadIndexes[backwards--] = quadIndex;
         }
      }

      return new LeafMultiBSPNode(BSPSortState.compressIndexes(IntArrayList.wrap(quadIndexes), false));
   }

   private static class IndexRemapper implements IntConsumer {
      private final int[] indexMap;
      private final IntArrayList newIndexes;
      private int index = 0;
      private int firstOffset = 0;
      private static final int OFFSET_CHANGED = Integer.MIN_VALUE;

      IndexRemapper(int length, IntArrayList newIndexes) {
         this.indexMap = new int[length];
         this.newIndexes = newIndexes;
      }

      public void accept(int oldIndex) {
         int newIndex = this.newIndexes.getInt(this.index);
         this.indexMap[oldIndex] = newIndex;
         int newOffset = newIndex - oldIndex;
         if (this.index == 0) {
            this.firstOffset = newOffset;
         } else if (this.firstOffset != newOffset) {
            this.firstOffset = Integer.MIN_VALUE;
         }

         this.index++;
      }

      boolean hasFixedOffset() {
         return this.firstOffset != Integer.MIN_VALUE;
      }
   }

   record NodeReuseData(float[][] quadExtents, int[] indexes, int indexCount, int maxIndex) {
   }

   private static class QuadIndexConsumerIntoArray implements IntConsumer {
      final int[] indexes;
      private int index = 0;

      QuadIndexConsumerIntoArray(int size) {
         this.indexes = new int[size];
      }

      public void accept(int value) {
         this.indexes[this.index++] = value;
      }
   }
}
