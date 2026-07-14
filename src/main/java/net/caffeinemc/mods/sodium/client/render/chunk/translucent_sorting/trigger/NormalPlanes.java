package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.floats.FloatIterator;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.util.interval_tree.DoubleInterval;
import net.caffeinemc.mods.sodium.client.util.interval_tree.Interval;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3fc;

public class NormalPlanes {
   final FloatOpenHashSet relativeDistancesSet = new FloatOpenHashSet(16);
   final AlignableNormal normal;
   final ChunkSectionPos sectionPos;
   float[] relativeDistances;
   DoubleInterval distanceRange;
   long relDistanceHash;
   double baseDistance;

   private NormalPlanes(ChunkSectionPos sectionPos, AlignableNormal normal) {
      this.sectionPos = sectionPos;
      this.normal = normal;
   }

   public NormalPlanes(ChunkSectionPos sectionPos, Vector3fc normal) {
      this(sectionPos, AlignableNormal.fromUnaligned(normal));
   }

   public NormalPlanes(ChunkSectionPos sectionPos, int alignedDirection) {
      this(sectionPos, AlignableNormal.fromAligned(alignedDirection));
   }

   boolean addPlaneMember(float vertexX, float vertexY, float vertexZ) {
      return this.addPlaneMember(this.normal.dot(vertexX, vertexY, vertexZ));
   }

   public boolean addPlaneMember(float distance) {
      return this.relativeDistancesSet.add(distance);
   }

   public void prepareIntegration() {
      if (this.relativeDistances != null) {
         throw new IllegalStateException("Already prepared");
      } else {
         int size = this.relativeDistancesSet.size();
         this.relativeDistances = new float[this.relativeDistancesSet.size()];
         int i = 0;
         FloatIterator var3 = this.relativeDistancesSet.iterator();

         while (var3.hasNext()) {
            float relDistance = (Float)var3.next();
            this.relativeDistances[i++] = relDistance;
            long distanceBits = Double.doubleToLongBits(relDistance);
            this.relDistanceHash = this.relDistanceHash ^ this.relDistanceHash * 31L + distanceBits;
         }

         Arrays.sort(this.relativeDistances);
         this.baseDistance = this.normal.dot(this.sectionPos.getMinX(), this.sectionPos.getMinY(), this.sectionPos.getMinZ());
         this.distanceRange = new DoubleInterval(
            this.relativeDistances[0] + this.baseDistance, this.relativeDistances[size - 1] + this.baseDistance, Interval.Bounded.CLOSED
         );
      }
   }

   public void prepareAndInsert(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
      this.prepareIntegration();
      if (distancesByNormal != null) {
         distancesByNormal.put(this.normal, this.relativeDistances);
      }
   }
}
