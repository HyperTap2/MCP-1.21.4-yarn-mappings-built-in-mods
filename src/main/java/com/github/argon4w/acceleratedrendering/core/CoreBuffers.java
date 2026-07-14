package com.github.argon4w.acceleratedrendering.core;

import com.github.argon4w.acceleratedrendering.core.buffers.AcceleratedBufferSources;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSource;
import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import com.github.argon4w.acceleratedrendering.core.meshes.ClientMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatBuffers;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.render.VertexFormat;

public class CoreBuffers {

    private static boolean deleted;

    public static final AcceleratedBufferSource BLOCK = new AcceleratedBufferSource(IBufferEnvironment.Presets.BLOCK);
    public static final AcceleratedBufferSource ENTITY = new AcceleratedBufferSource(IBufferEnvironment.Presets.ENTITY);
    public static final AcceleratedBufferSource POS = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS);
    public static final AcceleratedBufferSource POS_TEX = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX);
    public static final AcceleratedBufferSource POS_TEX_COLOR = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX_COLOR);
    public static final AcceleratedBufferSource POS_COLOR_TEX_LIGHT = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_COLOR_TEX_LIGHT);
    public static final AcceleratedBufferSource POS_TEX_COLOR_OUTLINE = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX_COLOR);

    public static final AcceleratedBufferSources CORE = AcceleratedBufferSources
            .builder()
            .source(BLOCK)
            .source(ENTITY)
            .source(POS)
            .source(POS_TEX)
            .source(POS_TEX_COLOR)
            .source(POS_COLOR_TEX_LIGHT)
            .mode(VertexFormat.DrawMode.QUADS)
            .mode(VertexFormat.DrawMode.TRIANGLES)
            .invalid("breeze_wind")
            .invalid("energy_swirl")
            .build();

    public static final AcceleratedBufferSources OUTLINE = AcceleratedBufferSources
            .builder()
            .source(POS_TEX_COLOR_OUTLINE)
            .mode(VertexFormat.DrawMode.QUADS)
            .mode(VertexFormat.DrawMode.TRIANGLES)
            .invalid("breeze_wind")
            .invalid("energy_swirl")
            .build();

    public static AcceleratedBufferSources getCoreBufferSourceSet() {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return IrisCompatBuffers.SHADOW;
        }
        if (CoreFeature.isRenderingHand() && com.github.argon4w.acceleratedrendering.AcceleratedRendering.isIrisPipelineActive()) {
            return IrisCompatBuffers.HAND;
        }
        return CORE;
    }

    public static void drawBuffers() {
        ENTITY.drawBuffers();
        BLOCK.drawBuffers();
        POS.drawBuffers();
        POS_TEX.drawBuffers();
        POS_TEX_COLOR.drawBuffers();
        POS_COLOR_TEX_LIGHT.drawBuffers();
    }

    public static void drawOutlineBuffers() {
        POS_TEX_COLOR_OUTLINE.drawBuffers();
    }

    public static synchronized void deleteBuffers() {
        if (deleted) {
            return;
        }

        deleted = true;
        ENTITY.delete();
        BLOCK.delete();
        POS.delete();
        POS_TEX.delete();
        POS_TEX_COLOR.delete();
        POS_COLOR_TEX_LIGHT.delete();
        POS_TEX_COLOR_OUTLINE.delete();
        ServerMesh.Builder.INSTANCE.delete();
        ClientMesh.Builder.INSTANCE.delete();
    }
}
