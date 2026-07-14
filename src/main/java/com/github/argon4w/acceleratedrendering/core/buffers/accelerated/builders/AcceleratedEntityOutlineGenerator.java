package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

@ExtensionMethod(VertexConsumerExtension.class)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AcceleratedEntityOutlineGenerator extends AcceleratedVertexConsumerWrapper {

    private final VertexConsumer delegate;
    private final int color;

    @Override
    public VertexConsumer getDelegate() {
        return delegate;
    }

    @Override
    public VertexConsumer decorate(VertexConsumer buffer) {
        return new AcceleratedEntityOutlineGenerator(
                getDelegate()
                        .getAccelerated()
                        .decorate(buffer),
                color
        );
    }

    @Override
    public VertexConsumer vertex(
            float pX,
            float pY,
            float pZ
    ) {
        delegate.vertex(
                pX,
                pY,
                pZ
        ).color(color);
        return this;
    }

    @Override
    public VertexConsumer vertex(
            MatrixStack.Entry pPose,
            float pX,
            float pY,
            float pZ
    ) {
        delegate.vertex(
                pPose,
                pX,
                pY,
                pZ
        ).color(color);
        return this;
    }

    @Override
    public VertexConsumer color(
            int pRed,
            int pGreen,
            int pBlue,
            int pAlpha
    ) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int pU, int pV) {
        return this;
    }

    @Override
    public VertexConsumer light(int pU, int pV) {
        return this;
    }

    @Override
    public VertexConsumer normal(
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        return this;
    }

    @Override
    public VertexConsumer normal(
            MatrixStack.Entry pPose,
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        return this;
    }
}
