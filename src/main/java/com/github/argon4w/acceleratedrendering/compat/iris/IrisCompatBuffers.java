package com.github.argon4w.acceleratedrendering.compat.iris;

import com.github.argon4w.acceleratedrendering.core.buffers.AcceleratedBufferSources;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSource;
import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import net.minecraft.client.render.VertexFormat;

public class IrisCompatBuffers {

    private static boolean deleted;

    public static final AcceleratedBufferSource BLOCK_SHADOW = new AcceleratedBufferSource(IBufferEnvironment.Presets.BLOCK);
    public static final AcceleratedBufferSource ENTITY_SHADOW = new AcceleratedBufferSource(IBufferEnvironment.Presets.ENTITY);
    public static final AcceleratedBufferSource GLYPH_SHADOW = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_COLOR_TEX_LIGHT);
    public static final AcceleratedBufferSource POS_TEX_SHADOW = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX);
    public static final AcceleratedBufferSource POS_TEX_COLOR_SHADOW = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX_COLOR);

    public static final AcceleratedBufferSource BLOCK_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.BLOCK);
    public static final AcceleratedBufferSource ENTITY_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.ENTITY);
    public static final AcceleratedBufferSource POS_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS);
    public static final AcceleratedBufferSource POS_TEX_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX);
    public static final AcceleratedBufferSource POS_TEX_COLOR_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_TEX_COLOR);
    public static final AcceleratedBufferSource POS_COLOR_TEX_LIGHT_HAND = new AcceleratedBufferSource(IBufferEnvironment.Presets.POS_COLOR_TEX_LIGHT);

    public static final AcceleratedBufferSources SHADOW = AcceleratedBufferSources
            .builder()
            .source(IrisCompatBuffers.BLOCK_SHADOW)
            .source(IrisCompatBuffers.ENTITY_SHADOW)
            .source(IrisCompatBuffers.GLYPH_SHADOW)
            .source(IrisCompatBuffers.POS_TEX_SHADOW)
            .source(IrisCompatBuffers.POS_TEX_COLOR_SHADOW)
            .mode(VertexFormat.DrawMode.QUADS)
            .mode(VertexFormat.DrawMode.TRIANGLES)
            .invalid("breeze_wind")
            .invalid("energy_swirl")
            .build();

    public static final AcceleratedBufferSources HAND = AcceleratedBufferSources
            .builder()
            .source(BLOCK_HAND)
            .source(ENTITY_HAND)
            .source(POS_HAND)
            .source(POS_TEX_HAND)
            .source(POS_TEX_COLOR_HAND)
            .source(POS_COLOR_TEX_LIGHT_HAND)
            .mode(VertexFormat.DrawMode.QUADS)
            .mode(VertexFormat.DrawMode.TRIANGLES)
            .invalid("breeze_wind")
            .invalid("energy_swirl")
            .build();

    public static void drawBuffers() {
        BLOCK_SHADOW.drawBuffers();
        ENTITY_SHADOW.drawBuffers();
        GLYPH_SHADOW.drawBuffers();
        POS_TEX_SHADOW.drawBuffers();
        POS_TEX_COLOR_SHADOW.drawBuffers();
    }

    public static void drawHandBuffers() {
        BLOCK_HAND.drawBuffers();
        ENTITY_HAND.drawBuffers();
        POS_HAND.drawBuffers();
        POS_TEX_HAND.drawBuffers();
        POS_TEX_COLOR_HAND.drawBuffers();
        POS_COLOR_TEX_LIGHT_HAND.drawBuffers();
    }

    public static synchronized void deleteBuffers() {
        if (deleted) {
            return;
        }

        deleted = true;
        BLOCK_SHADOW.delete();
        ENTITY_SHADOW.delete();
        GLYPH_SHADOW.delete();
        POS_TEX_SHADOW.delete();
        POS_TEX_COLOR_SHADOW.delete();
        BLOCK_HAND.delete();
        ENTITY_HAND.delete();
        POS_HAND.delete();
        POS_TEX_HAND.delete();
        POS_TEX_COLOR_HAND.delete();
        POS_COLOR_TEX_LIGHT_HAND.delete();
    }
}
