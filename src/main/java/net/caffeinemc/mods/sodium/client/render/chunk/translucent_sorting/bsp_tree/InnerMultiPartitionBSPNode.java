package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.joml.Vector3fc;

class InnerMultiPartitionBSPNode extends InnerPartitionBSPNode {
   private final float[] planeDistances;
   private final BSPNode[] partitions;
   private final int[][] onPlaneQuads;

   InnerMultiPartitionBSPNode(InnerPartitionBSPNode.NodeReuseData reuseData, int axis, float[] planeDistances, BSPNode[] partitions, int[][] onPlaneQuads) {
      super(reuseData, axis);
      this.planeDistances = planeDistances;
      this.partitions = partitions;
      this.onPlaneQuads = onPlaneQuads;
   }

   @Override
   void addPartitionPlanes(BSPWorkspace workspace) {
      for (int i = 0; i < this.planeDistances.length; i++) {
         workspace.addAlignedPartitionPlane(this.axis, this.planeDistances[i]);
      }

      for (BSPNode partition : this.partitions) {
         if (partition instanceof InnerPartitionBSPNode inner) {
            inner.addPartitionPlanes(workspace);
         }
      }
   }

   private void collectPlaneQuads(BSPSortState sortState, int planeIndex) {
      if (this.onPlaneQuads[planeIndex] != null) {
         sortState.writeIndexes(this.onPlaneQuads[planeIndex]);
      }
   }

   private void collectPartitionQuads(BSPSortState sortState, int partitionIndex, Vector3fc cameraPos) {
      if (this.partitions[partitionIndex] != null) {
         this.partitions[partitionIndex].collectSortedQuads(sortState, cameraPos);
      }
   }

   @Override
   void collectSortedQuads(BSPSortState sortState, Vector3fc cameraPos) {
      sortState.startNode(this);
      float cameraDistance = this.planeNormal.dot(cameraPos);

      for (int i = 0; i < this.planeDistances.length; i++) {
         if (cameraDistance <= this.planeDistances[i]) {
            boolean isOnPlane = cameraDistance == this.planeDistances[i];
            if (isOnPlane) {
               this.collectPartitionQuads(sortState, i, cameraPos);
            }

            for (int j = this.planeDistances.length; j > i; j--) {
               this.collectPartitionQuads(sortState, j, cameraPos);
               this.collectPlaneQuads(sortState, j - 1);
            }

            if (!isOnPlane) {
               this.collectPartitionQuads(sortState, i, cameraPos);
            }

            return;
         }

         this.collectPartitionQuads(sortState, i, cameraPos);
         this.collectPlaneQuads(sortState, i);
      }

      this.collectPartitionQuads(sortState, this.planeDistances.length, cameraPos);
   }

   static BSPNode buildFromPartitions(
      BSPWorkspace workspace, IntArrayList indexes, int depth, BSPNode oldNode, ReferenceArrayList<Partition> partitions, int axis, boolean endsWithPlane
   ) {
      int planeCount = endsWithPlane ? partitions.size() : partitions.size() - 1;
      float[] planeDistances = new float[planeCount];
      BSPNode[] partitionNodes = new BSPNode[planeCount + 1];
      int[][] onPlaneQuads = new int[planeCount][];
      BSPNode[] oldPartitionNodes = null;
      float[] oldPlaneDistances = null;
      int oldChildIndex = 0;
      float oldPartitionDistance = 0.0F;
      if (oldNode instanceof InnerMultiPartitionBSPNode multiNode && multiNode.axis == axis && multiNode.partitions.length > 0) {
         oldPartitionNodes = multiNode.partitions;
         oldPlaneDistances = multiNode.planeDistances;
         oldPartitionDistance = multiNode.planeDistances[0];
      }

      int i = 0;

      for (int count = partitions.size(); i < count; i++) {
         Partition partition = (Partition)partitions.get(i);
         float partitionDistance = Float.NaN;
         if (endsWithPlane || i < count - 1) {
            partitionDistance = partition.distance();
            workspace.addAlignedPartitionPlane(axis, partitionDistance);
            if (Float.isNaN(partitionDistance)) {
               throw new IllegalStateException("partition distance not set");
            }

            planeDistances[i] = partitionDistance;
         }

         if (partition.quadsBefore() != null) {
            BSPNode oldChild = null;
            if (oldPartitionNodes != null) {
               while (oldChildIndex < oldPartitionNodes.length && oldPartitionDistance < partitionDistance) {
                  oldChildIndex++;
                  oldPartitionDistance = oldChildIndex < oldPlaneDistances.length ? oldPlaneDistances[oldChildIndex] : Float.NaN;
               }

               if (oldChildIndex < oldPartitionNodes.length
                  && (oldPartitionDistance == partitionDistance || Float.isNaN(partitionDistance) && Float.isNaN(oldPartitionDistance))) {
                  oldChild = oldPartitionNodes[oldChildIndex];
               }
            }

            partitionNodes[i] = BSPNode.build(workspace, partition.quadsBefore(), depth, oldChild);
         }

         if (partition.quadsOn() != null) {
            onPlaneQuads[i] = BSPSortState.compressIndexes(partition.quadsOn());
         }
      }

      return new InnerMultiPartitionBSPNode(prepareNodeReuse(workspace, indexes, depth), axis, planeDistances, partitionNodes, onPlaneQuads);
   }
}
