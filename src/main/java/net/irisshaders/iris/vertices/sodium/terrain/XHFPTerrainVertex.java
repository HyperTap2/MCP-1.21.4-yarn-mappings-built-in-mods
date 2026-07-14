package net.irisshaders.iris.vertices.sodium.terrain;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder.Vertex;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class XHFPTerrainVertex implements ChunkVertexEncoder, VertexEncoderInterface {
   private static final int POSITION_MAX_VALUE = 1048576;
   private static final int TEXTURE_MAX_VALUE = 32768;
   private static final float MODEL_ORIGIN = 8.0F;
   private static final float MODEL_RANGE = 32.0F;
   private static final int DEFAULT_NORMAL;
   private final Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
   private final Vector4f tangent = new Vector4f(0.0F, 1.0F, 0.0F, 1.0F);
   private final int blockIdOffset;
   private final int normalOffset;
   private final int midBlockOffset;
   private final int midUvOffset;
   private final int stride;
   private final Vector2f normEncoded = new Vector2f();
   private final Vector2f tangEncoded = new Vector2f();
   private BlockContextHolder contextHolder;

   public XHFPTerrainVertex(int blockIdOffset, int normalOffset, int midUvOffset, int midBlockOffset, int stride) {
      this.blockIdOffset = blockIdOffset;
      this.normalOffset = normalOffset;
      this.midUvOffset = midUvOffset;
      this.midBlockOffset = midBlockOffset;
      this.stride = stride;
   }

   private static int packPositionHi(int x, int y, int z) {
      return (x >>> 10 & 1023) << 0 | (y >>> 10 & 1023) << 10 | (z >>> 10 & 1023) << 20;
   }

   private static int packPositionLo(int x, int y, int z) {
      return (x & 1023) << 0 | (y & 1023) << 10 | (z & 1023) << 20;
   }

   private static int quantizePosition(float position) {
      return (int)(normalizePosition(position) * 1048576.0F) & 1048575;
   }

   private static float normalizePosition(float v) {
      return (8.0F + v) / 32.0F;
   }

   private static int packTexture(int u, int v) {
      return (u & 65535) << 0 | (v & 65535) << 16;
   }

   private static int encodeTexture(float center, float x) {
      int bias = x < center ? 1 : -1;
      int quantized = Math.round(x * 32768.0F) + bias;
      return quantized & 32767 | sign(bias) << 15;
   }

   private static int encodeLight(int light) {
      int sky = MathHelper.clamp(light >>> 16 & 0xFF, 8, 248);
      int block = MathHelper.clamp(light >>> 0 & 0xFF, 8, 248);
      return block << 0 | sky << 8;
   }

   private static int sign(int x) {
      return x >>> 31;
   }

   private static int packLightAndData(int light, int material, int section) {
      return (light & 65535) << 0 | (material & 0xFF) << 16 | (section & 0xFF) << 24;
   }

   private static int floorInt(float x) {
      return (int)Math.floor(x);
   }

   @Override
   public void iris$setContextHolder(BlockContextHolder holder) {
      this.contextHolder = holder;
   }

   public long write(long ptr, int material, Vertex[] vertices, int section) {
      float texCentroidU = 0.0F;
      float texCentroidV = 0.0F;

      for (Vertex vertex : vertices) {
         texCentroidU += vertex.u;
         texCentroidV += vertex.v;
      }

      texCentroidU *= 0.25F;
      texCentroidV *= 0.25F;
      int midUV = XHFPModelVertexType.encodeOld(texCentroidU, texCentroidV);
      int finalNorm;
      if (this.normalOffset != 0) {
         NormalHelper.computeFaceNormalManual(
            this.normal,
            vertices[0].x,
            vertices[0].y,
            vertices[0].z,
            vertices[1].x,
            vertices[1].y,
            vertices[1].z,
            vertices[2].x,
            vertices[2].y,
            vertices[2].z,
            vertices[3].x,
            vertices[3].y,
            vertices[3].z
         );
         int tangent = this.computeTangentForQuad(this.normal, vertices);
         NormalHelper.octahedronEncode(this.normEncoded, this.normal.x, this.normal.y, this.normal.z);
         NormalHelper.tangentEncode(this.tangEncoded, this.tangent);
         finalNorm = NormI8.pack(this.normEncoded.x, this.normEncoded.y, this.tangEncoded.x, this.tangEncoded.y);
      } else {
         finalNorm = DEFAULT_NORMAL;
      }

      for (int i = 0; i < 4; i++) {
         Vertex vertex = vertices[i];
         int x = quantizePosition(vertex.x);
         int y = quantizePosition(vertex.y);
         int z = quantizePosition(vertex.z);
         int u = encodeTexture(texCentroidU, vertex.u);
         int v = encodeTexture(texCentroidV, vertex.v);
         int light = encodeLight(vertex.light);
         MemoryUtil.memPutInt(ptr, packPositionHi(x, y, z));
         MemoryUtil.memPutInt(ptr + 4L, packPositionLo(x, y, z));
         MemoryUtil.memPutInt(
            ptr + 8L,
            WorldRenderingSettings.INSTANCE.shouldUseSeparateAo() ? ColorABGR.withAlpha(vertex.color, vertex.ao) : ColorARGB.mulRGB(vertex.color, vertex.ao)
         );
         MemoryUtil.memPutInt(ptr + 12L, packTexture(u, v));
         MemoryUtil.memPutInt(ptr + 16L, packLightAndData(light, material, section));
         if (this.blockIdOffset != 0) {
            MemoryUtil.memPutInt(ptr + this.blockIdOffset, this.packBlockId(this.contextHolder));
         }

         if (this.midBlockOffset != 0) {
            MemoryUtil.memPutInt(
               ptr + this.midBlockOffset,
               this.contextHolder.ignoreMidBlock()
                  ? 0
                  : ExtendedDataHelper.computeMidBlock(
                     vertex.x, vertex.y, vertex.z, this.contextHolder.getLocalPosX(), this.contextHolder.getLocalPosY(), this.contextHolder.getLocalPosZ()
                  )
            );
            MemoryUtil.memPutByte(ptr + this.midBlockOffset + 3L, this.contextHolder.getBlockEmission());
         }

         if (this.midUvOffset != 0) {
            MemoryUtil.memPutInt(ptr + this.midUvOffset, midUV);
         }

         if (this.normalOffset != 0) {
            MemoryUtil.memPutInt(ptr + this.normalOffset, finalNorm);
         }

         ptr += this.stride;
      }

      return ptr;
   }

   private int computeTangentForQuad(Vector3f normal, Vertex[] vertices) {
      int tangent = NormalHelper.computeTangent(
         this.tangent,
         normal.x,
         normal.y,
         normal.z,
         vertices[0].x,
         vertices[0].y,
         vertices[0].z,
         vertices[0].u,
         vertices[0].v,
         vertices[1].x,
         vertices[1].y,
         vertices[1].z,
         vertices[1].u,
         vertices[1].v,
         vertices[2].x,
         vertices[2].y,
         vertices[2].z,
         vertices[2].u,
         vertices[2].v
      );
      if (tangent == -1) {
         tangent = NormalHelper.computeTangent(
            this.tangent,
            normal.x,
            normal.y,
            normal.z,
            vertices[2].x,
            vertices[2].y,
            vertices[2].z,
            vertices[2].u,
            vertices[2].v,
            vertices[3].x,
            vertices[3].y,
            vertices[3].z,
            vertices[3].u,
            vertices[3].v,
            vertices[0].x,
            vertices[0].y,
            vertices[0].z,
            vertices[0].u,
            vertices[0].v
         );
      }

      return tangent;
   }

   private int packBlockId(BlockContextHolder contextHolder) {
      return contextHolder.getBlockId() + 1 << 1 | contextHolder.getRenderType() & 1;
   }

   static {
      Vector2f normE = new Vector2f();
      Vector2f tangE = new Vector2f();
      NormalHelper.octahedronEncode(normE, 0.0F, 1.0F, 0.0F);
      NormalHelper.tangentEncode(tangE, new Vector4f(0.0F, 1.0F, 0.0F, 1.0F));
      DEFAULT_NORMAL = NormI8.pack(normE.x, normE.y, tangE.x, tangE.y);
   }
}
