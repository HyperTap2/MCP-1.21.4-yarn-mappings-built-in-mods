package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisMeshInfoCache;
import net.irisshaders.iris.uniforms.CapturedRenderingState;

public class FlattenVarHandleMeshInfoCache implements IMeshInfoCache, IIrisMeshInfoCache {

    public static final VarHandle HANDLE = MethodHandles.arrayElementVarHandle(int[].class).withInvokeExactBehavior();
    public static final int MESH_INFO_SIZE = 8;
    public static final int COLOR_OFFSET = 0;
    public static final int LIGHT_OFFSET = 1;
    public static final int OVERLAY_OFFSET = 2;
    public static final int SHARING_OFFSET = 3;
    public static final int SHOULD_CULL_OFFSET = 4;
    public static final int RENDERED_ENTITY_OFFSET = 5;
    public static final int RENDERED_BLOCK_ENTITY_OFFSET = 6;
    public static final int RENDERED_ITEM_OFFSET = 7;

    private int[] cache;
    private int size;
    private int count;

    public FlattenVarHandleMeshInfoCache() {
        this.size = 128;
        this.cache = new int[this.size * MESH_INFO_SIZE];
        this.count = 0;
    }

    @Override
    public void reset() {
        count = 0;
    }

    @Override
    public void delete() {

    }

    @Override
    public void setup(
            int color,
            int light,
            int overlay,
            int sharing,
            int shouldCull
    ) {
        if (count >= size) {
            size = size * 2;
            cache = Arrays.copyOf(cache, size * MESH_INFO_SIZE);
        }

        var infoIndex = count * MESH_INFO_SIZE;

        HANDLE.set(cache, infoIndex + COLOR_OFFSET, color);
        HANDLE.set(cache, infoIndex + LIGHT_OFFSET, light);
        HANDLE.set(cache, infoIndex + OVERLAY_OFFSET, overlay);
        HANDLE.set(cache, infoIndex + SHARING_OFFSET, sharing);
        HANDLE.set(cache, infoIndex + SHOULD_CULL_OFFSET, shouldCull);
        HANDLE.set(cache, infoIndex + RENDERED_ENTITY_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
        HANDLE.set(cache, infoIndex + RENDERED_BLOCK_ENTITY_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
        HANDLE.set(cache, infoIndex + RENDERED_ITEM_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

        count++;
    }

    @Override
    public int getMeshCount() {
        return count;
    }

    @Override
    public int getSharing(int i) {
        return (int) HANDLE.get(cache, i * MESH_INFO_SIZE + SHARING_OFFSET);
    }

    @Override
    public int getShouldCull(int i) {
        return (int) HANDLE.get(cache, i * MESH_INFO_SIZE + SHOULD_CULL_OFFSET);
    }

    @Override
    public int getColor(int i) {
        return (int) HANDLE.get(cache, i * MESH_INFO_SIZE + COLOR_OFFSET);
    }

    @Override
    public int getLight(int i) {
        return (int) HANDLE.get(cache, i * MESH_INFO_SIZE + LIGHT_OFFSET);
    }

    @Override
    public int getOverlay(int i) {
        return (int) HANDLE.get(cache, i * MESH_INFO_SIZE + OVERLAY_OFFSET);
    }

    @Override
    public short getRenderedEntity(int i) {
        return (short)(int) HANDLE.get(cache, i * MESH_INFO_SIZE + RENDERED_ENTITY_OFFSET);
    }

    @Override
    public short getRenderedBlockEntity(int i) {
        return (short)(int) HANDLE.get(cache, i * MESH_INFO_SIZE + RENDERED_BLOCK_ENTITY_OFFSET);
    }

    @Override
    public short getRenderedItem(int i) {
        return (short)(int) HANDLE.get(cache, i * MESH_INFO_SIZE + RENDERED_ITEM_OFFSET);
    }
}
