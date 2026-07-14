package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3dc;

public class SortTriggering {
   private BiConsumer<Long, Boolean> triggerSectionCallback;
   private DynamicData catchupData = null;
   private int gfniTriggerCount = 0;
   private int directTriggerCount = 0;
   private final ObjectOpenHashSet<AlignableNormal> triggeredNormals = new ObjectOpenHashSet();
   private int triggeredNormalCount = 0;
   private final int[] sortTypeCounters = new int[SortType.values().length];
   private final GFNITriggers gfni = new GFNITriggers();
   private final DirectTriggers direct = new DirectTriggers();

   public void triggerSections(BiConsumer<Long, Boolean> triggerSectionCallback, CameraMovement movement) {
      this.triggeredNormals.clear();
      this.triggerSectionCallback = triggerSectionCallback;
      int oldGfniTriggerCount = this.gfniTriggerCount;
      int oldDirectTriggerCount = this.directTriggerCount;
      this.gfniTriggerCount = 0;
      this.directTriggerCount = 0;
      this.gfni.processTriggers(this, movement);
      this.direct.processTriggers(this, movement);
      if (this.gfniTriggerCount <= 0 && this.directTriggerCount <= 0) {
         this.gfniTriggerCount = oldGfniTriggerCount;
         this.directTriggerCount = oldDirectTriggerCount;
      } else {
         this.triggeredNormalCount = this.triggeredNormals.size();
      }

      this.triggerSectionCallback = null;
   }

   private boolean isCatchingUp() {
      return this.catchupData != null;
   }

   void triggerSectionGFNI(long sectionPos, AlignableNormal normal) {
      if (this.isCatchingUp()) {
         this.triggerSectionCatchup(sectionPos, false);
      } else {
         this.triggeredNormals.add(normal);
         this.triggerSectionCallback.accept(sectionPos, false);
         this.gfniTriggerCount++;
      }
   }

   void triggerSectionDirect(ChunkSectionPos sectionPos) {
      if (this.isCatchingUp()) {
         this.triggerSectionCatchup(sectionPos.asLong(), true);
      } else {
         this.triggerSectionCallback.accept(sectionPos.asLong(), true);
         this.directTriggerCount++;
      }
   }

   private void triggerSectionCatchup(long sectionPos, boolean isDirectTrigger) {
      if (this.triggerSectionCallback != null) {
         this.catchupData.prepareTrigger(isDirectTrigger);
         this.triggerSectionCallback.accept(sectionPos, isDirectTrigger);
      }
   }

   public void applyTriggerChanges(DynamicTopoData data, DynamicTopoData.DynamicTopoSorter topoSorter, ChunkSectionPos pos, Vector3dc cameraPos) {
      if (data.isMatchingSorter(topoSorter)) {
         if (data.checkAndApplyGFNITriggerOff(topoSorter)) {
            this.gfni.removeSection(pos.asLong(), data);
         }

         if (data.checkAndApplyDirectTriggerOn(topoSorter)) {
            this.direct.integrateSection(this, pos, data, new CameraMovement(cameraPos, cameraPos));
         }

         if (data.checkAndApplyDirectTriggerOff(topoSorter)) {
            this.direct.removeSection(pos.asLong(), data);
         }

         data.applyTopoSortFailureCounterChange(topoSorter);
      }
   }

   private void decrementSortTypeCounter(TranslucentData oldData) {
      if (oldData != null) {
         this.sortTypeCounters[oldData.getSortType().ordinal()]--;
      }
   }

   private void incrementSortTypeCounter(TranslucentData newData) {
      this.sortTypeCounters[newData.getSortType().ordinal()]++;
   }

   public void removeSection(TranslucentData oldData, long sectionPos) {
      if (oldData != null) {
         this.gfni.removeSection(sectionPos, oldData);
         this.direct.removeSection(sectionPos, oldData);
         this.decrementSortTypeCounter(oldData);
      }
   }

   public void integrateTranslucentData(TranslucentData oldData, TranslucentData newData, Vector3dc cameraPos, BiConsumer<Long, Boolean> triggerSectionCallback) {
      if (oldData != newData) {
         ChunkSectionPos pos = newData.sectionPos;
         this.incrementSortTypeCounter(newData);
         if (newData instanceof DynamicData dynamicData) {
            this.direct.removeSection(pos.asLong(), oldData);
            this.decrementSortTypeCounter(oldData);
            this.triggerSectionCallback = triggerSectionCallback;
            this.catchupData = dynamicData;
            CameraMovement movement = new CameraMovement(dynamicData.getInitialCameraPos(), cameraPos);
            if (dynamicData instanceof DynamicTopoData topoSortData) {
               if (topoSortData.GFNITriggerEnabled()) {
                  this.gfni.integrateSection(this, pos, topoSortData, movement);
               } else {
                  topoSortData.discardGeometryPlanes();
               }

               if (topoSortData.directTriggerEnabled()) {
                  this.direct.integrateSection(this, pos, topoSortData, movement);
               }
            } else {
               this.gfni.integrateSection(this, pos, dynamicData, movement);
            }

            this.triggerSectionCallback = null;
            this.catchupData = null;
         } else {
            this.removeSection(oldData, pos.asLong());
         }
      }
   }

   public void addDebugStrings(List<String> list) {
      SortBehavior sortBehavior = SodiumClientMod.options().debug.getSortBehavior();
      if (sortBehavior.getSortMode() == SortBehavior.SortMode.NONE) {
         list.add("TS OFF");
      } else {
         list.add(
            "TS (%s) NL=%02d TrN=%02d TrS=G%03d/D%03d"
               .formatted(
                  sortBehavior.getShortName(), this.gfni.getUniqueNormalCount(), this.triggeredNormalCount, this.gfniTriggerCount, this.directTriggerCount
               )
         );
         list.add(
            "N=%05d SNR=%05d STA=%05d DYN=%05d (DIR=%02d)"
               .formatted(
                  this.sortTypeCounters[SortType.NONE.ordinal()],
                  this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()],
                  this.sortTypeCounters[SortType.STATIC_TOPO.ordinal()],
                  this.sortTypeCounters[SortType.DYNAMIC.ordinal()],
                  this.direct.getDirectTriggerCount()
               )
         );
      }
   }

   interface SectionTriggers<T extends DynamicData> {
      void processTriggers(SortTriggering var1, CameraMovement var2);

      void removeSection(long var1, TranslucentData var3);

      void integrateSection(SortTriggering var1, ChunkSectionPos var2, T var3, CameraMovement var4);
   }
}
