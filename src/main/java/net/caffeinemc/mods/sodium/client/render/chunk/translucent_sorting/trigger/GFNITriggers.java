package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3fc;

class GFNITriggers implements SortTriggering.SectionTriggers<DynamicData> {
   private Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap();

   int getUniqueNormalCount() {
      return this.normalLists.size();
   }

   @Override
   public void processTriggers(SortTriggering ts, CameraMovement movement) {
      ObjectIterator var3 = this.normalLists.values().iterator();

      while (var3.hasNext()) {
         NormalList normalList = (NormalList)var3.next();
         normalList.processMovement(ts, movement);
      }
   }

   private void addSectionInNewNormalLists(DynamicData dynamicData, NormalPlanes normalPlanes) {
      AlignableNormal normal = normalPlanes.normal;
      NormalList normalList = (NormalList)this.normalLists.get(normal);
      if (normalList == null) {
         normalList = new NormalList(normal);
         this.normalLists.put(normal, normalList);
         normalList.addSection(normalPlanes, normalPlanes.sectionPos.asLong());
      }
   }

   private boolean removeSectionFromList(NormalList normalList, long sectionPos) {
      normalList.removeSection(sectionPos);
      return normalList.isEmpty();
   }

   @Override
   public void removeSection(long sectionPos, TranslucentData data) {
      this.normalLists.values().removeIf(normalList -> this.removeSectionFromList(normalList, sectionPos));
   }

   @Override
   public void integrateSection(SortTriggering ts, ChunkSectionPos pos, DynamicData data, CameraMovement movement) {
      long sectionPos = pos.asLong();
      GeometryPlanes geometryPlanes = data.getGeometryPlanes();
      ObjectIterator<NormalList> iterator = this.normalLists.values().iterator();

      while (iterator.hasNext()) {
         NormalList normalList = (NormalList)iterator.next();
         NormalPlanes normalPlanes = geometryPlanes.getPlanesForNormal(normalList);
         if (normalList.hasSection(sectionPos)) {
            if (normalPlanes == null) {
               if (this.removeSectionFromList(normalList, sectionPos)) {
                  iterator.remove();
               }
            } else {
               normalList.updateSection(normalPlanes, sectionPos);
            }
         } else if (normalPlanes != null) {
            normalList.addSection(normalPlanes, sectionPos);
         }
      }

      NormalPlanes[] aligned = geometryPlanes.getAligned();
      if (aligned != null) {
         for (NormalPlanes normalPlane : aligned) {
            if (normalPlane != null) {
               this.addSectionInNewNormalLists(data, normalPlane);
            }
         }
      }

      Collection<NormalPlanes> unaligned = geometryPlanes.getUnaligned();
      if (unaligned != null) {
         for (NormalPlanes normalPlanex : unaligned) {
            this.addSectionInNewNormalLists(data, normalPlanex);
         }
      }

      data.discardGeometryPlanes();
      if (movement.hasChanged()) {
         ObjectIterator var18 = this.normalLists.values().iterator();

         while (var18.hasNext()) {
            NormalList normalList = (NormalList)var18.next();
            normalList.processCatchup(ts, movement, sectionPos);
         }
      }
   }
}
