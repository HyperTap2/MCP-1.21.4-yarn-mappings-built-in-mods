package com.github.argon4w.acceleratedrendering.core.programs.processing;

import net.minecraft.client.render.VertexFormat;

import java.util.function.UnaryOperator;

public class LoadPolygonProcessorEvent {

    private final VertexFormat vertexFormat;

    private IPolygonProcessor processor;

    public LoadPolygonProcessorEvent(VertexFormat vertexFormat) {
        this.vertexFormat = vertexFormat;
        this.processor = EmptyPolygonProcessor.INSTANCE;
    }

    public void loadFor(VertexFormat vertexFormat, UnaryOperator<IPolygonProcessor> selector) {
        if (this.vertexFormat == vertexFormat) {
            this.processor = selector.apply(this.processor);
        }
    }

    public IPolygonProcessor getProcessor() {
        return processor;
    }
}
