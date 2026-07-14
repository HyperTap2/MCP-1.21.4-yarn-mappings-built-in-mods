package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAccelerationHolder;
import net.minecraft.client.render.VertexConsumer;

public class VertexConsumerExtension {

    public static IAcceleratedVertexConsumer getAccelerated(VertexConsumer in) {
        return (IAcceleratedVertexConsumer) in;
    }

    public static IAccelerationHolder getHolder(VertexConsumer in) {
        return (IAccelerationHolder) in;
    }
}
