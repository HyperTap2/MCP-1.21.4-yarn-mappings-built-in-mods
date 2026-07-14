package com.github.argon4w.acceleratedrendering.core.utils;

import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

public class RenderTypeUtils {

    public static Identifier getTextureLocation(RenderLayer renderType) {
        if (renderType instanceof WrappableRenderType wrapped) {
            renderType = wrapped.unwrap();
        }
        if (renderType == null) {
            return null;
        }

        if (!(renderType instanceof RenderLayer.MultiPhase composite)) {
            return null;
        }

        return composite.acceleratedRendering$getTexture().orElse(null);
    }

    public static boolean isCulled(RenderLayer renderType) {
        if (renderType instanceof WrappableRenderType wrapped) {
            renderType = wrapped.unwrap();
        }
        if (renderType == null) {
            return false;
        }

        if (!(renderType instanceof RenderLayer.MultiPhase composite)) {
            return false;
        }

        return composite.acceleratedRendering$isCullEnabled();
    }
}
