package com.github.argon4w.acceleratedrendering.core.programs.culling;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;

public interface ICullingProgramDispatcher {

    int dispatch(AcceleratedBufferBuilder builder);

    boolean shouldCull();
}
