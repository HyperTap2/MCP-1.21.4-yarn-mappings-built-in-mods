package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class DynamicTopoData extends DynamicData {
   private static final int MAX_TOPO_SORT_QUADS = 1000;
   private static final int MAX_TOPO_SORT_TIME_NS = 1000000;
   private static final int MAX_FAILING_TOPO_SORT_TIME_NS = 750000;
   private static final int MAX_TOPO_SORT_PATIENT_TIME_NS = 250000;
   private static final int PATIENT_TOPO_ATTEMPTS = 5;
   private static final int REGULAR_TOPO_ATTEMPTS = 2;
   private boolean GFNITrigger = true;
   private boolean directTrigger = false;
   private int consecutiveTopoSortFailures = 0;
   private double directTriggerKey = -1.0;
   private boolean pendingTriggerIsDirect;
   private final TQuad[] quads;
   private final Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal;

   private DynamicTopoData(
      ChunkSectionPos sectionPos,
      int vertexCount,
      TQuad[] quads,
      GeometryPlanes geometryPlanes,
      Vector3dc initialCameraPos,
      Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal
   ) {
      super(sectionPos, vertexCount, quads.length, geometryPlanes, initialCameraPos);
      this.quads = quads;
      this.distancesByNormal = distancesByNormal;
      if (this.getQuadCount() > 1000) {
         this.directTrigger = true;
         this.GFNITrigger = false;
      }
   }

   @Override
   public Sorter getSorter() {
      return new DynamicTopoData.DynamicTopoSorter(
         this.getQuadCount(), this, this.pendingTriggerIsDirect, this.consecutiveTopoSortFailures, this.GFNITrigger, this.directTrigger
      );
   }

   public boolean GFNITriggerEnabled() {
      return this.GFNITrigger;
   }

   public boolean directTriggerEnabled() {
      return this.directTrigger;
   }

   public double getDirectTriggerKey() {
      return this.directTriggerKey;
   }

   public void setDirectTriggerKey(double key) {
      this.directTriggerKey = key;
   }

   public boolean isMatchingSorter(DynamicTopoData.DynamicTopoSorter sorter) {
      return sorter.parent == this;
   }

   public boolean checkAndApplyGFNITriggerOff(DynamicTopoData.DynamicTopoSorter sorter) {
      if (this.GFNITrigger && !sorter.GFNITrigger) {
         this.GFNITrigger = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean checkAndApplyDirectTriggerOff(DynamicTopoData.DynamicTopoSorter sorter) {
      if (this.directTrigger && !sorter.directTrigger) {
         this.directTrigger = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean checkAndApplyDirectTriggerOn(DynamicTopoData.DynamicTopoSorter sorter) {
      if (!this.directTrigger && sorter.directTrigger) {
         this.directTrigger = true;
         return true;
      } else {
         return false;
      }
   }

   public void applyTopoSortFailureCounterChange(DynamicTopoData.DynamicTopoSorter sorter) {
      if (sorter.hasSortFailureReset()) {
         this.consecutiveTopoSortFailures = 0;
      } else if (sorter.hasSortFailureIncrement()) {
         this.consecutiveTopoSortFailures++;
      }
   }

   private void copyStateFrom(DynamicTopoData.DynamicTopoSorter sorter) {
      this.GFNITrigger = sorter.GFNITrigger;
      this.directTrigger = sorter.directTrigger;
      this.consecutiveTopoSortFailures = sorter.consecutiveTopoSortFailuresNew;
   }

   @Override
   public void prepareTrigger(boolean isDirectTrigger) {
      this.pendingTriggerIsDirect = isDirectTrigger;
   }

   static void distanceSortDirect(IntBuffer indexBuffer, TQuad[] quads, Vector3fc cameraPos) {
      if (quads.length <= 1) {
         TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
      } else if (RadixSort.useRadixSort(quads.length)) {
         int[] keys = new int[quads.length];

         for (int q = 0; q < quads.length; q++) {
            keys[q] = ~Float.floatToRawIntBits(quads[q].getCenter().distanceSquared(cameraPos));
         }

         int[] indices = RadixSort.sort(keys);

         for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
         }
      } else {
         long[] data = new long[quads.length];

         for (int q = 0; q < quads.length; q++) {
            float distance = quads[q].getCenter().distanceSquared(cameraPos);
            data[q] = (long)(~Float.floatToRawIntBits(distance)) << 32 | q;
         }

         Arrays.sort(data);

         for (int i = 0; i < quads.length; i++) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, (int)data[i]);
         }
      }
   }

   public static DynamicTopoData fromMesh(
      int vertexCount, CombinedCameraPos cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, GeometryPlanes geometryPlanes
   ) {
      Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal = geometryPlanes.prepareAndGetDistances();
      return new DynamicTopoData(sectionPos, vertexCount, quads, geometryPlanes, cameraPos.getAbsoluteCameraPos(), distancesByNormal);
   }

   public class DynamicTopoSorter extends DynamicSorter implements IntConsumer {
      private final DynamicTopoData parent;
      private final boolean isDirectTrigger;
      private final int consecutiveTopoSortFailures;
      private boolean directTrigger;
      private boolean GFNITrigger;
      private int consecutiveTopoSortFailuresNew;
      private IntBuffer intBuffer;

      private DynamicTopoSorter(
         int quadCount, DynamicTopoData parent, boolean isDirectTrigger, int consecutiveTopoSortFailures, boolean GFNITrigger, boolean directTrigger
      ) {
         super(quadCount);
         this.parent = parent;
         this.isDirectTrigger = isDirectTrigger;
         this.consecutiveTopoSortFailures = consecutiveTopoSortFailures;
         this.consecutiveTopoSortFailuresNew = consecutiveTopoSortFailures;
         this.GFNITrigger = GFNITrigger;
         this.directTrigger = directTrigger;
      }

      private static int getAttemptsForTime(long ns) {
         return ns <= 250000L ? 5 : 2;
      }

      private boolean hasSortFailureReset() {
         return this.consecutiveTopoSortFailuresNew < this.consecutiveTopoSortFailures;
      }

      private boolean hasSortFailureIncrement() {
         return this.consecutiveTopoSortFailuresNew > this.consecutiveTopoSortFailures;
      }

      @Override
      public void accept(int value) {
         TranslucentData.writeQuadVertexIndexes(this.intBuffer, value);
      }

      @Override
      void writeSort(CombinedCameraPos cameraPos, boolean initial) {
         IntBuffer indexBuffer = this.getIntBuffer();
         if (this.GFNITrigger && !this.isDirectTrigger) {
            this.intBuffer = indexBuffer;
            long sortStart = initial ? 0L : System.nanoTime();
            boolean result = TopoGraphSorting.topoGraphSort(
               this, DynamicTopoData.this.quads, DynamicTopoData.this.distancesByNormal, cameraPos.getRelativeCameraPos()
            );
            this.intBuffer = null;
            long sortTime = initial ? 0L : System.nanoTime() - sortStart;
            if (!initial && sortTime > (this.consecutiveTopoSortFailuresNew > 0 ? 750000 : 1000000)) {
               this.directTrigger = true;
               this.GFNITrigger = false;
            } else if (result) {
               this.directTrigger = false;
               this.consecutiveTopoSortFailuresNew = 0;
            } else {
               this.consecutiveTopoSortFailuresNew++;
               this.directTrigger = true;
               if (this.consecutiveTopoSortFailuresNew >= getAttemptsForTime(sortTime)) {
                  this.GFNITrigger = false;
               }
            }
         }

         if (this.directTrigger) {
            indexBuffer.rewind();
            DynamicTopoData.distanceSortDirect(indexBuffer, DynamicTopoData.this.quads, cameraPos.getRelativeCameraPos());
         }

         if (initial) {
            DynamicTopoData.this.copyStateFrom(this);
         }
      }
   }
}
