package com.github.argon4w.acceleratedrendering.core.programs.culling;

import net.minecraft.client.render.RenderLayer;

public class PassThroughCullingProgramSelector implements ICullingProgramSelector {

    public static final ICullingProgramSelector INSTANCE = new PassThroughCullingProgramSelector();

    @Override
    public ICullingProgramDispatcher select(RenderLayer renderType) {
        return switch (renderType.getDrawMode()) {
            case QUADS -> PassThroughCullingProgramDispatcher.QUAD;
            case TRIANGLES -> PassThroughCullingProgramDispatcher.TRIANGLE;
            default -> throw new IllegalArgumentException("Unsupported mode: " + renderType.getDrawMode());
        };
    }
}
