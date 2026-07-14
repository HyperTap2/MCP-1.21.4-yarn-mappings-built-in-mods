package com.github.argon4w.acceleratedrendering.core.buffers.environments;

import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.AcceleratedRenderingRegistry;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.VertexFormatMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramSelector;
import com.github.argon4w.acceleratedrendering.core.programs.culling.LoadCullingProgramSelectorEvent;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.MeshUploadingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.TransformProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.processing.IPolygonProcessor;
import com.github.argon4w.acceleratedrendering.core.programs.processing.LoadPolygonProcessorEvent;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.Identifier;

import java.util.Set;

public class VanillaBufferEnvironment implements IBufferEnvironment {

    private final VertexFormat vertexFormat;
    private final IMemoryLayout<VertexFormatElement> layout;

    private final MeshUploadingProgramDispatcher meshUploadingProgramDispatcher;
    private final TransformProgramDispatcher transformProgramDispatcher;
    private final ICullingProgramSelector cullingProgramSelector;
    private final IPolygonProcessor polygonProcessor;

    public VanillaBufferEnvironment(
            VertexFormat vertexFormat,
            Identifier meshUploadingProgramKey,
            Identifier transformProgramKey
    ) {
        this.vertexFormat = vertexFormat;
        this.layout = new VertexFormatMemoryLayout(vertexFormat);

        this.meshUploadingProgramDispatcher = new MeshUploadingProgramDispatcher(meshUploadingProgramKey);
        this.transformProgramDispatcher = new TransformProgramDispatcher(transformProgramKey);
        this.cullingProgramSelector = AcceleratedRenderingRegistry.createCullingSelector(this.vertexFormat);
        this.polygonProcessor = AcceleratedRenderingRegistry.createPolygonProcessor(this.vertexFormat);
    }

    @Override
    public void setupBufferState() {
        vertexFormat.setupState();
    }

    @Override
    public Set<VertexFormat> getVertexFormats() {
        return Set.of(vertexFormat);
    }

    @Override
    public IMemoryLayout<VertexFormatElement> getLayout() {
        return layout;
    }

    @Override
    public MeshUploadingProgramDispatcher selectMeshUploadingProgramDispatcher() {
        return meshUploadingProgramDispatcher;
    }

    @Override
    public TransformProgramDispatcher selectTransformProgramDispatcher() {
        return transformProgramDispatcher;
    }

    @Override
    public ICullingProgramDispatcher selectCullingProgramDispatcher(RenderLayer renderType) {
        return cullingProgramSelector.select(renderType);
    }

    @Override
    public IPolygonProgramDispatcher selectProcessingProgramDispatcher(VertexFormat.DrawMode mode) {
        return polygonProcessor.select(mode);
    }

    @Override
    public boolean isAccelerated(VertexFormat vertexFormat) {
        return this.vertexFormat == vertexFormat;
    }

    @Override
    public int getVertexSize() {
        return vertexFormat.getVertexSizeByte();
    }
}
