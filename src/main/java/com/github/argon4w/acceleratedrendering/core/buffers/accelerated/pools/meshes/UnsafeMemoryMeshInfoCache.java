package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisMeshInfoCache;
import net.irisshaders.iris.uniforms.CapturedRenderingState;

public class UnsafeMemoryMeshInfoCache implements IMeshInfoCache, IIrisMeshInfoCache {

    public static final Unsafe UNSAFE = UnsafeAccess.UNSAFE;
    public static final long MESH_INFO_SIZE = 8L * 4L;
    public static final long COLOR_OFFSET = 0L;
    public static final long LIGHT_OFFSET = 4L;
    public static final long OVERLAY_OFFSET = 2L * 4L;
    public static final long SHARING_OFFSET = 3L * 4L;
    public static final long SHOULD_CULL_OFFSET = 4L * 4L;
    public static final long RENDERED_ENTITY_OFFSET = 5L * 4L;
    public static final long RENDERED_BLOCK_ENTITY_OFFSET = 6L * 4L;
    public static final long RENDERED_ITEM_OFFSET = 7L * 4L;

    private long address;
    private int size;
    private int count;

    public UnsafeMemoryMeshInfoCache() {
        this.size = 128;
        this.address = UNSAFE.allocateMemory(this.size * MESH_INFO_SIZE);
        this.count = 0;
    }

    @Override
    public void reset() {
        count = 0;
    }

    @Override
    public void delete() {
        UNSAFE.freeMemory(address);
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
            address = UNSAFE.reallocateMemory(address, size * MESH_INFO_SIZE);
        }

        var infoAddress = address + count * MESH_INFO_SIZE;

        UNSAFE.putInt(infoAddress + COLOR_OFFSET, color);
        UNSAFE.putInt(infoAddress + LIGHT_OFFSET, light);
        UNSAFE.putInt(infoAddress + OVERLAY_OFFSET, overlay);
        UNSAFE.putInt(infoAddress + SHARING_OFFSET, sharing);
        UNSAFE.putInt(infoAddress + SHOULD_CULL_OFFSET, shouldCull);
        UNSAFE.putInt(infoAddress + RENDERED_ENTITY_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
        UNSAFE.putInt(infoAddress + RENDERED_BLOCK_ENTITY_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
        UNSAFE.putInt(infoAddress + RENDERED_ITEM_OFFSET, CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

        count++;
    }

    @Override
    public int getMeshCount() {
        return count;
    }

    @Override
    public int getSharing(int i) {
        return UNSAFE.getInt(address + i * MESH_INFO_SIZE + SHARING_OFFSET);
    }

    @Override
    public int getShouldCull(int i) {
        return UNSAFE.getInt(address + i * MESH_INFO_SIZE + SHOULD_CULL_OFFSET);
    }

    @Override
    public int getColor(int i) {
        return UNSAFE.getInt(address + i * MESH_INFO_SIZE + COLOR_OFFSET);
    }

    @Override
    public int getLight(int i) {
        return UNSAFE.getInt(address + i * MESH_INFO_SIZE + LIGHT_OFFSET);
    }

    @Override
    public int getOverlay(int i) {
        return UNSAFE.getInt(address + i * MESH_INFO_SIZE + OVERLAY_OFFSET);
    }

    @Override
    public short getRenderedEntity(int i) {
        return (short) UNSAFE.getInt(address + i * MESH_INFO_SIZE + RENDERED_ENTITY_OFFSET);
    }

    @Override
    public short getRenderedBlockEntity(int i) {
        return (short) UNSAFE.getInt(address + i * MESH_INFO_SIZE + RENDERED_BLOCK_ENTITY_OFFSET);
    }

    @Override
    public short getRenderedItem(int i) {
        return (short) UNSAFE.getInt(address + i * MESH_INFO_SIZE + RENDERED_ITEM_OFFSET);
    }
}
