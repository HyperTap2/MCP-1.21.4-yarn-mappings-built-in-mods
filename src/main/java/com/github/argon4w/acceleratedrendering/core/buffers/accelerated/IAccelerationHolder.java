package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

public interface IAccelerationHolder {

    default VertexConsumer initAcceleration(RenderLayer renderType) {
        return (VertexConsumer) this;
    }
}
