package com.github.argon4w.acceleratedrendering.compat.iris.programs.processing;

import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatFeature;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.FixedPolygonProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.processing.IPolygonProcessor;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public class IrisPolygonProcessor implements IPolygonProcessor {

    private final IPolygonProcessor parent;
    private final IPolygonProgramDispatcher quadDispatcher;
    private final IPolygonProgramDispatcher triangleDispatcher;

    public IrisPolygonProcessor(
            IPolygonProcessor parent,
            Identifier quadProgramKey,
            Identifier triangleProgramKey
    ) {
        this.parent = parent;
        this.quadDispatcher = new FixedPolygonProgramDispatcher(VertexFormat.DrawMode.QUADS, quadProgramKey);
        this.triangleDispatcher = new FixedPolygonProgramDispatcher(VertexFormat.DrawMode.TRIANGLES, triangleProgramKey);
    }

    @Override
    public IPolygonProgramDispatcher select(VertexFormat.DrawMode mode) {
        if (IrisCompatFeature.isEnabled()
                && IrisCompatFeature.isPolygonProcessingEnabled()) {
            return switch (mode) {
                case QUADS -> quadDispatcher;
                case TRIANGLES -> triangleDispatcher;
                default -> parent.select(mode);
            };
        }

        return parent.select(mode);
    }
}
