package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSetPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.ElementBufferPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.StagingBufferPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes.MeshUploaderPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryInterface;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.SimpleMemoryInterface;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisAcceleratedBufferBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AcceleratedBufferBuilder implements IAcceleratedVertexConsumer, IIrisAcceleratedBufferBuilder, VertexConsumer {

    public static final long VARYING_SIZE = 4L * 4L;
    public static final IMemoryInterface VARYING_OFFSET = new SimpleMemoryInterface(0L, VARYING_SIZE);
    public static final IMemoryInterface VARYING_SHARING = new SimpleMemoryInterface(4L, VARYING_SIZE);
    public static final IMemoryInterface VARYING_MESH = new SimpleMemoryInterface(2L * 4L, VARYING_SIZE);
    public static final IMemoryInterface VARYING_SHOULD_CULL = new SimpleMemoryInterface(3L * 4L, VARYING_SIZE);

    public static final long SHARING_SIZE = 4L * 4L * 4L + 4L * 3L * 4L;
    public static final IMemoryInterface SHARING_TRANSFORM = new SimpleMemoryInterface(0L, SHARING_SIZE);
    public static final IMemoryInterface SHARING_NORMAL = new SimpleMemoryInterface(4L * 4L * 4L, SHARING_SIZE);

    @Getter
    private final Map<ServerMesh, MeshUploaderPool.MeshUploader> meshUploaders;
    @Getter
    private final StagingBufferPool.StagingBuffer vertexBuffer;
    @Getter
    private final StagingBufferPool.StagingBuffer varyingBuffer;
    @Getter
    private final ElementBufferPool.ElementSegment elementSegment;
    private final AcceleratedBufferSetPool.BufferSet bufferSet;


    @EqualsAndHashCode.Include
    private final IMemoryLayout<VertexFormatElement> layout;
    @EqualsAndHashCode.Include
    private final RenderLayer renderType;
    @Getter
    private final ICullingProgramDispatcher cullingProgramDispatcher;
    @Getter
    private final VertexFormat.DrawMode mode;
    @Getter
    private final long vertexSize;
    private final int polygonSize;
    private final int polygonElementCount;

    private final IMemoryInterface posOffset;
    @Getter
    private final IMemoryInterface colorOffset;
    private final IMemoryInterface uv0Offset;
    @Getter
    private final IMemoryInterface uv1Offset;
    @Getter
    private final IMemoryInterface uv2Offset;
    private final IMemoryInterface normalOffset;
    private final IMemoryInterface entityIdOffset;
    private final IMemoryInterface entityOffset;
    private final Matrix4f cachedTransformValue;
    private final Matrix3f cachedNormalValue;
    private int elementCount;
    @Getter
    private int meshVertexCount;
    @Getter
    private int vertexCount;
    private long vertexAddress;
    private long sharingAddress;
    private int activeSharing;
    private int cachedSharing;
    private Matrix4f cachedTransform;
    private Matrix3f cachedNormal;

    public AcceleratedBufferBuilder(
            StagingBufferPool.StagingBuffer vertexBuffer,
            StagingBufferPool.StagingBuffer varyingBuffer,
            ElementBufferPool.ElementSegment elementSegment,
            AcceleratedBufferSetPool.BufferSet bufferSet,
            RenderLayer renderType
    ) {
        this.meshUploaders = new Reference2ObjectLinkedOpenHashMap<>();
        this.vertexBuffer = vertexBuffer;
        this.varyingBuffer = varyingBuffer;
        this.elementSegment = elementSegment;
        this.bufferSet = bufferSet;


        this.renderType = renderType;
        this.layout = this.bufferSet.getBufferEnvironment().getLayout();
        this.cullingProgramDispatcher = this.bufferSet.getBufferEnvironment().selectCullingProgramDispatcher(this.renderType);
        this.mode = this.renderType.getDrawMode();
        this.vertexSize = this.bufferSet.getVertexSize();
        this.polygonSize = this.mode.firstVertexCount;
        this.polygonElementCount = this.mode.getIndexCount(this.polygonSize);

        this.posOffset = this.layout.getElement(VertexFormatElement.POSITION);
        this.colorOffset = this.layout.getElement(VertexFormatElement.COLOR);
        this.uv0Offset = this.layout.getElement(VertexFormatElement.UV_0);
        this.uv1Offset = this.layout.getElement(VertexFormatElement.UV_1);
        this.uv2Offset = this.layout.getElement(VertexFormatElement.UV_2);
        this.normalOffset = this.layout.getElement(VertexFormatElement.NORMAL);
        this.entityIdOffset = this.layout.getElement(IrisVertexFormats.ENTITY_ID_ELEMENT);
        this.entityOffset = this.layout.getElement(IrisVertexFormats.ENTITY_ELEMENT);

        this.elementCount = 0;
        this.meshVertexCount = 0;
        this.vertexCount = 0;
        this.vertexAddress = -1;
        this.sharingAddress = -1;
        this.activeSharing = -1;
        this.cachedSharing = -1;

        this.cachedTransform = null;
        this.cachedNormal = null;

        this.cachedTransformValue = new Matrix4f();
        this.cachedNormalValue = new Matrix3f();
    }

    @Override
    public VertexConsumer vertex(
            MatrixStack.Entry pPose,
            float pX,
            float pY,
            float pZ
    ) {
        beginTransform(pPose.getPositionMatrix(), pPose.getNormalMatrix());
        return vertex(
                pX,
                pY,
                pZ
        );
    }

    @Override
    public VertexConsumer vertex(
            float pX,
            float pY,
            float pZ
    ) {
        var vertexAddress = vertexBuffer.reserve(vertexSize);
        var varyingAddress = varyingBuffer.reserve(VARYING_SIZE);

        this.vertexAddress = vertexAddress;

        posOffset.putFloat(vertexAddress, pX);
        posOffset.putFloat(vertexAddress + 4L, pY);
        posOffset.putFloat(vertexAddress + 8L, pZ);
        this.writeCurrentIrisData(vertexAddress);

        VARYING_OFFSET.putInt(varyingAddress, 0);
        VARYING_SHARING.putInt(varyingAddress, activeSharing);
        VARYING_MESH.putInt(varyingAddress, -1);
        VARYING_SHOULD_CULL.putInt(varyingAddress, cullingProgramDispatcher.shouldCull() ? 1 : 0);

        vertexCount++;
        elementCount++;

        if (elementCount >= polygonSize) {
            elementSegment.countElements(polygonElementCount);
            elementCount = 0;
            activeSharing = -1;
        }

        return this;
    }

    @Override
    public VertexConsumer color(
            int pRed,
            int pGreen,
            int pBlue,
            int pAlpha
    ) {
        if (vertexAddress == -1) {
            throw new IllegalStateException("Vertex not building!");
        }

        colorOffset.putByte(vertexAddress, (byte) pRed);
        colorOffset.putByte(vertexAddress + 1L, (byte) pGreen);
        colorOffset.putByte(vertexAddress + 2L, (byte) pBlue);
        colorOffset.putByte(vertexAddress + 3L, (byte) pAlpha);

        return this;
    }

    @Override
    public VertexConsumer texture(float pU, float pV) {
        if (vertexAddress == -1) {
            throw new IllegalStateException("Vertex not building!");
        }

        uv0Offset.putFloat(vertexAddress, pU);
        uv0Offset.putFloat(vertexAddress + 4L, pV);

        return this;
    }

    @Override
    public VertexConsumer overlay(int pU, int pV) {
        if (vertexAddress == -1) {
            throw new IllegalStateException("Vertex not building!");
        }

        uv1Offset.putShort(vertexAddress, (short) pU);
        uv1Offset.putShort(vertexAddress + 2L, (short) pV);

        return this;
    }

    @Override
    public VertexConsumer light(int pU, int pV) {
        if (vertexAddress == -1) {
            throw new IllegalStateException("Vertex not building!");
        }

        uv2Offset.putShort(vertexAddress, (short) pU);
        uv2Offset.putShort(vertexAddress + 2L, (short) pV);

        return this;
    }

    @Override
    public VertexConsumer normal(
            MatrixStack.Entry pPose,
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        var normal = pPose.getNormalMatrix();

        if (activeSharing == -1) {
            return VertexConsumer.super.normal(
                    pPose,
                    pNormalX,
                    pNormalY,
                    pNormalZ
            );
        }

        if (!normal.equals(cachedNormal)) {
            SHARING_NORMAL.putMatrix3f(sharingAddress, normal);
        }

        return normal(
                pNormalX,
                pNormalY,
                pNormalZ
        );
    }

    @Override
    public VertexConsumer normal(
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        if (vertexAddress == -1) {
            throw new IllegalStateException("Vertex not building!");
        }

        normalOffset.putNormal(vertexAddress, pNormalX);
        normalOffset.putNormal(vertexAddress + 1L, pNormalY);
        normalOffset.putNormal(vertexAddress + 2L, pNormalZ);
        this.writeCurrentIrisData(vertexAddress);

        return this;
    }

    @Override
    public void vertex(
            float pX,
            float pY,
            float pZ,
            int pColor,
            float pU,
            float pV,
            int pPackedOverlay,
            int pPackedLight,
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        var vertexAddress = vertexBuffer.reserve(vertexSize);
        var varyingAddress = varyingBuffer.reserve(VARYING_SIZE);

        posOffset.putFloat(vertexAddress, pX);
        posOffset.putFloat(vertexAddress + 4L, pY);
        posOffset.putFloat(vertexAddress + 8L, pZ);
        colorOffset.putInt(vertexAddress, ColorHelper.toAbgr(pColor));
        uv0Offset.putFloat(vertexAddress, pU);
        uv0Offset.putFloat(vertexAddress + 4L, pV);
        uv1Offset.putInt(vertexAddress, pPackedOverlay);
        uv2Offset.putInt(vertexAddress, pPackedLight);
        normalOffset.putNormal(vertexAddress, pNormalX);
        normalOffset.putNormal(vertexAddress + 1L, pNormalY);
        normalOffset.putNormal(vertexAddress + 2L, pNormalZ);
        this.writeCurrentIrisData(vertexAddress);

        VARYING_OFFSET.putInt(varyingAddress, 0);
        VARYING_SHARING.putInt(varyingAddress, activeSharing);
        VARYING_MESH.putInt(varyingAddress, -1);
        VARYING_SHOULD_CULL.putInt(varyingAddress, cullingProgramDispatcher.shouldCull() ? 1 : 0);

        vertexCount++;
        elementCount++;

        if (elementCount >= polygonSize) {
            elementSegment.countElements(polygonElementCount);
            elementCount = 0;
            activeSharing = -1;
        }
    }

    @Override
    public void beginTransform(Matrix4f transform, Matrix3f normal) {
        if (CoreFeature.shouldCacheIdenticalPose()
                && transform.equals(cachedTransform)
                && normal.equals(cachedNormal)
        ) {
            activeSharing = cachedSharing;
            return;
        }

        cachedTransform = cachedTransformValue.set(transform);
        cachedNormal = cachedNormalValue.set(normal);

        sharingAddress = bufferSet.reserveSharing();
        cachedSharing = bufferSet.getSharing();
        activeSharing = cachedSharing;

        SHARING_TRANSFORM.putMatrix4f(sharingAddress, transform);
        SHARING_NORMAL.putMatrix3f(sharingAddress, normal);
    }

    @Override
    public void endTransform() {
        cachedTransform = null;
        cachedNormal = null;
        activeSharing = -1;
        cachedSharing = -1;
    }

    @Override
    public void addClientMesh(
            ByteBuffer meshBuffer,
            int size,
            int color,
            int light,
            int overlay
    ) {
        var bufferSize = vertexSize * size;
        var vertexAddress = vertexBuffer.reserve(bufferSize);
        var varyingAddress = varyingBuffer.reserve(VARYING_SIZE * size);

        MemoryUtil.memCopy(
                MemoryUtil.memAddress0(meshBuffer),
                vertexAddress,
                bufferSize
        );

        colorOffset.putInt(vertexAddress, ColorHelper.toAbgr(color));
        uv1Offset.putInt(vertexAddress, overlay);
        uv2Offset.putInt(vertexAddress, light);

        VARYING_SHARING.putInt(varyingAddress, activeSharing);
        VARYING_MESH.putInt(varyingAddress, -1);
        VARYING_SHOULD_CULL.putInt(varyingAddress, cullingProgramDispatcher.shouldCull() ? 1 : 0);

        for (int i = 0; i < size; i++) {
            VARYING_OFFSET.putInt(varyingAddress, i, i);
        }

        elementSegment.countElements(mode.getIndexCount(size));
        vertexCount += size;
    }

    @Override
    public void addServerMesh(
            ServerMesh serverMesh,
            int color,
            int light,
            int overlay
    ) {
        var meshSize = (int) serverMesh.size();
        var meshUploader = meshUploaders.get(serverMesh);
        meshVertexCount = meshVertexCount + meshSize;

        if (meshUploader == null) {
            meshUploader = bufferSet.getMeshUploader();
            meshUploader.setServerMesh(serverMesh);
            meshUploaders.put(serverMesh, meshUploader);
        }

        elementSegment.countElements(mode.getIndexCount(meshSize));
        meshUploader.addUpload(
                color,
                light,
                overlay,
                activeSharing,
                cullingProgramDispatcher.shouldCull() ? 1 : 0
        );
    }

    @Override
    public <T> void doRender(
            IAcceleratedRenderer<T> renderer,
            T context,
            Matrix4f transform,
            Matrix3f normal,
            int light,
            int overlay,
            int color
    ) {
        renderer.render(
                this,
                context,
                transform,
                normal,
                light,
                overlay,
                color
        );
    }

    @Override
    public VertexConsumer decorate(VertexConsumer buffer) {
        return buffer;
    }

    @Override
    public boolean isAccelerated() {
        return AcceleratedRendering.isAvailable();
    }

    @Override
    public RenderLayer getRenderType() {
        return renderType;
    }

    @Override
    public AcceleratedBufferSetPool.BufferSet getBufferSet() {
        return bufferSet;
    }

    public int getTotalVertexCount() {
        return vertexCount + meshVertexCount;
    }

    public boolean isEmpty() {
        return getTotalVertexCount() == 0;
    }

    private void writeCurrentIrisData(long address) {
        writeIrisData(
                address,
                0,
                (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity(),
                (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity(),
                (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem()
        );
    }

    public void writeIrisData(long address, int vertexIndex, short entity, short blockEntity, short item) {
        entityOffset.putShort(address, vertexIndex, (short) -1);
        entityOffset.putShort(address + 2L, vertexIndex, (short) -1);
        entityIdOffset.putShort(address, vertexIndex, entity);
        entityIdOffset.putShort(address + 2L, vertexIndex, blockEntity);
        entityIdOffset.putShort(address + 4L, vertexIndex, item);
    }

    @Override
    public IMemoryInterface getEntityIdOffset() {
        return entityIdOffset;
    }

    @Override
    public IMemoryInterface getEntityOffset() {
        return entityOffset;
    }
}
