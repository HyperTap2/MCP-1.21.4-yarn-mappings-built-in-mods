package com.github.argon4w.acceleratedrendering.features.items;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;

public class BakedModelExtension {

    public static IAcceleratedBakedModel getAccelerated(BakedModel in) {
        return (IAcceleratedBakedModel) in;
    }

    public static IAcceleratedBakedQuad getAccelerated(BakedQuad in) {
        return (IAcceleratedBakedQuad) in;
    }
}

