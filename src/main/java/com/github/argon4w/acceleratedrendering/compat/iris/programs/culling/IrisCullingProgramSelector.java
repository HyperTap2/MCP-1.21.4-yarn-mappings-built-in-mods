package com.github.argon4w.acceleratedrendering.compat.iris.programs.culling;

import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatFeature;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.culling.ICullingProgramSelector;
import com.github.argon4w.acceleratedrendering.core.utils.RenderTypeUtils;
import com.github.argon4w.acceleratedrendering.features.culling.OrientationCullingFeature;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.util.Identifier;

public class IrisCullingProgramSelector implements ICullingProgramSelector {

    private final ICullingProgramSelector parent;
    private final ICullingProgramDispatcher quadDispatcher;
    private final ICullingProgramDispatcher triangleDispatcher;

    public IrisCullingProgramSelector(
            ICullingProgramSelector parent,
            Identifier quadProgramKey,
            Identifier triangleProgramKey
    ) {
        this.parent = parent;
        this.quadDispatcher = new IrisCullingProgramDispatcher(VertexFormat.DrawMode.QUADS, quadProgramKey);
        this.triangleDispatcher = new IrisCullingProgramDispatcher(VertexFormat.DrawMode.TRIANGLES, triangleProgramKey);
    }

    @Override
    public ICullingProgramDispatcher select(RenderLayer renderType) {
        if (IrisCompatFeature.isEnabled()
                && IrisCompatFeature.isIrisCompatCullingEnabled()
                && (IrisCompatFeature.isShadowCullingEnabled() || !ShadowRenderingState.areShadowsCurrentlyBeingRendered())
                && OrientationCullingFeature.isEnabled()
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
