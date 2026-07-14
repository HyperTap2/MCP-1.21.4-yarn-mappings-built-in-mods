package net.irisshaders.batchedentityrendering.impl;

import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.irisshaders.batchedentityrendering.impl.ordering.GraphTranslucencyRenderOrderManager;
import net.irisshaders.batchedentityrendering.impl.ordering.RenderOrderManager;
import net.irisshaders.iris.layer.WrappingMultiBufferSource;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

public class FullyBufferedMultiBufferSource extends Immediate implements MemoryTrackingBuffer, Groupable, WrappingMultiBufferSource {
   private static final int NUM_BUFFERS = 32;
   private final RenderOrderManager renderOrderManager;
   private final SegmentedBufferBuilder[] builders;
   private final LinkedHashMap<RenderLayer, Integer> affinities;
   private final BufferSegmentRenderer segmentRenderer;
   private final FullyBufferedMultiBufferSource.UnflushableWrapper unflushableWrapper;
   private final List<Function<RenderLayer, RenderLayer>> wrappingFunctionStack;
   private final Map<RenderLayer, List<BufferSegment>> typeToSegment = new HashMap<>();
   private int drawCalls;
   private int renderTypes;
   private Function<RenderLayer, RenderLayer> wrappingFunction = null;
   private boolean isReady;
   private List<RenderLayer> renderOrder = new ArrayList<>();

   public FullyBufferedMultiBufferSource() {
      super(new BufferAllocator(0), Object2ObjectSortedMaps.emptyMap());
      this.renderOrderManager = new GraphTranslucencyRenderOrderManager();
      this.builders = new SegmentedBufferBuilder[32];

      for (int i = 0; i < this.builders.length; i++) {
         this.builders[i] = new SegmentedBufferBuilder(this);
      }

      this.affinities = new LinkedHashMap<>(32, 0.75F, true);
      this.drawCalls = 0;
      this.segmentRenderer = new BufferSegmentRenderer();
      this.unflushableWrapper = new FullyBufferedMultiBufferSource.UnflushableWrapper(this);
      this.wrappingFunctionStack = new ArrayList<>();
   }

   private static long toMib(long x) {
      return x / 1024L / 1024L;
   }

   public VertexConsumer getBuffer(RenderLayer renderType) {
      this.removeReady();
      if (this.wrappingFunction != null) {
         renderType = this.wrappingFunction.apply(renderType);
      }

      this.renderOrderManager.begin(renderType);
      Integer affinity = this.affinities.get(renderType);
      if (affinity == null) {
         if (this.affinities.size() < this.builders.length) {
            affinity = this.affinities.size();
         } else {
            Iterator<Entry<RenderLayer, Integer>> iterator = this.affinities.entrySet().iterator();
            Entry<RenderLayer, Integer> evicted = iterator.next();
            iterator.remove();
            this.affinities.remove(evicted.getKey());
            affinity = evicted.getValue();
         }

         this.affinities.put(renderType, affinity);
      }

      return this.builders[affinity].getBuffer(renderType).initAcceleration(renderType);
   }

   private void removeReady() {
      this.isReady = false;
      this.typeToSegment.clear();
      this.renderOrder.clear();
   }

   public void readyUp() {
      this.isReady = true;
      Profiler profiler = Profilers.get();
      profiler.push("iris collect");

      for (SegmentedBufferBuilder builder : this.builders) {
         for (BufferSegment segment : builder.getSegments()) {
            this.typeToSegment.computeIfAbsent(segment.type(), type -> new ArrayList<>()).add(segment);
         }
      }

      profiler.swap("resolve ordering");
      this.renderOrder = this.renderOrderManager.getRenderOrder();
      this.renderOrderManager.reset();
      this.affinities.clear();
      profiler.pop();
   }

   public void draw() {
      Profiler profiler = Profilers.get();
      if (!this.isReady) {
         this.readyUp();
      }

      profiler.push("iris draw buffers");

      for (RenderLayer type : this.renderOrder) {
         if (this.typeToSegment.containsKey(type)) {
            type.startDrawing();
            this.renderTypes++;

            for (BufferSegment segment : this.typeToSegment.getOrDefault(type, Collections.emptyList())) {
               this.segmentRenderer.drawInner(segment);
               this.drawCalls++;
            }

            type.endDrawing();
         }
      }

      int targetClearTime = this.getTargetClearTime();

      for (SegmentedBufferBuilder builder : this.builders) {
         builder.clearBuffers(targetClearTime);
      }

      profiler.swap("reset");
      this.removeReady();
      profiler.pop();
   }

