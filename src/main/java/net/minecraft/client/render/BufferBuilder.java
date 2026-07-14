package net.minecraft.client.render;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.stream.Collectors;
import com.github.argon4w.acceleratedrendering.core.CoreBuffers;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderProgramLoader;
import com.google.common.base.Suppliers;
import java.util.function.Function;
import java.util.function.Supplier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.BufferBuilderExtension;
import net.irisshaders.batchedentityrendering.impl.BufferBuilderExt;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.BufferBuilderPolygonView;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.MojangBufferAccessor;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryStack;

public class BufferBuilder implements VertexConsumer, BufferBuilderExtension, BlockSensitiveBufferBuilder, BufferBuilderExt {
   private static final long field_52068 = -1L;
   private static final long field_52069 = -1L;
   private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
   private final BufferAllocator allocator;
   private long vertexPointer = -1L;
   private int vertexCount;
   private final VertexFormat format;
   private final VertexFormat.DrawMode drawMode;
   private final boolean canSkipElementChecks;
   private final boolean hasOverlay;
   private final int vertexSizeByte;
   private final int requiredMask;
   private final int[] offsetsByElementId;
   private int currentMask;
   private boolean building = true;
   private final BufferBuilderPolygonView iris$polygon = new BufferBuilderPolygonView();
   private final Vector3f iris$normal = new Vector3f();
   private final long[] iris$vertexOffsets = new long[4];
   private boolean iris$skipEndVertexOnce;
   private boolean iris$extending;
   private boolean iris$injectNormalAndUv1;
   private int iris$vertexCount;
   private int iris$currentBlock = -1;
   private byte iris$currentRenderType = -1;
   private byte iris$currentBlockEmission = -1;
   private int iris$currentLocalPosX;
   private int iris$currentLocalPosY;
   private int iris$currentLocalPosZ;
   private int iris$oldBlock = -1;
   private boolean iris$ignoreMidBlock;
   private boolean batchedentityrendering$duplicateNextVertex;
   private boolean batchedentityrendering$duplicateNextVertexAfter;
   private RenderLayer acceleratedRendering$renderType;
   private Function<RenderLayer, VertexConsumer> acceleratedRendering$bufferSources = layer -> null;
   private final Supplier<VertexConsumer> acceleratedRendering$consumer = Suppliers.memoize(
      () -> this.acceleratedRendering$bufferSources.apply(this.acceleratedRendering$renderType)
   );

   @Override
   public VertexConsumer initAcceleration(RenderLayer renderType) {
      if (AcceleratedRendering.isAvailable() && ComputeShaderProgramLoader.isProgramsLoaded()) {
         this.acceleratedRendering$renderType = renderType;
         this.acceleratedRendering$bufferSources = renderType.isOutline() ? CoreBuffers.OUTLINE : CoreBuffers.getCoreBufferSourceSet();
      }
      return this;
   }

   @Override
   public boolean isAccelerated() {
      return AcceleratedRendering.isAvailable() && this.acceleratedRendering$consumer.get() != null;
   }

   @Override
   public <T> void doRender(
      IAcceleratedRenderer<T> renderer, T context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color
   ) {
      VertexConsumer consumer = this.acceleratedRendering$consumer.get();
      if (consumer != null) {
         ((IAcceleratedVertexConsumer)consumer).doRender(renderer, context, transform, normal, light, overlay, color);
      }
   }

   public BufferBuilder(BufferAllocator allocator, VertexFormat.DrawMode drawMode, VertexFormat format) {
      format = this.iris$extendFormat(format);
      if (!format.has(VertexFormatElement.POSITION)) {
         throw new IllegalArgumentException("Cannot build mesh with no position element");
      }

      this.allocator = allocator;
      this.drawMode = drawMode;
      this.format = format;
      this.vertexSizeByte = format.getVertexSizeByte();
      this.requiredMask = format.getRequiredMask() & ~VertexFormatElement.POSITION.getBit();
      this.offsetsByElementId = format.getOffsetsByElementId();
      boolean bl = format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
      boolean bl2 = format == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
      this.canSkipElementChecks = bl || bl2;
      this.hasOverlay = bl;
   }

