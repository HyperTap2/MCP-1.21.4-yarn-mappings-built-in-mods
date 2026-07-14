package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.sodium.terrain.BlockContextHolder;
import net.irisshaders.iris.vertices.sodium.terrain.VertexEncoderInterface;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.NotNull;

public class BakedChunkModelBuilder implements ChunkModelBuilder, BlockSensitiveBufferBuilder {
   private final ChunkMeshBufferBuilder[] vertexBuffers;
   private final ChunkVertexConsumer fallbackVertexConsumer = new ChunkVertexConsumer(this);
   private BuiltSectionInfo.Builder renderData;
   private final BlockContextHolder iris$contextHolder = new BlockContextHolder();

   public BakedChunkModelBuilder(ChunkMeshBufferBuilder[] vertexBuffers) {
      this.vertexBuffers = vertexBuffers;
      for (ChunkMeshBufferBuilder vertexBuffer : vertexBuffers) {
         ((VertexEncoderInterface)vertexBuffer).iris$setContextHolder(this.iris$contextHolder);
      }
   }

   @Override
   public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
      this.iris$contextHolder.setBlockData(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
   }

   @Override
   public void overrideBlock(int block) {
      this.iris$contextHolder.overrideBlock(block);
   }

   @Override
   public void restoreBlock() {
      this.iris$contextHolder.restoreBlock();
   }

   @Override
   public void endBlock() {
      this.iris$contextHolder.setBlockData(0, (byte)0, (byte)0, 0, 0, 0);
   }

   @Override
   public void ignoreMidBlock(boolean ignore) {
      this.iris$contextHolder.setIgnoreMidBlock(ignore);
   }

   @Override
   public ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing) {
      return this.vertexBuffers[facing.ordinal()];
   }

   @Override
   public void addSprite(@NotNull Sprite sprite) {
      this.renderData.addSprite(sprite);
   }

   @Override
   public VertexConsumer asFallbackVertexConsumer(Material material, TranslucentGeometryCollector collector) {
      this.fallbackVertexConsumer.setData(material, collector);
      return this.fallbackVertexConsumer;
   }

   public void destroy() {
      for (ChunkMeshBufferBuilder builder : this.vertexBuffers) {
         builder.destroy();
      }
   }

   public void begin(BuiltSectionInfo.Builder renderData, int sectionIndex) {
      this.renderData = renderData;

      for (ChunkMeshBufferBuilder vertexBuffer : this.vertexBuffers) {
         vertexBuffer.start(sectionIndex);
      }
   }
}
