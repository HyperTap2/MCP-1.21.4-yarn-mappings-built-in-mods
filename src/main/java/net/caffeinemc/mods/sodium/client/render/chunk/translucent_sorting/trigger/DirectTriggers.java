package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectSortedMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

class DirectTriggers implements SortTriggering.SectionTriggers<DynamicTopoData> {
   private Double2ObjectRBTreeMap<DirectTriggers.DirectTriggerData> directTriggerSections = new Double2ObjectRBTreeMap();
   private double accumulatedDistance = 0.0;
   private static final double EARLY_TRIGGER_FACTOR = 0.9;
   private static final double TRIGGER_ANGLE = Math.toRadians(10.0);
   private static final double EARLY_TRIGGER_ANGLE_COS = Math.cos(TRIGGER_ANGLE * 0.9);
   private static final double SECTION_CENTER_DIST_SQUARED = 3.0 * Math.pow(8.0, 2.0) + 1.0;
   private static final double SECTION_CENTER_DIST = Math.sqrt(SECTION_CENTER_DIST_SQUARED);
   private static final double TRIGGER_DISTANCE = 1.0;
   private static final double EARLY_TRIGGER_DISTANCE_SQUARED = Math.pow(0.9, 2.0);

   int getDirectTriggerCount() {
      return this.directTriggerSections.size();
   }

   private static double angleCos(double ax, double ay, double az, double bx, double by, double bz) {
      double lengthA = Math.sqrt(Math.fma(ax, ax, Math.fma(ay, ay, az * az)));
      double lengthB = Math.sqrt(Math.fma(bx, bx, Math.fma(by, by, bz * bz)));
      double dot = Math.fma(ax, bx, Math.fma(ay, by, az * bz));
      return dot / (lengthA * lengthB);
   }

   private void insertDirectAngleTrigger(DirectTriggers.DirectTriggerData data, Vector3dc cameraPos, double remainingAngle) {
      double triggerCameraSectionCenterDist = data.getSectionCenterTriggerCameraDist();
      double centerMinDistance = Math.tan(remainingAngle) * (triggerCameraSectionCenterDist - SECTION_CENTER_DIST);
      this.insertTrigger(this.accumulatedDistance + centerMinDistance, data);
   }

   private void insertDirectDistanceTrigger(DirectTriggers.DirectTriggerData data, Vector3dc cameraPos, double remainingDistance) {
      this.insertTrigger(this.accumulatedDistance + remainingDistance, data);
   }

   private void insertTrigger(double key, DirectTriggers.DirectTriggerData data) {
      data.dynamicData.setDirectTriggerKey(key);
      data.next = (DirectTriggers.DirectTriggerData)this.directTriggerSections.put(key, data);
   }

   @Override
   public void processTriggers(SortTriggering ts, CameraMovement movement) {
      Vector3dc lastCamera = movement.start();
      Vector3dc camera = movement.end();
      this.accumulatedDistance = this.accumulatedDistance + lastCamera.distance(camera);
      Double2ObjectSortedMap<DirectTriggers.DirectTriggerData> head = this.directTriggerSections.headMap(this.accumulatedDistance);
      ObjectBidirectionalIterator var6 = head.double2ObjectEntrySet().iterator();

      while (var6.hasNext()) {
         Entry<DirectTriggers.DirectTriggerData> entry = (Entry<DirectTriggers.DirectTriggerData>)var6.next();
         this.directTriggerSections.remove(entry.getDoubleKey());
         DirectTriggers.DirectTriggerData data = (DirectTriggers.DirectTriggerData)entry.getValue();

         while (data != null) {
            DirectTriggers.DirectTriggerData next = data.next;
            this.processSingleTrigger(data, ts, camera);
            data = next;
         }
      }
   }

