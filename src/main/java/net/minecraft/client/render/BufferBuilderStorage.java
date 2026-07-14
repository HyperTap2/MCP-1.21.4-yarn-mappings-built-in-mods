package net.minecraft.client.render;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SequencedMap;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Util;

public class BufferBuilderStorage implements RenderBuffersExt, MemoryTrackingRenderBuffers, DrawCallTrackingRenderBuffers {
   private final BlockBufferAllocatorStorage blockBufferBuilders = new BlockBufferAllocatorStorage();
   private final BlockBufferBuilderPool blockBufferBuildersPool;
   private final VertexConsumerProvider.Immediate entityVertexConsumers;
   private final VertexConsumerProvider.Immediate effectVertexConsumers;
   private final OutlineVertexConsumerProvider outlineVertexConsumers;
   private FullyBufferedMultiBufferSource iris$buffered;
   private OutlineVertexConsumerProvider iris$outlineVertexConsumers;
   private int iris$begins;
   private int iris$maxBegins;

   public BufferBuilderStorage(int maxBlockBuildersPoolSize) {
      this.blockBufferBuildersPool = BlockBufferBuilderPool.allocate(maxBlockBuildersPoolSize);
      SequencedMap<RenderLayer, BufferAllocator> sequencedMap = (SequencedMap<RenderLayer, BufferAllocator>)Util.make(
         new Object2ObjectLinkedOpenHashMap(), map -> {
            map.put(TexturedRenderLayers.getEntitySolid(), this.blockBufferBuilders.get(RenderLayer.getSolid()));
            map.put(TexturedRenderLayers.getEntityCutout(), this.blockBufferBuilders.get(RenderLayer.getCutout()));
            map.put(TexturedRenderLayers.getBannerPatterns(), this.blockBufferBuilders.get(RenderLayer.getCutoutMipped()));
            map.put(TexturedRenderLayers.getItemEntityTranslucentCull(), this.blockBufferBuilders.get(RenderLayer.getTranslucent()));
            assignBufferBuilder(map, TexturedRenderLayers.getShieldPatterns());
            assignBufferBuilder(map, TexturedRenderLayers.getBeds());
            assignBufferBuilder(map, TexturedRenderLayers.getShulkerBoxes());
            assignBufferBuilder(map, TexturedRenderLayers.getSign());
            assignBufferBuilder(map, TexturedRenderLayers.getHangingSign());
            map.put(TexturedRenderLayers.getChest(), new BufferAllocator(786432));
            assignBufferBuilder(map, RenderLayer.getArmorEntityGlint());
            assignBufferBuilder(map, RenderLayer.getGlint());
            assignBufferBuilder(map, RenderLayer.getGlintTranslucent());
            assignBufferBuilder(map, RenderLayer.getEntityGlint());
            assignBufferBuilder(map, RenderLayer.getWaterMask());
         }
      );
      this.entityVertexConsumers = VertexConsumerProvider.immediate(sequencedMap, new BufferAllocator(786432));
      this.outlineVertexConsumers = new OutlineVertexConsumerProvider(this.entityVertexConsumers);
      SequencedMap<RenderLayer, BufferAllocator> sequencedMap2 = (SequencedMap<RenderLayer, BufferAllocator>)Util.make(
         new Object2ObjectLinkedOpenHashMap(),
         object2ObjectLinkedOpenHashMap -> ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS
            .forEach(renderLayer -> assignBufferBuilder(object2ObjectLinkedOpenHashMap, renderLayer))
      );
      this.effectVertexConsumers = VertexConsumerProvider.immediate(sequencedMap2, new BufferAllocator(0));
   }

   private static void assignBufferBuilder(Object2ObjectLinkedOpenHashMap<RenderLayer, BufferAllocator> builderStorage, RenderLayer layer) {
      builderStorage.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
   }

   public BlockBufferAllocatorStorage getBlockBufferBuilders() {
      return this.blockBufferBuilders;
   }

   public BlockBufferBuilderPool getBlockBufferBuildersPool() {
      return this.blockBufferBuildersPool;
   }

   public VertexConsumerProvider.Immediate getEntityVertexConsumers() {
      return this.iris$begins != 0 ? this.iris$buffered : this.entityVertexConsumers;
   }

   public VertexConsumerProvider.Immediate getEffectVertexConsumers() {
      return this.iris$begins != 0 ? this.iris$buffered.getUnflushableWrapper() : this.effectVertexConsumers;
   }

   public OutlineVertexConsumerProvider getOutlineVertexConsumers() {
      return this.iris$begins != 0 ? this.iris$outlineVertexConsumers : this.outlineVertexConsumers;
   }

   @Override
   public void beginLevelRendering() {
      if (this.iris$begins == 0) {
         if (this.iris$buffered == null) {
            this.iris$buffered = new FullyBufferedMultiBufferSource();
            this.iris$outlineVertexConsumers = new OutlineVertexConsumerProvider(this.iris$buffered);
         }

         this.iris$buffered.assertWrapStackEmpty();
      }

      this.iris$begins++;
      this.iris$maxBegins = Math.max(this.iris$begins, this.iris$maxBegins);
   }

   @Override
   public void endLevelRendering() {
      this.iris$begins--;
      if (this.iris$begins == 0) {
         this.iris$buffered.assertWrapStackEmpty();
      }
   }

   @Override
   public long getEntityBufferAllocatedSize() {
      return this.iris$buffered == null ? 0L : this.iris$buffered.getAllocatedSize();
   }

   @Override
   public long getMiscBufferAllocatedSize() {
      return ((MemoryTrackingBuffer)this.entityVertexConsumers).getAllocatedSize();
   }

   @Override
   public int getMaxBegins() {
      return this.iris$maxBegins;
   }

   @Override
   public void freeAndDeleteBuffers() {
      if (this.iris$buffered != null) {
         this.iris$buffered.freeAndDeleteBuffer();
      }

      this.blockBufferBuilders
         .iris$getAllocators()
         .values()
         .forEach(buffer -> ((MemoryTrackingBuffer)buffer).freeAndDeleteBuffer());
      this.entityVertexConsumers.layerBuffers
         .forEach((renderLayer, buffer) -> ((MemoryTrackingBuffer)buffer).freeAndDeleteBuffer());
      this.entityVertexConsumers.layerBuffers.clear();
      if (this.iris$outlineVertexConsumers != null) {
         ((MemoryTrackingBuffer)this.iris$outlineVertexConsumers.iris$getPlainDrawer()).freeAndDeleteBuffer();
      }
   }

   @Override
   public int getDrawCalls() {
      return this.iris$buffered == null ? 0 : this.iris$buffered.getDrawCalls();
   }

   @Override
   public int getRenderTypes() {
      return this.iris$buffered == null ? 0 : this.iris$buffered.getRenderTypes();
   }

   @Override
   public void resetDrawCounts() {
      if (this.iris$buffered != null) {
         this.iris$buffered.resetDrawCalls();
      }
   }
}
