package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class ChunkVertexConsumer implements VertexConsumer, BlockSensitiveBufferBuilder {
   private static final int ATTRIBUTE_POSITION_BIT = 1;
   private static final int ATTRIBUTE_COLOR_BIT = 2;
   private static final int ATTRIBUTE_TEXTURE_BIT = 4;
   private static final int ATTRIBUTE_LIGHT_BIT = 8;
   private static final int ATTRIBUTE_NORMAL_BIT = 16;
   private static final int REQUIRED_ATTRIBUTES = 31;
   private final ChunkModelBuilder modelBuilder;
   private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
   private Material material;
   private int vertexIndex;
   private int writtenAttributes;
   private TranslucentGeometryCollector collector;

   public ChunkVertexConsumer(ChunkModelBuilder modelBuilder) {
      this.modelBuilder = modelBuilder;
   }

   public void setData(Material material, TranslucentGeometryCollector collector) {
      this.material = material;
      this.collector = collector;
   }

   @NotNull
   @Override
   public VertexConsumer vertex(float x, float y, float z) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.x = x;
      vertex.y = y;
      vertex.z = z;
      vertex.ao = 1.0F;
      this.writtenAttributes |= 1;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer color(int red, int green, int blue, int alpha) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.color = ColorABGR.pack(red, green, blue, alpha);
      this.writtenAttributes |= 2;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer color(float red, float green, float blue, float alpha) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.color = ColorABGR.pack(red, green, blue, alpha);
      this.writtenAttributes |= 2;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer color(int argb) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.color = ColorARGB.toABGR(argb);
      this.writtenAttributes |= 2;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer texture(float u, float v) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.u = u;
      vertex.v = v;
      this.writtenAttributes |= 4;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer overlay(int u, int v) {
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer overlay(int uv) {
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer light(int u, int v) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.light = (v & 65535) << 16 | u & 65535;
      this.writtenAttributes |= 8;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer light(int uv) {
      ChunkVertexEncoder.Vertex vertex = this.vertices[this.vertexIndex];
      vertex.light = uv;
      this.writtenAttributes |= 8;
      return this.potentiallyEndVertex();
   }

   @NotNull
   @Override
   public VertexConsumer normal(float x, float y, float z) {
      this.writtenAttributes |= 16;
      return this.potentiallyEndVertex();
   }

   public VertexConsumer potentiallyEndVertex() {
      if (this.writtenAttributes != 31) {
         return this;
      } else {
         this.vertexIndex++;
         this.writtenAttributes = 0;
         if (this.vertexIndex == 4) {
            int normal = this.calculateNormal();
            ModelQuadFacing cullFace = ModelQuadFacing.fromPackedNormal(normal);
            if (this.material.isTranslucent() && this.collector != null) {
               this.collector.appendQuad(normal, this.vertices, cullFace);
            }

            this.modelBuilder.getVertexBuffer(cullFace).push(this.vertices, this.material);
            this.vertexIndex = 0;
         }

         return this;
      }
   }

   private int calculateNormal() {
      float x0 = this.vertices[0].x;
      float y0 = this.vertices[0].y;
      float z0 = this.vertices[0].z;
      float x1 = this.vertices[1].x;
      float y1 = this.vertices[1].y;
      float z1 = this.vertices[1].z;
      float x2 = this.vertices[2].x;
      float y2 = this.vertices[2].y;
      float z2 = this.vertices[2].z;
      float x3 = this.vertices[3].x;
      float y3 = this.vertices[3].y;
      float z3 = this.vertices[3].z;
      float dx0 = x2 - x0;
      float dy0 = y2 - y0;
      float dz0 = z2 - z0;
      float dx1 = x3 - x1;
      float dy1 = y3 - y1;
      float dz1 = z3 - z1;
      float normX = dy0 * dz1 - dz0 * dy1;
      float normY = dz0 * dx1 - dx0 * dz1;
      float normZ = dx0 * dy1 - dy0 * dx1;
      float length = (float)Math.sqrt(normX * normX + normY * normY + normZ * normZ);
      if (length != 0.0 && length != 1.0) {
         normX /= length;
         normY /= length;
         normZ /= length;
      }

      return NormI8.pack(normX, normY, normZ);
   }

   @Override
   public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
      ((BlockSensitiveBufferBuilder)this.modelBuilder).beginBlock(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
   }

   @Override
   public void overrideBlock(int block) {
      ((BlockSensitiveBufferBuilder)this.modelBuilder).overrideBlock(block);
   }

   @Override
   public void restoreBlock() {
      ((BlockSensitiveBufferBuilder)this.modelBuilder).restoreBlock();
   }

   @Override
   public void endBlock() {
      ((BlockSensitiveBufferBuilder)this.modelBuilder).endBlock();
   }

   @Override
   public void ignoreMidBlock(boolean ignore) {
      ((BlockSensitiveBufferBuilder)this.modelBuilder).ignoreMidBlock(ignore);
   }
}