   private void processSingleTrigger(DirectTriggers.DirectTriggerData data, SortTriggering ts, Vector3dc camera) {
      if (data.isAngleTriggering(camera)) {
         double remainingAngle = TRIGGER_ANGLE;
         double angleCos = data.centerRelativeAngleCos(data.triggerCameraPos, camera);
         if (angleCos <= EARLY_TRIGGER_ANGLE_COS) {
            ts.triggerSectionDirect(data.sectionPos);
            data.triggerCameraPos = camera;
         } else {
            remainingAngle -= Math.acos(angleCos);
         }

         this.insertDirectAngleTrigger(data, camera, remainingAngle);
      } else {
         double remainingDistance = 1.0;
         double lastTriggerCurrentCameraDistSquared = data.triggerCameraPos.distanceSquared(camera);
         if (lastTriggerCurrentCameraDistSquared >= EARLY_TRIGGER_DISTANCE_SQUARED) {
            ts.triggerSectionDirect(data.sectionPos);
            data.triggerCameraPos = camera;
         } else {
            remainingDistance -= Math.sqrt(lastTriggerCurrentCameraDistSquared);
         }

         this.insertDirectDistanceTrigger(data, camera, remainingDistance);
      }
   }

   @Override
   public void removeSection(long sectionPos, TranslucentData data) {
      if (data instanceof DynamicTopoData triggerable) {
         double key = triggerable.getDirectTriggerKey();
         if (key != -1.0) {
            this.directTriggerSections.remove(key);
            triggerable.setDirectTriggerKey(-1.0);
         }
      }
   }

   public void integrateSection(SortTriggering ts, ChunkSectionPos sectionPos, DynamicTopoData data, CameraMovement movement) {
      Vector3dc cameraPos = movement.start();
      DirectTriggers.DirectTriggerData newData = new DirectTriggers.DirectTriggerData(data, sectionPos, cameraPos);
      if (movement.hasChanged()) {
         this.processSingleTrigger(newData, ts, movement.end());
      } else if (newData.isAngleTriggering(cameraPos)) {
         this.insertDirectAngleTrigger(newData, cameraPos, TRIGGER_ANGLE);
      } else {
         this.insertDirectDistanceTrigger(newData, cameraPos, 1.0);
      }
   }

   private static class DirectTriggerData {
      final ChunkSectionPos sectionPos;
      private Vector3dc sectionCenter;
      final DynamicTopoData dynamicData;
      DirectTriggers.DirectTriggerData next;
      Vector3dc triggerCameraPos;

      DirectTriggerData(DynamicTopoData dynamicData, ChunkSectionPos sectionPos, Vector3dc triggerCameraPos) {
         this.dynamicData = dynamicData;
         this.sectionPos = sectionPos;
         this.triggerCameraPos = triggerCameraPos;
      }

      Vector3dc getSectionCenter() {
         if (this.sectionCenter == null) {
            this.sectionCenter = new Vector3d(this.sectionPos.getMinX() + 8, this.sectionPos.getMinY() + 8, this.sectionPos.getMinZ() + 8);
         }

         return this.sectionCenter;
      }

      double centerRelativeAngleCos(Vector3dc a, Vector3dc b) {
         Vector3dc sectionCenter = this.getSectionCenter();
         return DirectTriggers.angleCos(
            sectionCenter.x() - a.x(),
            sectionCenter.y() - a.y(),
            sectionCenter.z() - a.z(),
            sectionCenter.x() - b.x(),
            sectionCenter.y() - b.y(),
            sectionCenter.z() - b.z()
         );
      }

      double getSectionCenterTriggerCameraDist() {
         return Math.sqrt(this.getSectionCenterDistSquared(this.triggerCameraPos));
      }

      double getSectionCenterDistSquared(Vector3dc vector) {
         Vector3dc sectionCenter = this.getSectionCenter();
         return sectionCenter.distanceSquared(vector);
      }

      boolean isAngleTriggering(Vector3dc vector) {
         return this.getSectionCenterDistSquared(vector) > DirectTriggers.SECTION_CENTER_DIST_SQUARED;
      }
   }
}
