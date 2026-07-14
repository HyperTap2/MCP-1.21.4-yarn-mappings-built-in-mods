package com.github.argon4w.acceleratedrendering.features.culling;

import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramSelector;
import com.github.argon4w.acceleratedrendering.core.utils.RenderTypeUtils;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public class OrientationCullingProgramSelector implements ICullingProgramSelector {

    private final ICullingProgramSelector parent;
    private final ICullingProgramDispatcher quadDispatcher;
    private final ICullingProgramDispatcher triangleDispatcher;

    public OrientationCullingProgramSelector(
            ICullingProgramSelector parent,
            Identifier quadProgramKey,
            Identifier triangleProgramKey
    ) {
        this.parent = parent;
        this.quadDispatcher = new OrientationCullingProgramDispatcher(VertexFormat.DrawMode.QUADS, quadProgramKey);
        this.triangleDispatcher = new OrientationCullingProgramDispatcher(VertexFormat.DrawMode.TRIANGLES, triangleProgramKey);
    }

    @Override
    public ICullingProgramDispatcher select(RenderLayer renderType) {
        if (OrientationCullingFeature.isEnabled()
                && (OrientationCullingFeature.shouldIgnoreCullState() || RenderTypeUtils.isCulled(renderType))) {
            return switch (renderType.getDrawMode()) {
                case QUADS -> quadDispatcher;
                case TRIANGLES -> triangleDispatcher;
                default -> parent.select(renderType);
            };
        }

        return parent.select(renderType);
    }
}
