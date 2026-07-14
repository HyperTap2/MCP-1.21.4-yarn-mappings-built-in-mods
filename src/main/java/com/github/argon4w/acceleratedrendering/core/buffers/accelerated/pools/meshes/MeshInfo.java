package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.meshes;

import com.github.argon4w.acceleratedrendering.core.utils.SimpleCachedArray;
import lombok.Getter;
import com.github.argon4w.acceleratedrendering.compat.iris.interfaces.IIrisMeshInfo;
import net.irisshaders.iris.uniforms.CapturedRenderingState;

@Getter
public class MeshInfo implements SimpleCachedArray.Element, IIrisMeshInfo {

    private int color;
    private int light;
    private int overlay;
    private int sharing;
    private int shouldCull;
    private short renderedEntity;
    private short renderedBlockEntity;
    private short renderedItem;

    public MeshInfo() {
        this.color = -1;
        this.light = -1;
        this.overlay = -1;
        this.sharing = -1;
    }

    public void setupMeshInfo(
            int color,
            int light,
            int overlay,
            int sharing,
            int shouldCull
    ) {
        this.color = color;
        this.light = light;
        this.overlay = overlay;
        this.sharing = sharing;
        this.shouldCull = shouldCull;
        this.renderedEntity = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        this.renderedBlockEntity = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
        this.renderedItem = (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem();
    }

    @Override
    public void reset() {

    }

    @Override
    public void delete() {

    }

    @Override
    public short getRenderedEntity() {
        return renderedEntity;
    }

    @Override
    public short getRenderedBlockEntity() {
        return renderedBlockEntity;
    }

    @Override
    public short getRenderedItem() {
        return renderedItem;
    }
}
