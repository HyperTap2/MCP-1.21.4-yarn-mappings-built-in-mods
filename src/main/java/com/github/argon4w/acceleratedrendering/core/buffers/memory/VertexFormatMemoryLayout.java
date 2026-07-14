package com.github.argon4w.acceleratedrendering.core.buffers.memory;

import lombok.EqualsAndHashCode;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VertexFormatMemoryLayout implements IMemoryLayout<VertexFormatElement> {

    private final VertexFormat vertexFormat;
    @EqualsAndHashCode.Include
    private final int hashCode;
    private final long size;
    private final IMemoryInterface[] byId;

    public VertexFormatMemoryLayout(VertexFormat vertexFormat) {
        var offsets = vertexFormat.getOffsetsByElementId();
        var count = offsets.length;

        this.vertexFormat = vertexFormat;
        this.hashCode = vertexFormat.hashCode();
        this.size = vertexFormat.getVertexSizeByte();
        this.byId = new IMemoryInterface[count];

        for (var i = 0; i < count; i++) {
            var offset = offsets[i];
            byId[i] = offset == -1 ? NullMemoryInterface.INSTANCE : new SimpleMemoryInterface(offset, size);
        }
    }

    @Override
    public IMemoryInterface getElement(VertexFormatElement element) {
        return byId[element.id()];
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return vertexFormat.toString();
    }
}
