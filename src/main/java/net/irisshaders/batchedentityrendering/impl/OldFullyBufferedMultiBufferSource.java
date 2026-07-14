package net.irisshaders.batchedentityrendering.impl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;

public class OldFullyBufferedMultiBufferSource extends Immediate {
   private final Map<RenderLayer, BufferBuilder> bufferBuilders = new HashMap<>();
   private final Object2IntMap<RenderLayer> unused = new Object2IntOpenHashMap();
   private final Set<BufferBuilder> activeBuffers = new HashSet<>();
   private final Set<RenderLayer> typesThisFrame;
   private final List<RenderLayer> typesInOrder;
   private boolean flushed = false;

   public OldFullyBufferedMultiBufferSource() {
      super(new BufferAllocator(0), Object2ObjectSortedMaps.emptyMap());
      this.typesThisFrame = new HashSet<>();
      this.typesInOrder = new ArrayList<>();
   }

   private TransparencyType getTransparencyType(RenderLayer type) {
      while (type instanceof WrappableRenderType) {
         type = ((WrappableRenderType)type).unwrap();
      }

      return type instanceof BlendingStateHolder ? ((BlendingStateHolder)type).getTransparencyType() : TransparencyType.GENERAL_TRANSPARENT;
   }

   public VertexConsumer getBuffer(RenderLayer renderType) {
      this.flushed = false;
      BufferBuilder buffer = this.bufferBuilders
         .computeIfAbsent(
            renderType, type -> new BufferBuilder(new BufferAllocator(type.getExpectedBufferSize()), renderType.getDrawMode(), renderType.getVertexFormat())
         );
      if (this.activeBuffers.add(buffer)) {
      }

      if (this.typesThisFrame.add(renderType)) {
         this.typesInOrder.add(renderType);
      }

      this.unused.removeInt(renderType);
      return buffer;
   }

   public void draw() {
      if (!this.flushed) {
         List<RenderLayer> removedTypes = new ArrayList<>();
         this.unused.forEach((unusedType, unusedCount) -> {
            if (unusedCount >= 10) {
               BufferBuilder buffer = this.bufferBuilders.remove(unusedType);
               removedTypes.add(unusedType);
               if (this.activeBuffers.contains(buffer)) {
                  throw new IllegalStateException("A buffer was simultaneously marked as inactive and as active, something is very wrong...");
               }
            }
         });

         for (RenderLayer removed : removedTypes) {
            this.unused.removeInt(removed);
         }

         this.typesInOrder.sort(Comparator.comparing(this::getTransparencyType));

         for (RenderLayer type : this.typesInOrder) {
            this.drawInternal(type);
         }

         this.typesInOrder.clear();
         this.typesThisFrame.clear();
         this.flushed = true;
      }
   }

   public void draw(RenderLayer layer) {
   }

   private void drawInternal(RenderLayer type) {
      BufferBuilder buffer = this.bufferBuilders.get(type);
      if (buffer != null) {
         if (this.activeBuffers.remove(buffer)) {
            type.draw(buffer.endNullable());
         } else {
            int unusedCount = this.unused.getOrDefault(type, 0);
            this.unused.put(type, ++unusedCount);
         }
      }
   }
}
