package net.irisshaders.iris.vertices;

import net.irisshaders.iris.vertices.views.QuadView;
import org.lwjgl.system.MemoryUtil;

public class BufferBuilderPolygonView implements QuadView {
   private long[] writeOffsets;
   private long pointer;

   public void setup(long pointer, long[] writeOffsets, int stride, int vertexAmount) {
      this.pointer = pointer;
      this.writeOffsets = writeOffsets;
   }

   @Override
   public float x(int index) {
      return MemoryUtil.memGetFloat(this.pointer + this.writeOffsets[index]);
   }

   @Override
   public float y(int index) {
      return MemoryUtil.memGetFloat(this.pointer + this.writeOffsets[index] + 4L);
   }

   @Override
   public float z(int index) {
      return MemoryUtil.memGetFloat(this.pointer + this.writeOffsets[index] + 8L);
   }

   @Override
   public float u(int index) {
      return MemoryUtil.memGetFloat(this.pointer + this.writeOffsets[index] + 16L);
   }

   @Override
   public float v(int index) {
      return MemoryUtil.memGetFloat(this.pointer + this.writeOffsets[index] + 20L);
   }
}
