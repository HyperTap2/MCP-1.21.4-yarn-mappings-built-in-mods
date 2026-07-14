package com.github.argon4w.acceleratedrendering.compat.iris.environments;

import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import com.github.argon4w.acceleratedrendering.core.AcceleratedRenderingRegistry;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.buffers.memory.VertexFormatMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramSelector;
import com.github.argon4w.acceleratedrendering.core.programs.culling.LoadCullingProgramSelectorEvent;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.MeshUploadingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.TransformProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.processing.IPolygonProcessor;
import com.github.argon4w.acceleratedrendering.core.programs.processing.LoadPolygonProcessorEvent;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.Identifier;

import java.util.Set;

public class IrisBufferEnvironment implements IBufferEnvironment {

    private final IBufferEnvironment vanillaSubSet;
    private final IBufferEnvironment irisSubSet;

    public IrisBufferEnvironment(
            IBufferEnvironment vanillaSubSet,
            VertexFormat vanillaVertexFormat,
            VertexFormat irisVertexFormat,
            Identifier meshUploadingProgramKey,
            Identifier transformProgramKey
    ) {
        this.vanillaSubSet = vanillaSubSet;
        this.irisSubSet = new IrisSubSet(
                vanillaVertexFormat,
                irisVertexFormat,
                meshUploadingProgramKey,
                transformProgramKey
        );
    }

    private IBufferEnvironment getSubSet() {
        return IrisApi.getInstance().isShaderPackInUse() && (ImmediateState.isRenderingLevel || CoreFeature.isRenderingHand())
                ? irisSubSet
                : vanillaSubSet;
    }

    @Override
    public void setupBufferState() {
        getSubSet().setupBufferState();
    }

    @Override
    public Set<VertexFormat> getVertexFormats() {
        return irisSubSet.getVertexFormats();
    }

    @Override
    public IMemoryLayout<VertexFormatElement> getLayout() {
        return getSubSet().getLayout();
    }

    @Override
    public MeshUploadingProgramDispatcher selectMeshUploadingProgramDispatcher() {
        return getSubSet().selectMeshUploadingProgramDispatcher();
    }

    @Override
    public TransformProgramDispatcher selectTransformProgramDispatcher() {
        return getSubSet().selectTransformProgramDispatcher();
    }

    @Override
    public ICullingProgramDispatcher selectCullingProgramDispatcher(RenderLayer renderType) {
        return getSubSet().selectCullingProgramDispatcher(renderType);
    }

    @Override
    public IPolygonProgramDispatcher selectProcessingProgramDispatcher(VertexFormat.DrawMode mode) {
        return getSubSet().selectProcessingProgramDispatcher(mode);
    }

    @Override
    public boolean isAccelerated(VertexFormat vertexFormat) {
        return getSubSet().isAccelerated(vertexFormat);
    }

    @Override
    public int getVertexSize() {
        return getSubSet().getVertexSize();
    }

    public static class IrisSubSet implements IBufferEnvironment {

        private final VertexFormat vanillaVertexFormat;
        private final VertexFormat irisVertexFormat;
        private final IMemoryLayout<VertexFormatElement> layout;

        private final MeshUploadingProgramDispatcher meshUploadingProgramDispatcher;
        private final TransformProgramDispatcher transformProgramDispatcher;
        private final ICullingProgramSelector cullingProgramSelector;
        private final IPolygonProcessor polygonProcessor;

        public IrisSubSet(
                VertexFormat vanillaVertexFormat,
                VertexFormat irisVertexFormat,
                Identifier meshUploadingProgramKey,
                Identifier transformProgramKey
        ) {
            this.vanillaVertexFormat = vanillaVertexFormat;
            this.irisVertexFormat = irisVertexFormat;
            this.layout = new VertexFormatMemoryLayout(irisVertexFormat);

            this.meshUploadingProgramDispatcher = new MeshUploadingProgramDispatcher(meshUploadingProgramKey);
            this.transformProgramDispatcher = new TransformProgramDispatcher(transformProgramKey);
            this.cullingProgramSelector = AcceleratedRenderingRegistry.createCullingSelector(this.irisVertexFormat);
            this.polygonProcessor = AcceleratedRenderingRegistry.createPolygonProcessor(this.irisVertexFormat);
        }

        @Override
        public void setupBufferState() {
            irisVertexFormat.setupState();
        }

        @Override
        public boolean isAccelerated(VertexFormat vertexFormat) {
            return this.vanillaVertexFormat == vertexFormat || this.irisVertexFormat == vertexFormat;
        }

        @Override
        public Set<VertexFormat> getVertexFormats() {
            return Set.of(vanillaVertexFormat, irisVertexFormat);
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
        public int getVertexSize() {
            return irisVertexFormat.getVertexSizeByte();
        }
    }
}
