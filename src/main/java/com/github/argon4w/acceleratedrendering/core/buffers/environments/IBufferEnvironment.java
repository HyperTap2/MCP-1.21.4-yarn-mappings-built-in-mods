package com.github.argon4w.acceleratedrendering.core.buffers.environments;

import com.github.argon4w.acceleratedrendering.compat.iris.environments.IrisBufferEnvironment;
import com.github.argon4w.acceleratedrendering.compat.iris.programs.IrisPrograms;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.programs.ComputeShaderPrograms;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.MeshUploadingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.TransformProgramDispatcher;
import java.util.Set;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

public interface IBufferEnvironment {

    void setupBufferState();

    boolean isAccelerated(VertexFormat vertexFormat);

    Set<VertexFormat> getVertexFormats();

    IMemoryLayout<VertexFormatElement> getLayout();

    MeshUploadingProgramDispatcher selectMeshUploadingProgramDispatcher();

    TransformProgramDispatcher selectTransformProgramDispatcher();

    ICullingProgramDispatcher selectCullingProgramDispatcher(RenderLayer renderType);

    IPolygonProgramDispatcher selectProcessingProgramDispatcher(VertexFormat.DrawMode mode);

    int getVertexSize();

    class Presets {

        public static final IBufferEnvironment BLOCK = new IrisBufferEnvironment(
                new VanillaBufferEnvironment(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, ComputeShaderPrograms.CORE_BLOCK_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_BLOCK_VERTEX_TRANSFORM_KEY),
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
                IrisVertexFormats.TERRAIN,
                IrisPrograms.IRIS_BLOCK_MESH_UPLOADING_KEY,
                IrisPrograms.IRIS_BLOCK_VERTEX_TRANSFORM_KEY
        );
        public static final IBufferEnvironment ENTITY = new IrisBufferEnvironment(
                new VanillaBufferEnvironment(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, ComputeShaderPrograms.CORE_ENTITY_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_ENTITY_VERTEX_TRANSFORM_KEY),
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                IrisVertexFormats.ENTITY,
                IrisPrograms.IRIS_ENTITY_MESH_UPLOADING_KEY,
                IrisPrograms.IRIS_ENTITY_VERTEX_TRANSFORM_KEY
        );
        public static final IBufferEnvironment POS = new VanillaBufferEnvironment(VertexFormats.POSITION, ComputeShaderPrograms.CORE_POS_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_POS_VERTEX_TRANSFORM_KEY);
        public static final IBufferEnvironment POS_TEX = new VanillaBufferEnvironment(VertexFormats.POSITION_TEXTURE, ComputeShaderPrograms.CORE_POS_TEX_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_POS_TEX_VERTEX_TRANSFORM_KEY);
        public static final IBufferEnvironment POS_TEX_COLOR = new VanillaBufferEnvironment(VertexFormats.POSITION_TEXTURE_COLOR, ComputeShaderPrograms.CORE_POS_TEX_COLOR_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_POS_TEX_COLOR_VERTEX_TRANSFORM_KEY);
        public static final IBufferEnvironment POS_COLOR_TEX_LIGHT = new IrisBufferEnvironment(
                new VanillaBufferEnvironment(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, ComputeShaderPrograms.CORE_POS_COLOR_TEX_LIGHT_MESH_UPLOADING_KEY, ComputeShaderPrograms.CORE_POS_COLOR_TEX_LIGHT_VERTEX_TRANSFORM_KEY),
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT,
                IrisVertexFormats.GLYPH,
                IrisPrograms.IRIS_GLYPH_MESH_UPLOADING_KEY,
                IrisPrograms.IRIS_GLYPH_VERTEX_TRANSFORM_KEY
        );
    }
}
