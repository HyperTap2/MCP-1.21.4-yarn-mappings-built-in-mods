package com.github.argon4w.acceleratedrendering.core.programs.culling;

import net.minecraft.client.render.RenderLayer;

public interface ICullingProgramSelector {

    ICullingProgramDispatcher select(RenderLayer renderType);
}