   private VertexFormat iris$extendFormat(VertexFormat format) {
      this.iris$injectNormalAndUv1 = false;
      if (ImmediateState.skipExtension.get() || !Iris.isPackInUseQuick()) {
         return format;
      }

      if (format == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL || format == IrisVertexFormats.TERRAIN) {
         this.iris$extending = true;
         return IrisVertexFormats.TERRAIN;
      } else if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL || format == IrisVertexFormats.ENTITY) {
         this.iris$extending = true;
         return IrisVertexFormats.ENTITY;
      } else if (format == VertexFormats.POSITION_COLOR_TEXTURE_LIGHT || format == IrisVertexFormats.GLYPH) {
         this.iris$extending = true;
         this.iris$injectNormalAndUv1 = true;
         return IrisVertexFormats.GLYPH;
      }

      return format;
   }

   @Nullable
   public BuiltBuffer endNullable() {
      this.ensureBuilding();
      this.endVertex();
      BuiltBuffer builtBuffer = this.build();
      this.building = false;
      this.vertexPointer = -1L;
      return builtBuffer;
   }

   public BuiltBuffer end() {
      BuiltBuffer builtBuffer = this.endNullable();
      if (builtBuffer == null) {
         throw new IllegalStateException("BufferBuilder was empty");
      } else {
         return builtBuffer;
      }
   }

   private void ensureBuilding() {
      if (!this.building) {
         throw new IllegalStateException("Not building!");
      }
   }

   @Nullable
   private BuiltBuffer build() {
      if (this.vertexCount == 0) {
         return null;
      }

      BufferAllocator.CloseableBuffer closeableBuffer = this.allocator.getAllocated();
      if (closeableBuffer == null) {
         return null;
      }

      int i = this.drawMode.getIndexCount(this.vertexCount);
      VertexFormat.IndexType indexType = VertexFormat.IndexType.smallestFor(this.vertexCount);
      return new BuiltBuffer(closeableBuffer, new BuiltBuffer.DrawParameters(this.format, this.vertexCount, i, this.drawMode, indexType));
   }

   private long beginVertex() {
      this.ensureBuilding();
      this.endVertex();
      this.vertexCount++;
      long l = this.allocator.allocate(this.vertexSizeByte);
      this.vertexPointer = l;
      return l;
   }

   private long beginElement(VertexFormatElement element) {
      int i = this.currentMask;
      int j = i & ~element.getBit();
      if (j == i) {
         return -1L;
      } else {
         this.currentMask = j;
         long l = this.vertexPointer;
         if (l == -1L) {
            throw new IllegalArgumentException("Not currently building vertex");
         } else {
            return l + this.offsetsByElementId[element.id()];
         }
      }
   }

   private void endVertex() {
      this.iris$beforeEndVertex();
      if (this.vertexCount != 0) {
         if (this.currentMask != 0) {
            String string = VertexFormatElement.streamFromMask(this.currentMask).map(this.format::getName).collect(Collectors.joining(", "));
            throw new IllegalStateException("Missing elements in vertex: " + string);
         }

         if (this.drawMode == VertexFormat.DrawMode.LINES || this.drawMode == VertexFormat.DrawMode.LINE_STRIP) {
            long l = this.allocator.allocate(this.vertexSizeByte);
            MemoryUtil.memCopy(l - this.vertexSizeByte, l, this.vertexSizeByte);
            this.vertexCount++;
         }
      }

      this.batchedentityrendering$afterEndVertex();
   }

   private void iris$beforeEndVertex() {
      if (this.vertexCount == 0 || !this.iris$extending) {
         return;
      }

      this.currentMask &= ~IrisVertexFormats.MID_TEXTURE_ELEMENT.getBit();
      this.currentMask &= ~IrisVertexFormats.TANGENT_ELEMENT.getBit();
      if (this.iris$injectNormalAndUv1 && (this.currentMask & VertexFormatElement.NORMAL.getBit()) != 0) {
         this.normal(0.0F, 1.0F, 0.0F);
      }

      if (this.iris$skipEndVertexOnce) {
         this.iris$skipEndVertexOnce = false;
         return;
      }

      if (this.drawMode != VertexFormat.DrawMode.QUADS && this.drawMode != VertexFormat.DrawMode.TRIANGLES) {
         return;
      }

      this.iris$vertexOffsets[this.iris$vertexCount++] = this.vertexPointer - ((MojangBufferAccessor)this.allocator).getPointer();
      int verticesPerPrimitive = this.drawMode == VertexFormat.DrawMode.QUADS ? 4 : 3;
      if (this.iris$vertexCount == verticesPerPrimitive) {
         this.iris$fillExtendedData(verticesPerPrimitive);
      }
   }

   private static void putColor(long pointer, int argb) {
      int i = ColorHelper.toAbgr(argb);
      MemoryUtil.memPutInt(pointer, LITTLE_ENDIAN ? i : Integer.reverseBytes(i));
   }

   private static void putInt(long pointer, int i) {
      if (LITTLE_ENDIAN) {
         MemoryUtil.memPutInt(pointer, i);
      } else {
         MemoryUtil.memPutShort(pointer, (short)(i & 65535));
         MemoryUtil.memPutShort(pointer + 2L, (short)(i >> 16 & 65535));
      }
   }

   @Override
   public VertexConsumer vertex(float x, float y, float z) {
      long l = this.beginVertex() + this.offsetsByElementId[VertexFormatElement.POSITION.id()];
      this.currentMask = this.requiredMask;
      MemoryUtil.memPutFloat(l, x);
      MemoryUtil.memPutFloat(l + 4L, y);
      MemoryUtil.memPutFloat(l + 8L, z);
      this.iris$injectExtendedVertexData(x, y, z);
      return this;
   }

   private void iris$injectExtendedVertexData(float x, float y, float z) {
      if ((this.currentMask & IrisVertexFormats.MID_BLOCK_ELEMENT.getBit()) != 0) {
         long offset = this.beginElement(IrisVertexFormats.MID_BLOCK_ELEMENT);
         int midBlock = this.iris$ignoreMidBlock
            ? 0
            : ExtendedDataHelper.computeMidBlock(
               x, y, z, this.iris$currentLocalPosX, this.iris$currentLocalPosY, this.iris$currentLocalPosZ
            );
         MemoryUtil.memPutInt(offset, midBlock);
         MemoryUtil.memPutByte(offset + 3L, this.iris$currentBlockEmission);
      }

      if ((this.currentMask & IrisVertexFormats.ENTITY_ELEMENT.getBit()) != 0) {
         long offset = this.beginElement(IrisVertexFormats.ENTITY_ELEMENT);
         MemoryUtil.memPutShort(offset, (short)this.iris$currentBlock);
         MemoryUtil.memPutShort(offset + 2L, (short)this.iris$currentRenderType);
      } else if ((this.currentMask & IrisVertexFormats.ENTITY_ID_ELEMENT.getBit()) != 0) {
         long offset = this.beginElement(IrisVertexFormats.ENTITY_ID_ELEMENT);
         MemoryUtil.memPutShort(offset, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
         MemoryUtil.memPutShort(offset + 2L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
         MemoryUtil.memPutShort(offset + 4L, (short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
      }
   }

   private void iris$fillExtendedData(int vertexAmount) {
      this.iris$vertexCount = 0;
      int stride = this.format.getVertexSizeByte();
      long bufferPointer = ((MojangBufferAccessor)this.allocator).getPointer();
      this.iris$polygon.setup(bufferPointer, this.iris$vertexOffsets, stride, vertexAmount);
      float midU = 0.0F;
      float midV = 0.0F;

      for (int vertex = 0; vertex < vertexAmount; vertex++) {
         midU += this.iris$polygon.u(vertex);
         midV += this.iris$polygon.v(vertex);
      }

      midU /= vertexAmount;
      midV /= vertexAmount;
      int midTextureOffset = this.offsetsByElementId[IrisVertexFormats.MID_TEXTURE_ELEMENT.id()];
      int normalOffset = this.offsetsByElementId[VertexFormatElement.NORMAL.id()];
      int tangentOffset = this.offsetsByElementId[IrisVertexFormats.TANGENT_ELEMENT.id()];
      if (vertexAmount == 3) {
         for (int vertex = 0; vertex < vertexAmount; vertex++) {
            long pointer = bufferPointer + this.iris$vertexOffsets[vertex];
            int packedNormal = MemoryUtil.memGetInt(pointer + normalOffset);
            int tangent = NormalHelper.computeTangentSmooth(
               NormI8.unpackX(packedNormal), NormI8.unpackY(packedNormal), NormI8.unpackZ(packedNormal), this.iris$polygon
            );
            MemoryUtil.memPutFloat(pointer + midTextureOffset, midU);
            MemoryUtil.memPutFloat(pointer + midTextureOffset + 4L, midV);
            MemoryUtil.memPutInt(pointer + tangentOffset, tangent);
         }
      } else {
         boolean recalculateNormal = ImmediateState.isRenderingLevel;
         NormalHelper.computeFaceNormal(this.iris$normal, this.iris$polygon);
         int packedNormal = recalculateNormal ? NormI8.pack(this.iris$normal.x, this.iris$normal.y, this.iris$normal.z, 0.0F) : 0;
         int tangent = NormalHelper.computeTangent(this.iris$normal.x, this.iris$normal.y, this.iris$normal.z, this.iris$polygon);

         for (int vertex = 0; vertex < vertexAmount; vertex++) {
            long pointer = bufferPointer + this.iris$vertexOffsets[vertex];
            MemoryUtil.memPutFloat(pointer + midTextureOffset, midU);
            MemoryUtil.memPutFloat(pointer + midTextureOffset + 4L, midV);
            if (recalculateNormal) {
               MemoryUtil.memPutInt(pointer + normalOffset, packedNormal);
            }

            MemoryUtil.memPutInt(pointer + tangentOffset, tangent);
         }
      }

      Arrays.fill(this.iris$vertexOffsets, 0L);
   }

   @Override
   public VertexConsumer color(int red, int green, int blue, int alpha) {
      long l = this.beginElement(VertexFormatElement.COLOR);
      if (l != -1L) {
         MemoryUtil.memPutByte(l, (byte)red);
         MemoryUtil.memPutByte(l + 1L, (byte)green);
         MemoryUtil.memPutByte(l + 2L, (byte)blue);
         MemoryUtil.memPutByte(l + 3L, (byte)alpha);
      }

      return this;
   }

   @Override
   public VertexConsumer color(int argb) {
      long l = this.beginElement(VertexFormatElement.COLOR);
      if (l != -1L) {
         putColor(l, argb);
      }

      return this;
   }

   @Override
   public VertexConsumer texture(float u, float v) {
      long l = this.beginElement(VertexFormatElement.UV_0);
      if (l != -1L) {
         MemoryUtil.memPutFloat(l, u);
         MemoryUtil.memPutFloat(l + 4L, v);
      }

      return this;
   }

   @Override
   public VertexConsumer overlay(int u, int v) {
      return this.putUv((short)u, (short)v, VertexFormatElement.UV_1);
   }

   @Override
   public VertexConsumer overlay(int uv) {
      long l = this.beginElement(VertexFormatElement.UV_1);
      if (l != -1L) {
         putInt(l, uv);
      }

      return this;
   }

   @Override
   public VertexConsumer light(int u, int v) {
      return this.putUv((short)u, (short)v, VertexFormatElement.UV_2);
   }

   @Override
   public VertexConsumer light(int uv) {
      long l = this.beginElement(VertexFormatElement.UV_2);
      if (l != -1L) {
         putInt(l, uv);
      }

      return this;
   }

   private VertexConsumer putUv(short u, short v, VertexFormatElement element) {
      long l = this.beginElement(element);
      if (l != -1L) {
         MemoryUtil.memPutShort(l, u);
         MemoryUtil.memPutShort(l + 2L, v);
      }

      return this;
   }

   @Override
   public VertexConsumer normal(float x, float y, float z) {
      long l = this.beginElement(VertexFormatElement.NORMAL);
      if (l != -1L) {
         MemoryUtil.memPutByte(l, floatToByte(x));
         MemoryUtil.memPutByte(l + 1L, floatToByte(y));
         MemoryUtil.memPutByte(l + 2L, floatToByte(z));
      }

      return this;
   }

   private static byte floatToByte(float f) {
      return (byte)((int)(MathHelper.clamp(f, -1.0F, 1.0F) * 127.0F) & 0xFF);
   }

   @Override
   public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
      this.iris$currentBlock = block;
      this.iris$currentRenderType = renderType;
      this.iris$currentBlockEmission = blockEmission;
      this.iris$currentLocalPosX = localPosX;
      this.iris$currentLocalPosY = localPosY;
      this.iris$currentLocalPosZ = localPosZ;
      this.iris$oldBlock = -1;
   }

   @Override
   public void overrideBlock(int block) {
      if (this.iris$currentBlock != block) {
         if (this.iris$oldBlock == -1) {
            this.iris$oldBlock = this.iris$currentBlock;
         }

         this.iris$currentBlock = block;
      }
   }

   @Override
   public void restoreBlock() {
      if (this.iris$oldBlock != -1) {
         this.iris$currentBlock = this.iris$oldBlock;
         this.iris$oldBlock = -1;
      }
   }

   @Override
   public void endBlock() {
      this.iris$currentBlock = -1;
      this.iris$currentRenderType = -1;
      this.iris$currentBlockEmission = -1;
      this.iris$currentLocalPosX = 0;
      this.iris$currentLocalPosY = 0;
      this.iris$currentLocalPosZ = 0;
      this.iris$oldBlock = -1;
   }

   @Override
   public void ignoreMidBlock(boolean ignore) {
      this.iris$ignoreMidBlock = ignore;
   }

   @Override
   public void splitStrip() {
      if (this.vertexCount == 0) {
         return;
      }

      this.batchedentityrendering$duplicateLastVertex();
      this.batchedentityrendering$duplicateNextVertexAfter = true;
      this.batchedentityrendering$duplicateNextVertex = false;
   }

   private void batchedentityrendering$duplicateLastVertex() {
      long destination = this.allocator.allocate(this.vertexSizeByte);
      MemoryIntrinsics.copyMemory(destination - this.vertexSizeByte, destination, this.vertexSizeByte);
      this.vertexCount++;
   }

   private void batchedentityrendering$afterEndVertex() {
      if (this.batchedentityrendering$duplicateNextVertexAfter) {
         this.batchedentityrendering$duplicateNextVertexAfter = false;
         this.batchedentityrendering$duplicateNextVertex = true;
      } else if (this.batchedentityrendering$duplicateNextVertex) {
         this.batchedentityrendering$duplicateNextVertex = false;
         this.batchedentityrendering$duplicateLastVertex();
      }
   }

   @Override
   public void sodium$duplicateVertex() {
      if (this.vertexCount != 0) {
         long destination = this.allocator.allocate(this.vertexSizeByte);
         MemoryIntrinsics.copyMemory(destination - this.vertexSizeByte, destination, this.vertexSizeByte);
         this.vertexCount++;
      }
   }

   @Override
   public void push(MemoryStack stack, long source, int count, VertexFormat sourceFormat) {
      this.ensureBuilding();
      this.endVertex();
      int length = count * this.vertexSizeByte;
      long destination = this.allocator.allocate(length);
      if (sourceFormat == this.format) {
         MemoryIntrinsics.copyMemory(source, destination, length);
      } else {
         VertexSerializerRegistry.instance().get(sourceFormat, this.format).serialize(source, destination, count);
      }

      this.vertexCount += count;
      this.vertexPointer = destination + length - this.vertexSizeByte;
      this.currentMask = 0;
      this.iris$skipEndVertexOnce = true;
   }

   @Override
   public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
      if (this.canSkipElementChecks && !this.iris$extending) {
         long l = this.beginVertex();
         MemoryUtil.memPutFloat(l + 0L, x);
         MemoryUtil.memPutFloat(l + 4L, y);
         MemoryUtil.memPutFloat(l + 8L, z);
         putColor(l + 12L, color);
         MemoryUtil.memPutFloat(l + 16L, u);
         MemoryUtil.memPutFloat(l + 20L, v);
         long m;
         if (this.hasOverlay) {
            putInt(l + 24L, overlay);
            m = l + 28L;
         } else {
            m = l + 24L;
         }

         putInt(m + 0L, light);
         MemoryUtil.memPutByte(m + 4L, floatToByte(normalX));
         MemoryUtil.memPutByte(m + 5L, floatToByte(normalY));
         MemoryUtil.memPutByte(m + 6L, floatToByte(normalZ));
      } else {
         VertexConsumer.super.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
      }
   }
}
