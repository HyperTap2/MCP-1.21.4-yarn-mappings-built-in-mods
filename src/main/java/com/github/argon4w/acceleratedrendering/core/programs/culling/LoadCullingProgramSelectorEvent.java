package com.github.argon4w.acceleratedrendering.core.programs.culling;

import net.minecraft.client.render.VertexFormat;

import java.util.function.UnaryOperator;

public class LoadCullingProgramSelectorEvent {

    private final VertexFormat vertexFormat;

    private ICullingProgramSelector selector;

    public LoadCullingProgramSelectorEvent(VertexFormat vertexFormat) {
        this.vertexFormat = vertexFormat;
        this.selector = PassThroughCullingProgramSelector.INSTANCE;
    }

    public void loadFor(VertexFormat vertexFormat, UnaryOperator<ICullingProgramSelector> selector) {
        if (this.vertexFormat == vertexFormat) {
            this.selector = selector.apply(this.selector);
        }
    }

    public ICullingProgramSelector getSelector() {
        return selector;
    }
}
