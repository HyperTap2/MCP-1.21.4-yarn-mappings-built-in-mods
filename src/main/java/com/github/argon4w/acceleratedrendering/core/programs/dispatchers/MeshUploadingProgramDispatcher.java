package com.github.argon4w.acceleratedrendering.core.programs.dispatchers;

import com.github.argon4w.acceleratedrendering.core.backends.buffers.IServerBuffer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSetPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes.MeshUploaderPool;
import com.github.argon4w.acceleratedrendering.core.programs.ReloadableComputeProgram;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisMeshInfoCache;
import com.github.argon4w.acceleratedrendering.compat.iris.programs.IrisPrograms;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class MeshUploadingProgramDispatcher {

    public static final int SMALL_MESH_BUFFER_INDEX = 5;
    public static final int MESH_BUFFER_INDEX = 7;
    private static final int GROUP_SIZE = 128;
    private static final int DISPATCH_COUNT_Y_Z = 1;
    private final Map<IServerBuffer, List<MeshUploaderPool.MeshUploader>> denseUploaders;
    private final Map<IServerBuffer, List<MeshUploaderPool.MeshUploader>> sparseUploaders;
    private final ReloadableComputeProgram program;
    private final boolean irisLayout;

    public MeshUploadingProgramDispatcher(Identifier key) {
        this.denseUploaders = new Reference2ObjectLinkedOpenHashMap<>();
        this.sparseUploaders = new Reference2ObjectLinkedOpenHashMap<>();
        this.program = new ReloadableComputeProgram(key);
        this.irisLayout = key.equals(IrisPrograms.IRIS_BLOCK_MESH_UPLOADING_KEY)
                || key.equals(IrisPrograms.IRIS_ENTITY_MESH_UPLOADING_KEY)
                || key.equals(IrisPrograms.IRIS_GLYPH_MESH_UPLOADING_KEY);
    }

    public void dispatch(Collection<AcceleratedBufferBuilder> builders, AcceleratedBufferSetPool.BufferSet bufferSet) {
        var computeProgram = this.program.get();
        var meshCountUniform = this.program.uniform("meshCount");
        var meshSizeUniform = this.program.uniform("meshSize");
        var meshOffsetUniform = this.program.uniform("meshOffset");
        var transformProgramDispatcher = bufferSet
                .getBufferEnvironment()
                .selectTransformProgramDispatcher();

        for (var builder : builders) {
            var vertexBuffer = builder.getVertexBuffer();
            var varyingBuffer = builder.getVaryingBuffer();
            var vertexSize = builder.getVertexSize();
            var meshVertexCount = builder.getMeshVertexCount();

            vertexBuffer.allocateOffset(meshVertexCount * vertexSize);
            varyingBuffer.allocateOffset(meshVertexCount * AcceleratedBufferBuilder.VARYING_SIZE);
        }

        bufferSet.prepare();
        bufferSet.bindTransformBuffers();

        for (var builder : builders) {
            var vertexBuffer = builder.getVertexBuffer();
            var varyingBuffer = builder.getVaryingBuffer();
            var vertexSize = builder.getVertexSize();
            var offset = 0;
            var sparseStart = 0;
            var vertexCount = builder.getVertexCount();
            var meshVertexCount = builder.getMeshVertexCount();
            var vertexAddress = vertexBuffer.reserve(meshVertexCount * vertexSize);
            var varyingAddress = varyingBuffer.reserve(meshVertexCount * AcceleratedBufferBuilder.VARYING_SIZE);
            var vertexOffset = vertexBuffer.getOffset() / vertexSize;
            var varyingOffset = varyingBuffer.getOffset() / AcceleratedBufferBuilder.VARYING_SIZE;

            for (var uploader : builder
                    .getMeshUploaders()
                    .values()
            ) {
                var serverMesh = uploader.getServerMesh();
                var meshCount = uploader.getMeshInfos().getMeshCount();
                var buffer = serverMesh.meshBuffer();
                var dense = denseUploaders.get(buffer);
                var sparse = sparseUploaders.get(buffer);

                if (dense == null) {
                    dense = new ReferenceArrayList<>();
                    sparse = new ReferenceArrayList<>();
                    denseUploaders.put(buffer, dense);
                    sparseUploaders.put(buffer, sparse);
                }

                (meshCount < 128
                        ? sparse
                        : dense).add(uploader);
            }

            for (var buffer : sparseUploaders.keySet()) {
                for (var uploader : sparseUploaders.get(buffer)) {
                    var mesh = uploader.getServerMesh();
                    var meshInfos = uploader.getMeshInfos();
                    var meshCount = meshInfos.getMeshCount();
                    var meshSize = mesh.size();

                    for (var i = 0; i < meshCount; i++) {
                        builder.getColorOffset().putInt(vertexAddress, offset, ColorHelper.toAbgr(meshInfos.getColor(i)));
                        builder.getUv1Offset().putInt(vertexAddress, offset, meshInfos.getOverlay(i));
                        builder.getUv2Offset().putInt(vertexAddress, offset, meshInfos.getLight(i));
                        if (irisLayout) {
                            IIrisMeshInfoCache irisMeshInfos = (IIrisMeshInfoCache) meshInfos;
                            builder.writeIrisData(
                                    vertexAddress,
                                    offset,
                                    irisMeshInfos.getRenderedEntity(i),
                                    irisMeshInfos.getRenderedBlockEntity(i),
                                    irisMeshInfos.getRenderedItem(i)
                            );
                        }

                        AcceleratedBufferBuilder.VARYING_SHARING.putInt(varyingAddress, offset, meshInfos.getSharing(i));
                        AcceleratedBufferBuilder.VARYING_MESH.putInt(varyingAddress, offset, (int) mesh.offset());
                        AcceleratedBufferBuilder.VARYING_SHOULD_CULL.putInt(varyingAddress, offset, meshInfos.getShouldCull(i));

                        for (var j = 0; j < meshSize; j++) {
                            AcceleratedBufferBuilder.VARYING_OFFSET.putInt(varyingAddress, offset + j, j);
                        }

                        offset += (int) meshSize;
                    }
                }

                if (offset != 0) {
                    buffer.bindBase(GL_SHADER_STORAGE_BUFFER, SMALL_MESH_BUFFER_INDEX);
                    transformProgramDispatcher.dispatch(
                            vertexBuffer,
                            varyingBuffer,
                            offset,
                            sparseStart + vertexCount + vertexOffset,
                            sparseStart + vertexCount + varyingOffset
                    );
                }

                sparseStart = offset;
            }

            for (var buffer : denseUploaders.keySet()) {
                computeProgram.useProgram();
                buffer.bindBase(GL_SHADER_STORAGE_BUFFER, MESH_BUFFER_INDEX);

                for (var uploader : denseUploaders.get(buffer)) {
                    var meshCount = uploader.getMeshInfos().getMeshCount();
                    var mesh = uploader.getServerMesh();
                    var meshSize = mesh.size();
                    var uploadSize = meshCount * meshSize;

                    uploader.upload(
                            (int)(offset + vertexCount + vertexOffset),
                            (int)(offset + vertexCount + varyingOffset),
                            irisLayout
                    );
                    uploader.bindBuffers();
                    meshCountUniform.uploadUnsignedInt(meshCount);
                    meshSizeUniform.uploadUnsignedInt((int) meshSize);
                    meshOffsetUniform.uploadUnsignedInt((int) mesh.offset());

                    computeProgram.dispatch(
                            (int)((uploadSize + GROUP_SIZE - 1) / GROUP_SIZE),
                            DISPATCH_COUNT_Y_Z,
                            DISPATCH_COUNT_Y_Z
                    );

                    offset += uploadSize;
                }
            }

            for (var buffer : denseUploaders.keySet()) {
                denseUploaders.get(buffer).clear();
                sparseUploaders.get(buffer).clear();
            }
        }

        computeProgram.resetProgram();
        computeProgram.waitBarriers(transformProgramDispatcher.getBarrierFlags());
    }
}
