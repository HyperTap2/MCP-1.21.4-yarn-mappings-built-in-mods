package com.github.argon4w.acceleratedrendering.compat.iris.interfaces;

import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryInterface;

public interface IIrisAcceleratedBufferBuilder {

    IMemoryInterface getEntityIdOffset();

    IMemoryInterface getEntityOffset();
}

