package net.caffeinemc.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Arrays;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.gl.arena.GlBufferArena;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.StagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.commons.lang3.Validate;

public class RenderRegion {
   public static final int REGION_WIDTH = 8;
   public static final int REGION_HEIGHT = 4;
   public static final int REGION_LENGTH = 8;
   public static final int REGION_WIDTH_M = 7;
   public static final int REGION_HEIGHT_M = 3;
   public static final int REGION_LENGTH_M = 7;
   public static final int REGION_WIDTH_SH = Integer.bitCount(7);
   public static final int REGION_HEIGHT_SH = Integer.bitCount(3);
   public static final int REGION_LENGTH_SH = Integer.bitCount(7);
   public static final int REGION_SIZE = 256;
   private final StagingBuffer stagingBuffer;
   private final int x;
   private final int y;
   private final int z;
   private final ChunkRenderList renderList;
   private final RenderSection[] sections = new RenderSection[256];
   private int sectionCount;
   private final Map<TerrainRenderPass, SectionRenderDataStorage> sectionRenderData = new Reference2ReferenceOpenHashMap();
   private RenderRegion.DeviceResources resources;

   public RenderRegion(int x, int y, int z, StagingBuffer stagingBuffer) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.stagingBuffer = stagingBuffer;
      this.renderList = new ChunkRenderList(this);
   }

   public static long key(int x, int y, int z) {
      return ChunkSectionPos.asLong(x, y, z);
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public int getChunkX() {
      return this.x << REGION_WIDTH_SH;
   }

   public int getChunkY() {
      return this.y << REGION_HEIGHT_SH;
   }

   public int getChunkZ() {
      return this.z << REGION_LENGTH_SH;
   }

   public int getOriginX() {
      return this.getChunkX() << 4;
   }

   public int getOriginY() {
      return this.getChunkY() << 4;
   }

   public int getOriginZ() {
      return this.getChunkZ() << 4;
   }

   public void delete(CommandList commandList) {
      for (SectionRenderDataStorage storage : this.sectionRenderData.values()) {
         storage.delete();
      }

      this.sectionRenderData.clear();
      if (this.resources != null) {
         this.resources.delete(commandList);
         this.resources = null;
      }

      Arrays.fill(this.sections, null);
   }

   public boolean isEmpty() {
      return this.sectionCount == 0;
   }

   public SectionRenderDataStorage getStorage(TerrainRenderPass pass) {
      return this.sectionRenderData.get(pass);
   }

   public SectionRenderDataStorage createStorage(TerrainRenderPass pass) {
      SectionRenderDataStorage storage = this.sectionRenderData.get(pass);
      if (storage == null) {
         storage = new SectionRenderDataStorage(pass.isTranslucent());
         this.sectionRenderData.put(pass, storage);
      }

      return storage;
   }

   public void refreshTesselation(CommandList commandList) {
      if (this.resources != null) {
         this.resources.deleteTessellation(commandList);
         this.resources.deleteIndexedTessellation(commandList);
      }

      for (SectionRenderDataStorage storage : this.sectionRenderData.values()) {
         storage.onBufferResized();
      }
   }

   public void refreshIndexedTesselation(CommandList commandList) {
      if (this.resources != null) {
         this.resources.deleteIndexedTessellation(commandList);
      }

      this.sectionRenderData.get(DefaultTerrainRenderPasses.TRANSLUCENT).onIndexBufferResized();
   }

   public void addSection(RenderSection section) {
      int sectionIndex = section.getSectionIndex();
      RenderSection prev = this.sections[sectionIndex];
      if (prev != null) {
         throw new IllegalStateException("Section has already been added to the region");
      } else {
         this.sections[sectionIndex] = section;
         this.sectionCount++;
      }
   }

   public void removeSection(RenderSection section) {
      int sectionIndex = section.getSectionIndex();
      RenderSection prev = this.sections[sectionIndex];
      if (prev == null) {
         throw new IllegalStateException("Section was not loaded within the region");
      } else if (prev != section) {
         throw new IllegalStateException("Tried to remove the wrong section");
      } else {
         for (SectionRenderDataStorage storage : this.sectionRenderData.values()) {
            storage.removeData(sectionIndex);
         }

         this.sections[sectionIndex] = null;
         this.sectionCount--;
      }
   }

   public RenderSection getSection(int id) {
      return this.sections[id];
   }

   public RenderRegion.DeviceResources getResources() {
      return this.resources;
   }

   public RenderRegion.DeviceResources createResources(CommandList commandList) {
      if (this.resources == null) {
         this.resources = new RenderRegion.DeviceResources(commandList, this.stagingBuffer);
      }

      return this.resources;
   }

   public void update(CommandList commandList) {
      if (this.resources != null && this.resources.shouldDelete()) {
         this.resources.delete(commandList);
         this.resources = null;
      }
   }

   public ChunkRenderList getRenderList() {
      return this.renderList;
   }

   static {
      Validate.isTrue(MathUtil.isPowerOfTwo(8));
      Validate.isTrue(MathUtil.isPowerOfTwo(4));
      Validate.isTrue(MathUtil.isPowerOfTwo(8));
   }

   public static class DeviceResources {
      private final GlBufferArena geometryArena;
      private final GlBufferArena indexArena;
      private GlTessellation tessellation;
      private GlTessellation indexedTessellation;

      public DeviceResources(CommandList commandList, StagingBuffer stagingBuffer) {
         int stride = WorldRenderingSettings.INSTANCE.getVertexFormat().getVertexFormat().getStride();
         int initialVertices = 756;
         this.geometryArena = new GlBufferArena(commandList, 256 * initialVertices, stride, stagingBuffer);
         int initialIndices = initialVertices / 4 * 6;
         this.indexArena = new GlBufferArena(commandList, 256 * initialIndices, 4, stagingBuffer);
      }

      public void updateTessellation(CommandList commandList, GlTessellation tessellation) {
         if (this.tessellation != null) {
            this.tessellation.delete(commandList);
         }

         this.tessellation = tessellation;
      }

      public void updateIndexedTessellation(CommandList commandList, GlTessellation tessellation) {
         if (this.indexedTessellation != null) {
            this.indexedTessellation.delete(commandList);
         }

         this.indexedTessellation = tessellation;
      }

      public GlTessellation getTessellation() {
         return this.tessellation;
      }

      public GlTessellation getIndexedTessellation() {
         return this.indexedTessellation;
      }

      public void deleteTessellation(CommandList commandList) {
         if (this.tessellation != null) {
            this.tessellation.delete(commandList);
            this.tessellation = null;
         }
      }

      public void deleteIndexedTessellation(CommandList commandList) {
         if (this.indexedTessellation != null) {
            this.indexedTessellation.delete(commandList);
            this.indexedTessellation = null;
         }
      }

      public GlBuffer getGeometryBuffer() {
         return this.geometryArena.getBufferObject();
      }

      public GlBuffer getIndexBuffer() {
         return this.indexArena.getBufferObject();
      }

      public void delete(CommandList commandList) {
         this.deleteTessellation(commandList);
         this.deleteIndexedTessellation(commandList);
         this.geometryArena.delete(commandList);
         this.indexArena.delete(commandList);
      }

      public GlBufferArena getGeometryArena() {
         return this.geometryArena;
      }

      public GlBufferArena getIndexArena() {
         return this.indexArena;
      }

      public boolean shouldDelete() {
         return this.geometryArena.isEmpty() && this.indexArena.isEmpty();
      }
   }
}
