package com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.AcceleratedBufferSetPool;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.ServerMesh;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

@ExtensionMethod(VertexConsumerExtension.class)
public abstract class AcceleratedVertexConsumerWrapper implements IAcceleratedVertexConsumer, VertexConsumer {

    @Override
    public abstract VertexConsumer decorate(VertexConsumer buffer);

    protected abstract VertexConsumer getDelegate();

    @Override
    public void beginTransform(Matrix4f transform, Matrix3f normal) {
        getDelegate()
                .getAccelerated()
                .beginTransform(transform, normal);
    }

    @Override
    public void endTransform() {
        getDelegate()
                .getAccelerated()
                .endTransform();
    }

    @Override
    public boolean isAccelerated() {
        return getDelegate()
                .getAccelerated()
                .isAccelerated();
    }

    @Override
    public AcceleratedBufferSetPool.BufferSet getBufferSet() {
        return getDelegate()
                .getAccelerated()
                .getBufferSet();
    }

    @Override
    public RenderLayer getRenderType() {
        return getDelegate()
                .getAccelerated()
                .getRenderType();
    }

    @Override
    public void addClientMesh(
            ByteBuffer meshBuffer,
            int size,
            int color,
            int light,
            int overlay
    ) {
        getDelegate()
                .getAccelerated()
                .addClientMesh(
                        meshBuffer,
                        size,
                        color,
                        light,
                        overlay
                );
    }

    @Override
    public void addServerMesh(
            ServerMesh serverMesh,
            int color,
            int light,
            int overlay
    ) {
        getDelegate()
                .getAccelerated()
                .addServerMesh(
                        serverMesh,
                        color,
                        light,
                        overlay
                );
    }

    @Override
    public <T> void doRender(
            IAcceleratedRenderer<T> renderer,
            T context,
            Matrix4f transform,
            Matrix3f normal,
            int light,
            int overlay,
            int color
    ) {
        renderer.render(
                this,
                context,
                transform,
                normal,
                light,
                overlay,
                color
        );
    }

    @Override
    public VertexConsumer vertex(
            float x,
            float y,
            float z
    ) {
        getDelegate().vertex(
                x,
                y,
                z
        );
        return this;
    }

    @Override
    public VertexConsumer vertex(
            MatrixStack.Entry pose,
            float x,
            float y,
            float z
    ) {
        getDelegate().vertex(
                pose,
                x,
                y,
                z
        );
        return this;
    }

    @Override
    public VertexConsumer color(
            int red,
            int green,
            int blue,
            int alpha
    ) {
        getDelegate().color(
                red,
                green,
                blue,
                alpha
        );
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        getDelegate().texture(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        getDelegate().overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        getDelegate().light(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(
            float normalX,
            float normalY,
            float normalZ
    ) {
        getDelegate().normal(
                normalX,
                normalY,
                normalZ
        );
        return this;
    }

    @Override
    public VertexConsumer normal(
            MatrixStack.Entry pose,
            float normalX,
            float normalY,
            float normalZ
    ) {
        getDelegate().normal(
                pose,
                normalX,
                normalY,
                normalZ
        );
        return this;
    }

    @Override
    public void vertex(
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int packedOverlay,
            int packedLight,
            float normalX,
            float normalY,
            float normalZ
    ) {
        getDelegate().vertex(
                x,
                y,
                z,
                color,
                u,
                v,
                packedOverlay,
                packedLight,
                normalX,
                normalY,
                normalZ
        );
    }
}
