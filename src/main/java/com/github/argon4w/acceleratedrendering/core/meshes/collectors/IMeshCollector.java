package com.github.argon4w.acceleratedrendering.core.meshes.collectors;

import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.util.BufferAllocator;

public interface IMeshCollector {

    BufferAllocator getBuffer();

    IMemoryLayout<VertexFormatElement> getLayout();

    int getVertexCount();
}
