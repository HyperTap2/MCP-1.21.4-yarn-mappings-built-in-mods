package com.github.argon4w.acceleratedrendering.compat.iris.programs.culling;

import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.programs.ReloadableComputeProgram;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.features.culling.OrientationCullingFeature;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public class IrisCullingProgramDispatcher implements ICullingProgramDispatcher {

    private static final int GROUP_SIZE = 128;
    private static final int DISPATCH_COUNT_Y_Z = 1;

    private final VertexFormat.DrawMode mode;
    private final ReloadableComputeProgram program;

    public IrisCullingProgramDispatcher(VertexFormat.DrawMode mode, Identifier key) {
        this.mode = mode;
        this.program = new ReloadableComputeProgram(key);
    }

    @Override
    public int dispatch(AcceleratedBufferBuilder builder) {
        var computeProgram = this.program.get();
        var viewMatrixUniform = this.program.uniform("viewMatrix");
        var projectMatrixUniform = this.program.uniform("projectMatrix");
        var polygonCountUniform = this.program.uniform("polygonCount");
        var vertexOffsetUniform = this.program.uniform("vertexOffset");
        var varyingOffsetUniform = this.program.uniform("varyingOffset");
        var shadowState = ShadowRenderingState.areShadowsCurrentlyBeingRendered();
        var vertexCount = builder.getTotalVertexCount();
        var polygonCount = vertexCount / mode.firstVertexCount;

        viewMatrixUniform.uploadMatrix4f(shadowState ? ShadowRenderer.MODELVIEW : RenderSystem.getModelViewMatrix());
        projectMatrixUniform.uploadMatrix4f(shadowState ? ShadowRenderer.PROJECTION : RenderSystem.getProjectionMatrix());

        polygonCountUniform.uploadUnsignedInt(polygonCount);
        vertexOffsetUniform.uploadUnsignedInt((int) (builder.getVertexBuffer().getOffset() / builder.getVertexSize()));
        varyingOffsetUniform.uploadUnsignedInt((int) (builder.getVaryingBuffer().getOffset() / AcceleratedBufferBuilder.VARYING_SIZE));

        computeProgram.useProgram();
        computeProgram.dispatch(
                (polygonCount + GROUP_SIZE - 1) / GROUP_SIZE,
                DISPATCH_COUNT_Y_Z,
                DISPATCH_COUNT_Y_Z
        );
        computeProgram.resetProgram();

        return computeProgram.getBarrierFlags();
    }

    @Override
    public boolean shouldCull() {
        return OrientationCullingFeature.shouldCull()
                && (IrisCompatFeature.isShadowCullingEnabled() || !ShadowRenderingState.areShadowsCurrentlyBeingRendered());
    }
}
