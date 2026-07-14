package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.util.BufferAllocator;
import org.jetbrains.annotations.Nullable;

public interface VertexConsumerProvider {
   static VertexConsumerProvider.Immediate immediate(BufferAllocator buffer) {
      return immediate(Object2ObjectSortedMaps.emptyMap(), buffer);
   }

   static VertexConsumerProvider.Immediate immediate(SequencedMap<RenderLayer, BufferAllocator> layerBuffers, BufferAllocator fallbackBuffer) {
      return new VertexConsumerProvider.Immediate(fallbackBuffer, layerBuffers);
   }

   VertexConsumer getBuffer(RenderLayer layer);

   class Immediate implements VertexConsumerProvider, MemoryTrackingBuffer {
      protected final BufferAllocator allocator;
      protected final SequencedMap<RenderLayer, BufferAllocator> layerBuffers;
      protected final Map<RenderLayer, BufferBuilder> pending = new HashMap<>();
      @Nullable
      protected RenderLayer currentLayer;

      protected Immediate(BufferAllocator allocator, SequencedMap<RenderLayer, BufferAllocator> sequencedMap) {
         this.allocator = allocator;
         this.layerBuffers = sequencedMap;
      }

      @Override
      public VertexConsumer getBuffer(RenderLayer renderLayer) {
         BufferBuilder bufferBuilder = this.pending.get(renderLayer);
         if (bufferBuilder != null && !renderLayer.areVerticesNotShared()) {
            this.draw(renderLayer, bufferBuilder);
            bufferBuilder = null;
         }

         if (bufferBuilder != null) {
            return bufferBuilder.initAcceleration(renderLayer);
         }

         BufferAllocator bufferAllocator = this.layerBuffers.get(renderLayer);
         if (bufferAllocator != null) {
            bufferBuilder = this.iris$createBufferBuilder(bufferAllocator, renderLayer);
         } else {
            if (this.currentLayer != null) {
               this.draw(this.currentLayer);
            }

            bufferBuilder = this.iris$createBufferBuilder(this.allocator, renderLayer);
            this.currentLayer = renderLayer;
         }

         this.pending.put(renderLayer, bufferBuilder);
         return bufferBuilder.initAcceleration(renderLayer);
      }

      private BufferBuilder iris$createBufferBuilder(BufferAllocator allocator, RenderLayer layer) {
         boolean previous = ImmediateState.skipExtension.get();
         ImmediateState.skipExtension.set(!ImmediateState.isRenderingLevel);

         try {
            return new BufferBuilder(allocator, layer.getDrawMode(), layer.getVertexFormat());
         } finally {
            ImmediateState.skipExtension.set(previous);
         }
      }

      public void drawCurrentLayer() {
         if (this.currentLayer != null) {
            this.draw(this.currentLayer);
            this.currentLayer = null;
         }
      }

      public void draw() {
         this.drawCurrentLayer();

         for (RenderLayer renderLayer : this.layerBuffers.keySet()) {
            this.draw(renderLayer);
         }
      }

      public void draw(RenderLayer layer) {
         BufferBuilder bufferBuilder = this.pending.remove(layer);
         if (bufferBuilder != null) {
            this.draw(layer, bufferBuilder);
         }
      }

      private void draw(RenderLayer layer, BufferBuilder builder) {
         BuiltBuffer builtBuffer = builder.endNullable();
         if (builtBuffer != null) {
            if (layer.isTranslucent()) {
               BufferAllocator bufferAllocator = this.layerBuffers.getOrDefault(layer, this.allocator);
               builtBuffer.sortQuads(bufferAllocator, RenderSystem.getProjectionType().getVertexSorter());
            }

            boolean previous = ImmediateState.renderWithExtendedVertexFormat;
            if (!ImmediateState.isRenderingLevel) {
               ImmediateState.renderWithExtendedVertexFormat = false;
            }

            try {
               layer.draw(builtBuffer);
            } finally {
               ImmediateState.renderWithExtendedVertexFormat = previous;
            }
         }

         if (layer.equals(this.currentLayer)) {
            this.currentLayer = null;
         }
      }

      @Override
      public long getAllocatedSize() {
         long allocatedSize = ((MemoryTrackingBuffer)this.allocator).getAllocatedSize();
         for (BufferAllocator buffer : this.layerBuffers.values()) {
            allocatedSize += ((MemoryTrackingBuffer)buffer).getAllocatedSize();
         }

         return allocatedSize;
      }

      @Override
      public long getUsedSize() {
         long usedSize = ((MemoryTrackingBuffer)this.allocator).getUsedSize();
         for (BufferAllocator buffer : this.layerBuffers.values()) {
            usedSize += ((MemoryTrackingBuffer)buffer).getUsedSize();
         }

         return usedSize;
      }

      @Override
      public void freeAndDeleteBuffer() {
         ((MemoryTrackingBuffer)this.allocator).freeAndDeleteBuffer();
         for (BufferAllocator buffer : this.layerBuffers.values()) {
            ((MemoryTrackingBuffer)buffer).freeAndDeleteBuffer();
         }
      }
   }
}
