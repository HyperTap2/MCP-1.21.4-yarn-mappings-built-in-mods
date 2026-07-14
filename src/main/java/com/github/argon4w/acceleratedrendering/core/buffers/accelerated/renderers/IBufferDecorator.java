package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers;

import net.minecraft.client.render.VertexConsumer;

public interface IBufferDecorator {

    VertexConsumer decorate(VertexConsumer buffer);
}
