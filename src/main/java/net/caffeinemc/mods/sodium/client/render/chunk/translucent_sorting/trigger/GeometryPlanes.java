package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collection;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.Vector3fc;

public class GeometryPlanes {
   private NormalPlanes[] alignedPlanes;
   private Object2ReferenceOpenHashMap<Vector3fc, NormalPlanes> unalignedPlanes;

   public NormalPlanes[] getAligned() {
      return this.alignedPlanes;
   }

   public NormalPlanes[] getAlignedOrCreate() {
      if (this.alignedPlanes == null) {
         this.alignedPlanes = new NormalPlanes[ModelQuadFacing.DIRECTIONS];
      }

      return this.alignedPlanes;
   }

   public Collection<NormalPlanes> getUnaligned() {
      return this.unalignedPlanes == null ? null : this.unalignedPlanes.values();
   }

   public Object2ReferenceOpenHashMap<Vector3fc, NormalPlanes> getUnalignedOrCreate() {
      if (this.unalignedPlanes == null) {
         this.unalignedPlanes = new Object2ReferenceOpenHashMap();
      }

      return this.unalignedPlanes;
   }

   public Collection<Vector3fc> getUnalignedNormals() {
      return this.unalignedPlanes == null ? null : this.unalignedPlanes.keySet();
   }

   NormalPlanes getPlanesForNormal(NormalList normalList) {
      AlignableNormal normal = normalList.getNormal();
      if (normal.isAligned()) {
         return this.alignedPlanes == null ? null : this.alignedPlanes[normal.getAlignedDirection()];
      } else {
         return this.unalignedPlanes == null ? null : (NormalPlanes)this.unalignedPlanes.get(normal);
      }
   }

   public void addAlignedPlane(ChunkSectionPos sectionPos, int direction, float distance) {
      NormalPlanes[] alignedDistances = this.getAlignedOrCreate();
      NormalPlanes normalPlanes = alignedDistances[direction];
      if (normalPlanes == null) {
         normalPlanes = new NormalPlanes(sectionPos, direction);
         alignedDistances[direction] = normalPlanes;
      }

      normalPlanes.addPlaneMember(distance);
   }

   public void addDoubleSidedPlane(ChunkSectionPos sectionPos, int axis, float distance) {
      this.addAlignedPlane(sectionPos, axis, distance);
      this.addAlignedPlane(sectionPos, axis + 3, -distance);
   }

   public void addUnalignedPlane(ChunkSectionPos sectionPos, Vector3fc normal, float distance) {
      Object2ReferenceOpenHashMap<Vector3fc, NormalPlanes> unalignedDistances = this.getUnalignedOrCreate();
      NormalPlanes normalPlanes = (NormalPlanes)unalignedDistances.get(normal);
      if (normalPlanes == null) {
         normalPlanes = new NormalPlanes(sectionPos, normal);
         unalignedDistances.put(normal, normalPlanes);
      }

      normalPlanes.addPlaneMember(distance);
   }

   public void addQuadPlane(ChunkSectionPos sectionPos, TQuad quad) {
      ModelQuadFacing facing = quad.useQuantizedFacing();
      if (facing.isAligned()) {
         this.addAlignedPlane(sectionPos, facing.ordinal(), quad.getQuantizedDotProduct());
      } else {
         this.addUnalignedPlane(sectionPos, quad.getQuantizedNormal(), quad.getQuantizedDotProduct());
      }
   }

   private void prepareAndInsert(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
      if (this.alignedPlanes != null) {
         for (NormalPlanes normalPlanes : this.alignedPlanes) {
            if (normalPlanes != null) {
               normalPlanes.prepareAndInsert(distancesByNormal);
            }
         }
      }

      if (this.unalignedPlanes != null) {
         ObjectIterator var6 = this.unalignedPlanes.values().iterator();

         while (var6.hasNext()) {
            NormalPlanes normalPlanesx = (NormalPlanes)var6.next();
            normalPlanesx.prepareAndInsert(distancesByNormal);
         }
      }
   }

   public void prepareIntegration() {
      this.prepareAndInsert(null);
   }

   public Object2ReferenceOpenHashMap<Vector3fc, float[]> prepareAndGetDistances() {
      Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal = new Object2ReferenceOpenHashMap(10);
      this.prepareAndInsert(distancesByNormal);
      return distancesByNormal;
   }

   public static GeometryPlanes fromQuadLists(ChunkSectionPos sectionPos, TQuad[] quads) {
      GeometryPlanes geometryPlanes = new GeometryPlanes();

      for (TQuad quad : quads) {
         geometryPlanes.addQuadPlane(sectionPos, quad);
      }

      return geometryPlanes;
   }
}
