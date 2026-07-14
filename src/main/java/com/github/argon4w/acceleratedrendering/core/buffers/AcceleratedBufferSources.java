package com.github.argon4w.acceleratedrendering.core.buffers;

import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.IAcceleratedBufferSource;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;

public class AcceleratedBufferSources implements Function<RenderLayer, VertexConsumer> {

    private final Map<VertexFormat, IAcceleratedBufferSource> sources;
    private final Set<VertexFormat.DrawMode> validModes;
    private final Set<String> invalidNames;
    private final boolean canSort;

    private AcceleratedBufferSources(
            Map<VertexFormat, IAcceleratedBufferSource> sources,
            Set<VertexFormat.DrawMode> validModes,
            Set<String> invalidNames,
            boolean canSort
    ) {
        this.sources = sources;
        this.validModes = validModes;
        this.invalidNames = invalidNames;
        this.canSort = canSort;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public VertexConsumer apply(RenderLayer pRenderType) {
        RenderLayer unwrapped = pRenderType instanceof WrappableRenderType wrapped ? wrapped.unwrap() : pRenderType;
        if (pRenderType != null
                && (CoreFeature.shouldForceAccelerateTranslucent() || canSort || !pRenderType.isTranslucent())
                && validModes.contains(pRenderType.getDrawMode())
                && !invalidNames.contains(unwrapped.getName())
                && sources.containsKey(pRenderType.getVertexFormat())
        ) {
            return sources
                    .get(pRenderType.getVertexFormat())
                    .getBuffer(pRenderType);
        }

        return null;
    }

    public static class Builder {

        private final Map<VertexFormat, IAcceleratedBufferSource> sources;
        private final Set<VertexFormat.DrawMode> validModes;
        private final Set<String> invalidNames;

        private boolean canSort;

        private Builder() {
            this.sources = new Object2ObjectOpenHashMap<>();
            this.validModes = new ReferenceOpenHashSet<>();
            this.invalidNames = new ObjectOpenHashSet<>();

            this.canSort = false;
        }

        public Builder source(IAcceleratedBufferSource bufferSource) {
            sources.putAll(Maps.asMap(
                    bufferSource
                            .getBufferEnvironment()
                            .getVertexFormats(),
                    $ -> bufferSource
            ));
            return this;
        }

        public Builder mode(VertexFormat.DrawMode mode) {
            validModes.add(mode);
            return this;
        }

        public Builder invalid(String name) {
            invalidNames.add(name);
            return this;
        }

        public Builder canSort() {
            canSort = true;
            return this;
        }

        public AcceleratedBufferSources build() {
            return new AcceleratedBufferSources(
                    sources,
                    validModes,
                    invalidNames,
                    canSort
            );
        }
    }
}
