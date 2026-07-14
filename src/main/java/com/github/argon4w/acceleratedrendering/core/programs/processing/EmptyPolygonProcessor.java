package com.github.argon4w.acceleratedrendering.core.programs.processing;

import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.EmptyProgramDispatcher;
import com.github.argon4w.acceleratedrendering.core.programs.dispatchers.IPolygonProgramDispatcher;
import net.minecraft.client.render.VertexFormat;

public class EmptyPolygonProcessor implements IPolygonProcessor {

    public static final EmptyPolygonProcessor INSTANCE = new EmptyPolygonProcessor();

    @Override
    public IPolygonProgramDispatcher select(VertexFormat.DrawMode mode) {
        return EmptyProgramDispatcher.INSTANCE;
    }
}
