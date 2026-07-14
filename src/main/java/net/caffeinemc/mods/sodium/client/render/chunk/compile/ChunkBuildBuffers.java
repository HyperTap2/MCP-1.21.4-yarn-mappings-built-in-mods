package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;

public class ChunkBuildBuffers implements BlockSensitiveBufferBuilder {
   private final Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders = new Reference2ReferenceOpenHashMap();
   private final ChunkVertexType vertexType;

   public ChunkBuildBuffers(ChunkVertexType vertexType) {
      this.vertexType = vertexType;

      for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
         ChunkMeshBufferBuilder[] vertexBuffers = new ChunkMeshBufferBuilder[ModelQuadFacing.COUNT];

         for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            vertexBuffers[facing] = new ChunkMeshBufferBuilder(this.vertexType, 131072);
         }

         this.builders.put(pass, new BakedChunkModelBuilder(vertexBuffers));
      }
   }

   public void init(BuiltSectionInfo.Builder renderData, int sectionIndex) {
      ObjectIterator var3 = this.builders.values().iterator();

      while (var3.hasNext()) {
         BakedChunkModelBuilder builder = (BakedChunkModelBuilder)var3.next();
         builder.begin(renderData, sectionIndex);
      }
   }

   public ChunkModelBuilder get(Material material) {
      return (ChunkModelBuilder)this.builders.get(material.pass);
   }

   public ChunkModelBuilder get(TerrainRenderPass pass) {
      return (ChunkModelBuilder)this.builders.get(pass);
   }

   public BuiltSectionMeshParts createMesh(TerrainRenderPass pass, boolean forceUnassigned) {
      BakedChunkModelBuilder builder = (BakedChunkModelBuilder)this.builders.get(pass);
      List<ByteBuffer> vertexBuffers = new ArrayList<>();
      int[] vertexCounts = new int[ModelQuadFacing.COUNT];
      int vertexSum = 0;

      for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
         int ordinal = facing.ordinal();
         ChunkMeshBufferBuilder buffer = builder.getVertexBuffer(facing);
         if (!buffer.isEmpty()) {
            vertexBuffers.add(buffer.slice());
            int bufferCount = buffer.count();
            if (!forceUnassigned) {
               vertexCounts[ordinal] = bufferCount;
            }

            vertexSum += bufferCount;
         }
      }

      if (vertexSum == 0) {
         return null;
      } else {
         if (forceUnassigned) {
            vertexCounts[ModelQuadFacing.UNASSIGNED.ordinal()] = vertexSum;
         }

         NativeBuffer mergedBuffer = new NativeBuffer(vertexSum * this.vertexType.getVertexFormat().getStride());
         ByteBuffer mergedBufferBuilder = mergedBuffer.getDirectBuffer();

         for (ByteBuffer buffer : vertexBuffers) {
            mergedBufferBuilder.put(buffer);
         }

         return new BuiltSectionMeshParts(mergedBuffer, vertexCounts);
      }
   }

   public void destroy() {
      ObjectIterator var1 = this.builders.values().iterator();

      while (var1.hasNext()) {
         BakedChunkModelBuilder builder = (BakedChunkModelBuilder)var1.next();
         builder.destroy();
      }
   }

   @Override
   public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
      for (BakedChunkModelBuilder builder : this.builders.values()) {
         builder.beginBlock(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
      }
   }

   @Override
   public void overrideBlock(int block) {
      for (BakedChunkModelBuilder builder : this.builders.values()) {
         builder.overrideBlock(block);
      }
   }

   @Override
   public void restoreBlock() {
      for (BakedChunkModelBuilder builder : this.builders.values()) {
         builder.restoreBlock();
      }
   }

   @Override
   public void endBlock() {
      for (BakedChunkModelBuilder builder : this.builders.values()) {
         builder.endBlock();
      }
   }

   @Override
   public void ignoreMidBlock(boolean ignore) {
      for (BakedChunkModelBuilder builder : this.builders.values()) {
         builder.ignoreMidBlock(ignore);
      }
   }
}
