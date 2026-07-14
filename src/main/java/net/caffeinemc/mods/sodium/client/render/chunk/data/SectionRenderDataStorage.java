package net.caffeinemc.mods.sodium.client.render.chunk.data;

import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.gl.arena.GlBufferSegment;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SectionRenderDataStorage {
   @Nullable
   private final GlBufferSegment[] vertexAllocations = new GlBufferSegment[256];
   @Nullable
   private final GlBufferSegment[] elementAllocations;
   private final long pMeshDataArray;

   public SectionRenderDataStorage(boolean storesIndices) {
      if (storesIndices) {
         this.elementAllocations = new GlBufferSegment[256];
      } else {
         this.elementAllocations = null;
      }

      this.pMeshDataArray = SectionRenderDataUnsafe.allocateHeap(256);
   }

   public void setVertexData(int localSectionIndex, GlBufferSegment allocation, int[] vertexCounts) {
      GlBufferSegment prev = this.vertexAllocations[localSectionIndex];
      if (prev != null) {
         prev.delete();
      }

      this.vertexAllocations[localSectionIndex] = allocation;
      long pMeshData = this.getDataPointer(localSectionIndex);
      int sliceMask = 0;
      long vertexOffset = allocation.getOffset();

      for (int facingIndex = 0; facingIndex < ModelQuadFacing.COUNT; facingIndex++) {
         long vertexCount = vertexCounts[facingIndex];
         SectionRenderDataUnsafe.setVertexOffset(pMeshData, facingIndex, UInt32.downcast(vertexOffset));
         SectionRenderDataUnsafe.setElementCount(pMeshData, facingIndex, UInt32.downcast((vertexCount >> 2) * 6L));
         if (vertexCount > 0L) {
            sliceMask |= 1 << facingIndex;
         }

         vertexOffset += vertexCount;
      }

      SectionRenderDataUnsafe.setSliceMask(pMeshData, sliceMask);
   }

   public void setIndexData(int localSectionIndex, GlBufferSegment allocation) {
      if (this.elementAllocations == null) {
         throw new IllegalStateException("Cannot set index data when storesIndices is false");
      } else {
         GlBufferSegment prev = this.elementAllocations[localSectionIndex];
         if (prev != null) {
            prev.delete();
         }

         this.elementAllocations[localSectionIndex] = allocation;
         long pMeshData = this.getDataPointer(localSectionIndex);
         SectionRenderDataUnsafe.setBaseElement(pMeshData, allocation.getOffset());
      }
   }

   public void removeData(int localSectionIndex) {
      this.removeVertexData(localSectionIndex, false);
      if (this.elementAllocations != null) {
         this.removeIndexData(localSectionIndex);
      }
   }

   public void removeVertexData(int localSectionIndex) {
      this.removeVertexData(localSectionIndex, true);
   }

   private void removeVertexData(int localSectionIndex, boolean retainIndexData) {
      GlBufferSegment prev = this.vertexAllocations[localSectionIndex];
      if (prev != null) {
         prev.delete();
         this.vertexAllocations[localSectionIndex] = null;
         long pMeshData = this.getDataPointer(localSectionIndex);
         long baseElement = SectionRenderDataUnsafe.getBaseElement(pMeshData);
         SectionRenderDataUnsafe.clear(pMeshData);
         if (retainIndexData) {
            SectionRenderDataUnsafe.setBaseElement(pMeshData, baseElement);
         }
      }
   }

   public void removeIndexData(int localSectionIndex) {
      GlBufferSegment[] allocations = this.elementAllocations;
      if (allocations == null) {
         throw new IllegalStateException("Cannot remove index data when storesIndices is false");
      } else {
         GlBufferSegment prev = allocations[localSectionIndex];
         if (prev != null) {
            prev.delete();
         }

         allocations[localSectionIndex] = null;
      }
   }

   public void onBufferResized() {
      for (int sectionIndex = 0; sectionIndex < 256; sectionIndex++) {
         this.updateMeshes(sectionIndex);
      }
   }

   private void updateMeshes(int sectionIndex) {
      GlBufferSegment allocation = this.vertexAllocations[sectionIndex];
      if (allocation != null) {
         long offset = allocation.getOffset();
         long data = this.getDataPointer(sectionIndex);

         for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            SectionRenderDataUnsafe.setVertexOffset(data, facing, offset);
            long count = SectionRenderDataUnsafe.getElementCount(data, facing);
            offset += count / 6L * 4L;
         }
      }
   }

   public void onIndexBufferResized() {
      if (this.elementAllocations != null) {
         for (int sectionIndex = 0; sectionIndex < 256; sectionIndex++) {
            GlBufferSegment allocation = this.elementAllocations[sectionIndex];
            if (allocation != null) {
               SectionRenderDataUnsafe.setBaseElement(this.getDataPointer(sectionIndex), allocation.getOffset());
            }
         }
      }
   }

   public long getDataPointer(int sectionIndex) {
      return SectionRenderDataUnsafe.heapPointer(this.pMeshDataArray, sectionIndex);
   }

   public void delete() {
      deleteAllocations(this.vertexAllocations);
      if (this.elementAllocations != null) {
         deleteAllocations(this.elementAllocations);
      }

      SectionRenderDataUnsafe.freeHeap(this.pMeshDataArray);
   }

   private static void deleteAllocations(@NotNull GlBufferSegment[] allocations) {
      for (GlBufferSegment allocation : allocations) {
         if (allocation != null) {
            allocation.delete();
         }
      }

      Arrays.fill(allocations, null);
   }
}
