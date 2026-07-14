package com.github.argon4w.acceleratedrendering.core.programs.dispatchers;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.programs.ReloadableComputeProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public class FixedPolygonProgramDispatcher implements IPolygonProgramDispatcher {

    private static final int GROUP_SIZE = 128;
    private static final int DISPATCH_COUNT_Y_Z = 1;

    private final VertexFormat.DrawMode mode;
    private final ReloadableComputeProgram program;

    public FixedPolygonProgramDispatcher(VertexFormat.DrawMode mode, Identifier key) {
        this.mode = mode;
        this.program = new ReloadableComputeProgram(key);
    }

    @Override
    public int dispatch(AcceleratedBufferBuilder builder) {
        var computeProgram = this.program.get();
        var polygonCountUniform = this.program.uniform("polygonCount");
        var vertexOffsetUniform = this.program.uniform("vertexOffset");
        var vertexCount = builder.getTotalVertexCount();
        var polygonCount = vertexCount / mode.firstVertexCount;

        polygonCountUniform.uploadUnsignedInt(polygonCount);
        vertexOffsetUniform.uploadUnsignedInt((int) (builder.getVertexBuffer().getOffset() / builder.getVertexSize()));

        computeProgram.useProgram();
        computeProgram.dispatch(
                (polygonCount + GROUP_SIZE - 1) / GROUP_SIZE,
                DISPATCH_COUNT_Y_Z,
                DISPATCH_COUNT_Y_Z
        );

        return computeProgram.getBarrierFlags();
    }
}
