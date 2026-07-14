package com.github.argon4w.acceleratedrendering.core.backends.buffers;

public interface IClientBuffer {

    long reserve(long bytes);

    long reserve(long bytes, boolean occupied);

    long addressAt(long position);
}
