package net.caffeinemc.mods.sodium.client.render.chunk.data;

import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.lwjgl.system.MemoryUtil;

public class SectionRenderDataUnsafe {
   private static final long OFFSET_BASE_ELEMENT = 0L;
   private static final long OFFSET_SLICE_MASK = 4L;
   private static final long OFFSET_SLICE_RANGES = 8L;
   private static final long ALIGNMENT = 64L;
   private static final long STRIDE = 64L;

   public static long allocateHeap(int count) {
      long bytes = 64L * count;
      long ptr = MemoryUtil.nmemAlignedAlloc(64L, bytes);
      MemoryUtil.memSet(ptr, 0, bytes);
      return ptr;
   }

   public static void freeHeap(long pointer) {
      MemoryUtil.nmemAlignedFree(pointer);
   }

   public static void clear(long pointer) {
      MemoryUtil.memSet(pointer, 0, 64L);
   }

   public static long heapPointer(long ptr, int index) {
      return ptr + index * 64L;
   }

   public static void setSliceMask(long ptr, int value) {
      MemoryUtil.memPutInt(ptr + 4L, value);
   }

   public static int getSliceMask(long ptr) {
      return MemoryUtil.memGetInt(ptr + 4L);
   }

   public static void setBaseElement(long ptr, long value) {
      MemoryUtil.memPutInt(ptr + 0L, UInt32.downcast(value));
   }

   public static long getBaseElement(long ptr) {
      return Integer.toUnsignedLong(MemoryUtil.memGetInt(ptr + 0L));
   }

   public static void setVertexOffset(long ptr, int facing, long value) {
      MemoryUtil.memPutInt(ptr + 8L + facing * 8L + 0L, UInt32.downcast(value));
   }

   public static long getVertexOffset(long ptr, int facing) {
      return UInt32.upcast(MemoryUtil.memGetInt(ptr + 8L + facing * 8L + 0L));
   }

   public static void setElementCount(long ptr, int facing, long value) {
      MemoryUtil.memPutInt(ptr + 8L + facing * 8L + 4L, UInt32.downcast(value));
   }

   public static long getElementCount(long ptr, int facing) {
      return UInt32.upcast(MemoryUtil.memGetInt(ptr + 8L + facing * 8L + 4L));
   }
}
