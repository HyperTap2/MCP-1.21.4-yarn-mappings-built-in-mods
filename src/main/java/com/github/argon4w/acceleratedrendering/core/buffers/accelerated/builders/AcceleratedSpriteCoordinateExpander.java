package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;

@ExtensionMethod(VertexConsumerExtension.class)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AcceleratedSpriteCoordinateExpander extends AcceleratedVertexConsumerWrapper {

    private final VertexConsumer delegate;
    private final Sprite sprite;

    @Override
    public VertexConsumer getDelegate() {
        return delegate;
    }

    @Override
    public VertexConsumer decorate(VertexConsumer buffer) {
        return new AcceleratedSpriteCoordinateExpander(
                getDelegate()
                        .getAccelerated()
                        .decorate(buffer),
                sprite
        );
    }

    @Override
    public VertexConsumer texture(float pU, float pV) {
        delegate.texture(
                sprite.getFrameU(pU),
                sprite.getFrameV(pV)
        );
        return this;
    }

    @Override
    public void vertex(
            float pX,
            float pY,
            float pZ,
            int pColor,
            float pU,
            float pV,
            int pPackedOverlay,
            int pPackedLight,
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        delegate.vertex(
                pX,
                pY,
                pZ,
                pColor,
                sprite.getFrameU(pU),
                sprite.getFrameV(pV),
                pPackedOverlay,
                pPackedLight,
                pNormalX,
                pNormalY,
                pNormalZ
        );
    }
}
