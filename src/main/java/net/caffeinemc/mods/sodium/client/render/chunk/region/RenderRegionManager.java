package net.caffeinemc.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap.FastEntrySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.arena.GlBufferArena;
import net.caffeinemc.mods.sodium.client.gl.arena.PendingUpload;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.StagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.jetbrains.annotations.NotNull;

public class RenderRegionManager {
   private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap();
   private final StagingBuffer stagingBuffer;

   public RenderRegionManager(CommandList commandList) {
      this.stagingBuffer = createStagingBuffer(commandList);
   }

   public void update() {
      this.stagingBuffer.flip();

      try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
         Iterator<RenderRegion> it = this.regions.values().iterator();

         while (it.hasNext()) {
            RenderRegion region = it.next();
            region.update(commandList);
            if (region.isEmpty()) {
               region.delete(commandList);
               it.remove();
            }
         }
      }
   }

   public void uploadResults(CommandList commandList, Collection<BuilderTaskOutput> results) {
      ObjectIterator var3 = this.createMeshUploadQueues(results).iterator();

      while (var3.hasNext()) {
         Entry<RenderRegion, List<BuilderTaskOutput>> entry = (Entry<RenderRegion, List<BuilderTaskOutput>>)var3.next();
         this.uploadResults(commandList, (RenderRegion)entry.getKey(), (Collection<BuilderTaskOutput>)entry.getValue());
      }
   }

   private void uploadResults(CommandList commandList, RenderRegion region, Collection<BuilderTaskOutput> results) {
      ArrayList<RenderRegionManager.PendingSectionMeshUpload> uploads = new ArrayList<>();
      ArrayList<RenderRegionManager.PendingSectionIndexBufferUpload> indexUploads = new ArrayList<>();

      for (BuilderTaskOutput result : results) {
         int renderSectionIndex = result.render.getSectionIndex();
         if (result.render.isDisposed()) {
            throw new IllegalStateException("Render section is disposed");
         }

         if (result instanceof ChunkBuildOutput chunkBuildOutput) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
               SectionRenderDataStorage storage = region.getStorage(pass);
               if (storage != null) {
                  storage.removeVertexData(renderSectionIndex);
               }

               BuiltSectionMeshParts mesh = chunkBuildOutput.getMesh(pass);
               if (mesh != null) {
                  uploads.add(new RenderRegionManager.PendingSectionMeshUpload(result.render, mesh, pass, new PendingUpload(mesh.getVertexData())));
               }
            }
         }

         if (result instanceof ChunkSortOutput indexDataOutput && !indexDataOutput.isReusingUploadedIndexData()) {
            NativeBuffer buffer = indexDataOutput.getIndexBuffer();
            if (buffer != null) {
               indexUploads.add(new RenderRegionManager.PendingSectionIndexBufferUpload(result.render, new PendingUpload(buffer)));
               SectionRenderDataStorage storagex = region.getStorage(DefaultTerrainRenderPasses.TRANSLUCENT);
               if (storagex != null) {
                  storagex.removeIndexData(renderSectionIndex);
               }
            }
         }
      }

      Profiler profiler = Profilers.get();
      if (!uploads.isEmpty() || !indexUploads.isEmpty()) {
         RenderRegion.DeviceResources resources = region.createResources(commandList);
         profiler.push("upload_vertices");
         if (!uploads.isEmpty()) {
            GlBufferArena arena = resources.getGeometryArena();
            boolean bufferChanged = arena.upload(commandList, uploads.stream().map(upload -> upload.vertexUpload));
            if (bufferChanged) {
               region.refreshTesselation(commandList);
            }

            for (RenderRegionManager.PendingSectionMeshUpload upload : uploads) {
               SectionRenderDataStorage storagex = region.createStorage(upload.pass);
               storagex.setVertexData(upload.section.getSectionIndex(), upload.vertexUpload.getResult(), upload.meshData.getVertexCounts());
            }
         }

         profiler.swap("upload_indices");
         if (!indexUploads.isEmpty()) {
            GlBufferArena arena = resources.getIndexArena();
            boolean bufferChanged = arena.upload(commandList, indexUploads.stream().map(upload -> upload.indexBufferUpload));
            if (bufferChanged) {
               region.refreshIndexedTesselation(commandList);
            }

            for (RenderRegionManager.PendingSectionIndexBufferUpload upload : indexUploads) {
               SectionRenderDataStorage storagex = region.createStorage(DefaultTerrainRenderPasses.TRANSLUCENT);
               storagex.setIndexData(upload.section.getSectionIndex(), upload.indexBufferUpload.getResult());
            }
         }

         profiler.pop();
      }
   }

   private FastEntrySet<RenderRegion, List<BuilderTaskOutput>> createMeshUploadQueues(Collection<BuilderTaskOutput> results) {
      Reference2ReferenceOpenHashMap<RenderRegion, List<BuilderTaskOutput>> map = new Reference2ReferenceOpenHashMap();

      for (BuilderTaskOutput result : results) {
         List<BuilderTaskOutput> queue = (List<BuilderTaskOutput>)map.computeIfAbsent(result.render.getRegion(), k -> new ArrayList());
         queue.add(result);
      }

      return map.reference2ReferenceEntrySet();
   }

   public void delete(CommandList commandList) {
      ObjectIterator var2 = this.regions.values().iterator();

      while (var2.hasNext()) {
         RenderRegion region = (RenderRegion)var2.next();
         region.delete(commandList);
      }

      this.regions.clear();
      this.stagingBuffer.delete(commandList);
   }

   public Collection<RenderRegion> getLoadedRegions() {
      return this.regions.values();
   }

   public StagingBuffer getStagingBuffer() {
      return this.stagingBuffer;
   }

   public RenderRegion createForChunk(int chunkX, int chunkY, int chunkZ) {
      return this.create(chunkX >> RenderRegion.REGION_WIDTH_SH, chunkY >> RenderRegion.REGION_HEIGHT_SH, chunkZ >> RenderRegion.REGION_LENGTH_SH);
   }

   @NotNull
   private RenderRegion create(int x, int y, int z) {
      long key = RenderRegion.key(x, y, z);
      RenderRegion instance = (RenderRegion)this.regions.get(key);
      if (instance == null) {
         this.regions.put(key, instance = new RenderRegion(x, y, z, this.stagingBuffer));
      }

      return instance;
   }

   private static StagingBuffer createStagingBuffer(CommandList commandList) {
      return (StagingBuffer)(SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)
         ? new MappedStagingBuffer(commandList)
         : new FallbackStagingBuffer(commandList));
   }

   private record PendingSectionIndexBufferUpload(RenderSection section, PendingUpload indexBufferUpload) {
   }

   private record PendingSectionMeshUpload(RenderSection section, BuiltSectionMeshParts meshData, TerrainRenderPass pass, PendingUpload vertexUpload) {
   }
}
