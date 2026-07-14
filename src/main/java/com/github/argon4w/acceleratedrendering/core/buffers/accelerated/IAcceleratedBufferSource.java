package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import net.minecraft.client.render.VertexConsumerProvider;

public interface IAcceleratedBufferSource extends VertexConsumerProvider {

    IBufferEnvironment getBufferEnvironment();

    void drawBuffers();
}
