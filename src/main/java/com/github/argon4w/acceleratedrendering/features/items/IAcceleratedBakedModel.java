package com.github.argon4w.acceleratedrendering.features.items;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

public interface IAcceleratedBakedModel {

    default void renderItemFast(
            AcceleratedItemRenderContext context,
            MatrixStack.Entry pose,
            IAcceleratedVertexConsumer extension,
            int combinedLight,
            int combinedOverlay
    ) {
        throw new UnsupportedOperationException("This model does not provide a specialized accelerated renderer");
    }

    default int getCustomColor(int layer, int color) {
        return color;
    }

    default boolean isAccelerated() {
        return false;
    }

    default boolean isAcceleratedInHand() {
        return false;
    }

    default boolean isAcceleratedInGui() {
        return false;
    }
}
