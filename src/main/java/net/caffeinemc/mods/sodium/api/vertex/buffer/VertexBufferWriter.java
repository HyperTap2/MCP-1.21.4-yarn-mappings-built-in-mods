package net.caffeinemc.mods.sodium.api.vertex.buffer;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

public interface VertexBufferWriter {
   static VertexBufferWriter of(VertexConsumer consumer) {
      if (consumer instanceof VertexBufferWriter writer && writer.canUseIntrinsics()) {
         return writer;
      } else {
         throw createUnsupportedVertexConsumerThrowable(consumer);
      }
   }

   @Nullable
   static VertexBufferWriter tryOf(VertexConsumer consumer) {
      return consumer instanceof VertexBufferWriter writer && writer.canUseIntrinsics() ? writer : null;
   }

   private static RuntimeException createUnsupportedVertexConsumerThrowable(VertexConsumer consumer) {
      Class<? extends VertexConsumer> clazz = (Class<? extends VertexConsumer>)consumer.getClass();
      String name = clazz.getName();
      return new IllegalArgumentException(
         "The class %s does not implement interface VertexBufferWriter, which is required for compatibility with Sodium (see: https://github.com/CaffeineMC/sodium/issues/1620)"
            .formatted(name)
      );
   }

   void push(MemoryStack var1, long var2, int var4, VertexFormat var5);

   default boolean canUseIntrinsics() {
      return true;
   }

   static void copyInto(VertexBufferWriter writer, MemoryStack stack, long ptr, int count, VertexFormat format) {
      int length = count * format.getVertexSizeByte();
      long copy = stack.nmalloc(length);
      MemoryIntrinsics.copyMemory(ptr, copy, length);
      writer.push(stack, copy, count, format);
   }
}
