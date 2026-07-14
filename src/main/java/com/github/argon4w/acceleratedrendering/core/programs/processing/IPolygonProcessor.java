package com.github.argon4w.acceleratedrendering.core.programs.processing;

import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import net.minecraft.client.render.VertexFormat;

public interface IPolygonProcessor {

    IPolygonProgramDispatcher select(VertexFormat.DrawMode mode);
}
