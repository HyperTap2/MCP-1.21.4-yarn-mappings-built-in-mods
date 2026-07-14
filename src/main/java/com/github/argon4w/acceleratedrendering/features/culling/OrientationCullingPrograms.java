package com.github.argon4w.acceleratedrendering.features.culling;

import com.github.argon4w.acceleratedrendering.core.backends.programs.BarrierFlags;
import com.github.argon4w.acceleratedrendering.core.programs.LoadComputeShaderEvent;
import com.github.argon4w.acceleratedrendering.core.programs.culling.LoadCullingProgramSelectorEvent;
import com.github.argon4w.acceleratedrendering.core.utils.ResourceLocationUtils;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
public class OrientationCullingPrograms {

    public static final Identifier CORE_ENTITY_QUAD_CULLING_KEY = ResourceLocationUtils.create("core_entity_quad_culling");
    public static final Identifier CORE_ENTITY_TRIANGLE_CULLING_KEY = ResourceLocationUtils.create("core_entity_triangle_culling");
    public static final Identifier CORE_BLOCK_QUAD_CULLING_KEY = ResourceLocationUtils.create("core_block_quad_culling");
    public static final Identifier CORE_BLOCK_TRIANGLE_CULLING_KEY = ResourceLocationUtils.create("core_block_triangle_culling");
    public static final Identifier CORE_POS_TEX_COLOR_QUAD_CULLING_KEY = ResourceLocationUtils.create("core_pos_tex_color_quad_culling");
    public static final Identifier CORE_POS_TEX_COLOR_TRIANGLE_CULLING_KEY = ResourceLocationUtils.create("core_pos_tex_color_triangle_culling");
    public static final Identifier CORE_POS_TEX_QUAD_CULLING_KEY = ResourceLocationUtils.create("core_pos_tex_quad_culling");
    public static final Identifier CORE_POS_TEX_TRIANGLE_CULLING_KEY = ResourceLocationUtils.create("core_pos_tex_triangle_culling");

    public static void onLoadComputeShaders(LoadComputeShaderEvent event) {
        event.loadComputeShader(
                CORE_ENTITY_QUAD_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/entity_quad_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_ENTITY_TRIANGLE_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/entity_triangle_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_BLOCK_QUAD_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/block_quad_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_BLOCK_TRIANGLE_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/block_triangle_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_POS_TEX_COLOR_QUAD_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/pos_tex_color_quad_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_POS_TEX_COLOR_TRIANGLE_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/pos_tex_color_triangle_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_POS_TEX_QUAD_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/pos_tex_quad_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );

        event.loadComputeShader(
                CORE_POS_TEX_TRIANGLE_CULLING_KEY,
                ResourceLocationUtils.create("shaders/core/culling/pos_tex_triangle_culling_shader.compute"),
                BarrierFlags.SHADER_STORAGE,
                BarrierFlags.ATOMIC_COUNTER
        );
    }

    public static void onLoadCullingPrograms(LoadCullingProgramSelectorEvent event) {
        event.loadFor(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, parent -> new OrientationCullingProgramSelector(
                parent,
                CORE_ENTITY_QUAD_CULLING_KEY,
                CORE_ENTITY_TRIANGLE_CULLING_KEY
        ));

        event.loadFor(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, parent -> new OrientationCullingProgramSelector(
                parent,
                CORE_BLOCK_QUAD_CULLING_KEY,
                CORE_BLOCK_TRIANGLE_CULLING_KEY
        ));

        event.loadFor(VertexFormats.POSITION_TEXTURE_COLOR, parent -> new OrientationCullingProgramSelector(
                parent,
                CORE_POS_TEX_COLOR_QUAD_CULLING_KEY,
                CORE_POS_TEX_COLOR_TRIANGLE_CULLING_KEY
        ));

        event.loadFor(VertexFormats.POSITION_TEXTURE, parent -> new OrientationCullingProgramSelector(
                parent,
                CORE_POS_TEX_QUAD_CULLING_KEY,
                CORE_POS_TEX_TRIANGLE_CULLING_KEY
        ));
    }
}
