package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.util.BufferAllocator;

public class ByteBufferBuilderHolder implements MemoryTrackingBuffer {
   private final BufferAllocator builder;
   private long lastUse = System.currentTimeMillis();

   public ByteBufferBuilderHolder(BufferAllocator builder) {
      this.builder = builder;
   }

   public BufferAllocator getBuffer() {
      return this.builder;
   }

   public boolean deleteOrClear(int clearTime) {
      if (System.currentTimeMillis() - this.lastUse > clearTime) {
         this.builder.close();
         return true;
      } else {
         this.builder.clear();
         return false;
      }
   }

   public boolean delete(int clearTime) {
      if (System.currentTimeMillis() - this.lastUse > clearTime) {
         this.builder.close();
         return true;
      } else {
         return false;
      }
   }

   public void forceDelete() {
      this.builder.close();
   }

   @Override
   public long getAllocatedSize() {
      return ((MemoryTrackingBuffer)this.builder).getAllocatedSize();
   }

   @Override
   public long getUsedSize() {
      return ((MemoryTrackingBuffer)this.builder).getUsedSize();
   }

   @Override
   public void freeAndDeleteBuffer() {
      this.builder.close();
   }

   public void wasUsed() {
      this.lastUse = System.currentTimeMillis();
   }
}
