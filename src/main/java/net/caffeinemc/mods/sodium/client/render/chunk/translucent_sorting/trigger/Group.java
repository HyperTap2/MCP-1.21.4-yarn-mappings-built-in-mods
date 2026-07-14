package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.util.interval_tree.DoubleInterval;

class Group {
   long sectionPos;
   float[] facePlaneDistances;
   long relDistanceHash;
   DoubleInterval distances;
   double baseDistance;
   AlignableNormal normal;

   Group(NormalPlanes normalPlanes) {
      this.replaceWith(normalPlanes);
   }

   void replaceWith(NormalPlanes normalPlanes) {
      this.sectionPos = normalPlanes.sectionPos.asLong();
      this.distances = normalPlanes.distanceRange;
      this.relDistanceHash = normalPlanes.relDistanceHash;
      this.facePlaneDistances = normalPlanes.relativeDistances;
      this.baseDistance = normalPlanes.baseDistance;
      this.normal = normalPlanes.normal;
   }

   private boolean planeTriggered(double start, double end) {
      return start < this.distances.getEnd()
         && end > this.distances.getStart()
         && AlignableNormal.queryRange(this.facePlaneDistances, (float)(start - this.baseDistance), (float)(end - this.baseDistance));
   }

   void triggerRange(SortTriggering ts, double start, double end) {
      if (this.planeTriggered(start, end)) {
         ts.triggerSectionGFNI(this.sectionPos, this.normal);
      }
   }

   boolean normalPlanesEquals(NormalPlanes normalPlanes) {
      return this.facePlaneDistances.length == normalPlanes.relativeDistancesSet.size()
         && this.distances.equals(normalPlanes.distanceRange)
         && this.relDistanceHash == normalPlanes.relDistanceHash;
   }
}
