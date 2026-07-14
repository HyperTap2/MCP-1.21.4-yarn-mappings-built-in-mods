package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.backends.buffers.MappedBuffer;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryInterface;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.SimpleMemoryInterface;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.core.utils.SimpleResetPool;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisMeshInfoCache;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.ColorHelper;
import java.util.function.IntFunction;

import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class MeshUploaderPool extends SimpleResetPool<MeshUploaderPool.MeshUploader, Void> {

    public MeshUploaderPool() {
        super(128, null);
    }

    @Override
    protected MeshUploader create(Void context, int i) {
        return new MeshUploader();
    }

    @Override
    protected void reset(MeshUploader meshUploader) {
        meshUploader.reset();
    }

    @Override
    protected void delete(MeshUploader meshUploader) {
        meshUploader.delete();
    }

    @Override
    public MeshUploader fail() {
        expand();
        return get();
    }

    public static class MeshUploader implements IntFunction<MeshInfo> {

        public static final int MESH_INFO_BUFFER_INDEX = 8;
        public static final long MESH_INFO_SIZE = 7L * 4L;
        public static final long IRIS_MESH_INFO_SIZE = 9L * 4L;

        private final MappedBuffer meshInfoBuffer;
        @Getter
        private final IMeshInfoCache meshInfos;

        @Getter
        @Setter
        private ServerMesh serverMesh;

        public MeshUploader() {
            this.meshInfoBuffer = new MappedBuffer(64L);
            this.meshInfos = MeshInfoCacheType.create(CoreFeature.getMeshInfoCacheType());

            this.serverMesh = null;
        }

        public void addUpload(
                int color,
                int light,
                int overlay,
                int sharing,
                int shouldCull
        ) {
            meshInfos.setup(
                    color,
                    light,
                    overlay,
                    sharing,
                    shouldCull
            );
        }

        public void upload(int vertexOffset, int varyingOffset, boolean irisLayout) {
            var meshCount = meshInfos.getMeshCount();
            long stride = irisLayout ? IRIS_MESH_INFO_SIZE : MESH_INFO_SIZE;
            var meshInfoAddress = meshInfoBuffer.reserve(stride * meshCount);
            var vertexOffsetField = new SimpleMemoryInterface(0L, stride);
            var varyingOffsetField = new SimpleMemoryInterface(4L, stride);
            var sharingField = new SimpleMemoryInterface(2L * 4L, stride);
            var shouldCullField = new SimpleMemoryInterface(3L * 4L, stride);
            var colorField = new SimpleMemoryInterface(4L * 4L, stride);
            var uv1Field = new SimpleMemoryInterface(5L * 4L, stride);
            var uv2Field = new SimpleMemoryInterface(6L * 4L, stride);
            var entityField = irisLayout ? new SimpleMemoryInterface(7L * 4L, stride) : null;
            var itemField = irisLayout ? new SimpleMemoryInterface(8L * 4L, stride) : null;

            for (var i = 0; i < meshCount; i++) {
                vertexOffsetField.putInt(meshInfoAddress, i, vertexOffset);
                varyingOffsetField.putInt(meshInfoAddress, i, varyingOffset);
                sharingField.putInt(meshInfoAddress, i, meshInfos.getSharing(i));
                shouldCullField.putInt(meshInfoAddress, i, meshInfos.getShouldCull(i));
                colorField.putInt(meshInfoAddress, i, ColorHelper.toAbgr(meshInfos.getColor(i)));
                uv1Field.putInt(meshInfoAddress, i, meshInfos.getOverlay(i));
                uv2Field.putInt(meshInfoAddress, i, meshInfos.getLight(i));
                if (irisLayout) {
                    IIrisMeshInfoCache irisMeshInfos = (IIrisMeshInfoCache) meshInfos;
                    entityField.putInt(meshInfoAddress, i, 0);
                    itemField.putInt(meshInfoAddress, i, 0);
                    entityField.putShort(meshInfoAddress, i, irisMeshInfos.getRenderedEntity(i));
                    entityField.putShort(meshInfoAddress + 2L, i, irisMeshInfos.getRenderedBlockEntity(i));
                    itemField.putShort(meshInfoAddress, i, irisMeshInfos.getRenderedItem(i));
                }
            }
        }

        public void bindBuffers() {
            meshInfoBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, MESH_INFO_BUFFER_INDEX);
        }

        public void reset() {
            meshInfos.reset();
            meshInfoBuffer.reset();
        }

        public void delete() {
            meshInfos.delete();
            meshInfoBuffer.delete();
        }

        @Override
        public MeshInfo apply(int value) {
            return new MeshInfo();
        }
    }
}
