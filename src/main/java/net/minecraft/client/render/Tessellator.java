package net.minecraft.client.render;

import net.minecraft.client.util.BufferAllocator;
import org.jetbrains.annotations.Nullable;

public class Tessellator {
   private static final int field_46841 = 786432;
   private final BufferAllocator allocator;
   @Nullable
   private static Tessellator INSTANCE;

   public static void initialize() {
      if (INSTANCE != null) {
         throw new IllegalStateException("Tesselator has already been initialized");
      }

      INSTANCE = new Tessellator();
   }

   public static Tessellator getInstance() {
      if (INSTANCE == null) {
         throw new IllegalStateException("Tesselator has not been initialized");
      } else {
         return INSTANCE;
      }
   }

   public Tessellator(int bufferCapacity) {
      this.allocator = new BufferAllocator(bufferCapacity);
   }

   public Tessellator() {
      this(786432);
   }

   public BufferBuilder begin(VertexFormat.DrawMode drawMode, VertexFormat format) {
      return new BufferBuilder(this.allocator, drawMode, format);
   }

   public void clear() {
      this.allocator.clear();
   }
}