   public void endBatchWithType(TransparencyType transparencyType) {
      Profiler profiler = Profilers.get();
      if (!this.isReady) {
         this.readyUp();
      }

      profiler.push("iris draw partial");
      List<RenderLayer> types = new ArrayList<>();

      for (RenderLayer type : this.renderOrder) {
         if (((BlendingStateHolder)type).getTransparencyType() == transparencyType) {
            types.add(type);
            type.startDrawing();
            this.renderTypes++;

            for (BufferSegment segment : this.typeToSegment.getOrDefault(type, Collections.emptyList())) {
               this.segmentRenderer.drawInner(segment);
               this.drawCalls++;
            }

            this.typeToSegment.remove(type);
            type.endDrawing();
         }
      }

      profiler.swap("reset type " + transparencyType);
      this.renderOrder.removeAll(types);
      profiler.pop();
   }

   private int getTargetClearTime() {
      long sizeInMiB = toMib(this.getAllocatedSize());
      if (sizeInMiB > 5000L) {
         return 1000;
      } else {
         return sizeInMiB > 1000L ? 5000 : 10000;
      }
   }

   public int getDrawCalls() {
      return this.drawCalls;
   }

   public int getRenderTypes() {
      return this.renderTypes;
   }

   public void resetDrawCalls() {
      this.drawCalls = 0;
      this.renderTypes = 0;
   }

   public void draw(RenderLayer layer) {
   }

   public Immediate getUnflushableWrapper() {
      return this.unflushableWrapper;
   }

   @Override
   public long getAllocatedSize() {
      long size = 0L;

      for (SegmentedBufferBuilder builder : this.builders) {
         size += builder.getAllocatedSize();
      }

      return size;
   }

   @Override
   public long getUsedSize() {
      long size = 0L;

      for (SegmentedBufferBuilder builder : this.builders) {
         size += builder.getUsedSize();
      }

      return size;
   }

   @Override
   public void freeAndDeleteBuffer() {
      for (SegmentedBufferBuilder builder : this.builders) {
         builder.freeAndDeleteBuffer();
      }
   }

   @Override
   public void startGroup() {
      this.renderOrderManager.startGroup();
   }

   @Override
   public boolean maybeStartGroup() {
      return this.renderOrderManager.maybeStartGroup();
   }

   @Override
   public void endGroup() {
      this.renderOrderManager.endGroup();
   }

   @Override
   public void pushWrappingFunction(Function<RenderLayer, RenderLayer> wrappingFunction) {
      if (this.wrappingFunction != null) {
         this.wrappingFunctionStack.add(this.wrappingFunction);
      }

      this.wrappingFunction = wrappingFunction;
   }

   @Override
   public void popWrappingFunction() {
      if (this.wrappingFunctionStack.isEmpty()) {
         this.wrappingFunction = null;
      } else {
         this.wrappingFunction = this.wrappingFunctionStack.removeLast();
      }
   }

   @Override
   public void assertWrapStackEmpty() {
      if (!this.wrappingFunctionStack.isEmpty() || this.wrappingFunction != null) {
         throw new IllegalStateException("Wrapping function stack not empty!");
      }
   }

   public void weAreOutOfMemory() {
      for (SegmentedBufferBuilder builder : this.builders) {
         builder.lastDitchAttempt();
      }
   }

   private static class UnflushableWrapper extends Immediate implements Groupable {
      private final FullyBufferedMultiBufferSource wrapped;

      UnflushableWrapper(FullyBufferedMultiBufferSource wrapped) {
         super(new BufferAllocator(0), Object2ObjectSortedMaps.emptyMap());
         this.wrapped = wrapped;
      }

      public VertexConsumer getBuffer(RenderLayer renderType) {
         return this.wrapped.getBuffer(renderType);
      }

      public void draw() {
      }

      public void draw(RenderLayer layer) {
      }

      @Override
      public void startGroup() {
         this.wrapped.startGroup();
      }

      @Override
      public boolean maybeStartGroup() {
         return this.wrapped.maybeStartGroup();
      }

      @Override
      public void endGroup() {
         this.wrapped.endGroup();
      }
   }
}
