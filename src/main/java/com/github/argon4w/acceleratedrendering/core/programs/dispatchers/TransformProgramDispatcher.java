package com.github.argon4w.acceleratedrendering.core.programs.dispatchers;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.StagingBufferPool;
import com.github.argon4w.acceleratedrendering.core.programs.ReloadableComputeProgram;
import java.util.Collection;
import net.minecraft.util.Identifier;

import static org.lwjgl.opengl.GL46.GL_SHADER_STORAGE_BUFFER;

public class TransformProgramDispatcher {

    public static final int VERTEX_BUFFER_IN_INDEX = 0;
    public static final int VARYING_BUFFER_INDEX = 3;
    private static final int GROUP_SIZE = 128;
    private static final int DISPATCH_COUNT_Y_Z = 1;

    private final ReloadableComputeProgram program;

    public TransformProgramDispatcher(Identifier key) {
        this.program = new ReloadableComputeProgram(key);
    }

    public void dispatch(Collection<AcceleratedBufferBuilder> builders) {
        var computeProgram = this.program.get();
        var vertexCountUniform = this.program.uniform("vertexCount");
        var vertexOffsetUniform = this.program.uniform("vertexOffset");
        var varyingOffsetUniform = this.program.uniform("varyingOffset");
        computeProgram.useProgram();

        for (var builder : builders) {
            var vertexCount = builder.getVertexCount();
            var vertexBuffer = builder.getVertexBuffer();
            var varyingBuffer = builder.getVaryingBuffer();

            if (vertexCount != 0) {

                vertexBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, VERTEX_BUFFER_IN_INDEX);
                varyingBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, VARYING_BUFFER_INDEX);

                vertexCountUniform.uploadUnsignedInt(vertexCount);
                vertexOffsetUniform.uploadUnsignedInt((int) (vertexBuffer.getOffset() / builder.getVertexSize()));
                varyingOffsetUniform.uploadUnsignedInt((int) (varyingBuffer.getOffset() / AcceleratedBufferBuilder.VARYING_SIZE));

                computeProgram.dispatch(
                        (vertexCount + GROUP_SIZE - 1) / GROUP_SIZE,
                        DISPATCH_COUNT_Y_Z,
                        DISPATCH_COUNT_Y_Z
                );
            }
        }

        computeProgram.resetProgram();
        computeProgram.waitBarriers();
    }

    public void dispatch(
            StagingBufferPool.StagingBuffer vertexBuffer,
            StagingBufferPool.StagingBuffer varyingBuffer,
            long vertexCount,
            long vertexOffset,
            long varyingOffset
    ) {
        var computeProgram = this.program.get();
        var vertexCountUniform = this.program.uniform("vertexCount");
        var vertexOffsetUniform = this.program.uniform("vertexOffset");
        var varyingOffsetUniform = this.program.uniform("varyingOffset");
        vertexBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, VERTEX_BUFFER_IN_INDEX);
        varyingBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, VARYING_BUFFER_INDEX);

        vertexCountUniform.uploadUnsignedInt((int) vertexCount);
        vertexOffsetUniform.uploadUnsignedInt((int) vertexOffset);
        varyingOffsetUniform.uploadUnsignedInt((int) varyingOffset);

        computeProgram.useProgram();
        computeProgram.dispatch(
                (int) (vertexCount + GROUP_SIZE - 1) / GROUP_SIZE,
                DISPATCH_COUNT_Y_Z,
                DISPATCH_COUNT_Y_Z
        );
    }

    public int getBarrierFlags() {
        return this.program.get().getBarrierFlags();
    }
}
