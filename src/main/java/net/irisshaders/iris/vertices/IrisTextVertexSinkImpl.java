package net.irisshaders.iris.vertices;

import java.nio.ByteBuffer;
import java.util.function.IntFunction;
import net.irisshaders.iris.api.v0.IrisTextVertexSink;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.views.QuadView;
import net.minecraft.client.render.VertexFormat;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class IrisTextVertexSinkImpl implements IrisTextVertexSink {
   static final VertexFormat format = IrisVertexFormats.GLYPH;
   private static final int STRIDE = IrisVertexFormats.GLYPH.getVertexSizeByte();
   private static final int OFFSET_POSITION = 0;
   private static final int OFFSET_COLOR = 12;
   private static final int OFFSET_TEXTURE = 16;
   private static final int OFFSET_MID_TEXTURE = 38;
   private static final int OFFSET_LIGHT = 24;
   private static final int OFFSET_NORMAL = 28;
   private static final int OFFSET_TANGENT = 46;
   private final ByteBuffer buffer;
   private final IrisTextVertexSinkImpl.TextQuadView quad = new IrisTextVertexSinkImpl.TextQuadView();
   private final Vector3f saveNormal = new Vector3f();
   private int vertexCount;
   private long elementOffset;
   private float uSum;
   private float vSum;

   public IrisTextVertexSinkImpl(int maxQuadCount, IntFunction<ByteBuffer> buffer) {
      this.buffer = buffer.apply(format.getVertexSizeByte() * 4 * maxQuadCount);
      this.elementOffset = MemoryUtil.memAddress(this.buffer);
   }

   @Override
   public VertexFormat getUnderlyingVertexFormat() {
      return format;
   }

   @Override
   public ByteBuffer getUnderlyingByteBuffer() {
      return this.buffer;
   }

   @Override
   public void quad(float minX, float minY, float maxX, float maxY, float z, int color, float minU, float minV, float maxU, float maxV, int light) {
      this.vertex(minX, minY, z, color, minU, minV, light);
      this.vertex(minX, maxY, z, color, minU, maxV, light);
      this.vertex(maxX, maxY, z, color, maxU, maxV, light);
      this.vertex(maxX, minY, z, color, maxU, minV, light);
   }

   private void vertex(float x, float y, float z, int color, float u, float v, int light) {
      this.vertexCount++;
      this.uSum += u;
      this.vSum += v;
      long ptr = this.elementOffset;
      MemoryUtil.memPutFloat(ptr + 0L, x);
      MemoryUtil.memPutFloat(ptr + 0L + 4L, y);
      MemoryUtil.memPutFloat(ptr + 0L + 8L, z);
      MemoryUtil.memPutInt(ptr + 12L, color);
      MemoryUtil.memPutFloat(ptr + 16L, u);
      MemoryUtil.memPutFloat(ptr + 16L + 4L, v);
      MemoryUtil.memPutInt(ptr + 24L, light);
      MemoryUtil.memPutShort(ptr + 32L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
      MemoryUtil.memPutShort(ptr + 34L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
      MemoryUtil.memPutShort(ptr + 36L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
      if (this.vertexCount == 4) {
         this.vertexCount = 0;
         this.uSum *= 0.25F;
         this.vSum *= 0.25F;
         this.quad.setup(this.elementOffset, IrisVertexFormats.GLYPH.getVertexSizeByte());
         NormalHelper.computeFaceNormal(this.saveNormal, this.quad);
         float normalX = this.saveNormal.x;
         float normalY = this.saveNormal.y;
         float normalZ = this.saveNormal.z;
         int normal = NormI8.pack(normalX, normalY, normalZ, 0.0F);
         int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, this.quad);

         for (long vertex = 0L; vertex < 4L; vertex++) {
            MemoryUtil.memPutFloat(ptr + 38L - STRIDE * vertex, this.uSum);
            MemoryUtil.memPutFloat(ptr + 42L - STRIDE * vertex, this.vSum);
            MemoryUtil.memPutInt(ptr + 28L - STRIDE * vertex, normal);
            MemoryUtil.memPutInt(ptr + 46L - STRIDE * vertex, tangent);
         }

         this.uSum = 0.0F;
         this.vSum = 0.0F;
      }

      this.buffer.position(this.buffer.position() + STRIDE);
      this.elementOffset = this.elementOffset + STRIDE;
   }

   static class TextQuadView implements QuadView {
      long writePointer;
      int stride;

      public TextQuadView() {
      }

      public void setup(long writePointer, int stride) {
         this.writePointer = writePointer;
         this.stride = stride;
      }

      @Override
      public float x(int index) {
         return MemoryUtil.memGetFloat(this.writePointer - this.stride * (3L - index));
      }

      @Override
      public float y(int index) {
         return MemoryUtil.memGetFloat(this.writePointer + 4L - this.stride * (3L - index));
      }

      @Override
      public float z(int index) {
         return MemoryUtil.memGetFloat(this.writePointer + 8L - this.stride * (3L - index));
      }

      @Override
      public float u(int index) {
         return MemoryUtil.memGetFloat(this.writePointer + 16L - this.stride * (3L - index));
      }

      @Override
      public float v(int index) {
         return MemoryUtil.memGetFloat(this.writePointer + 20L - this.stride * (3L - index));
      }
   }
}
