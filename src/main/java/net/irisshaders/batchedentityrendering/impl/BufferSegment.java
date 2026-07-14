package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public record BufferSegment(BuiltBuffer meshData, RenderLayer type) {
}
