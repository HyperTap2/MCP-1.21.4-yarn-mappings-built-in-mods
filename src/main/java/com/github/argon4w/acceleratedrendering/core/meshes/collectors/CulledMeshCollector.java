package com.github.argon4w.acceleratedrendering.core.meshes.collectors;

import com.github.argon4w.acceleratedrendering.core.buffers.memory.IMemoryLayout;
import com.github.argon4w.acceleratedrendering.core.utils.CullerUtils;
import com.github.argon4w.acceleratedrendering.core.utils.TextureUtils;
import com.github.argon4w.acceleratedrendering.core.utils.Vertex;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.BufferAllocator;

public class CulledMeshCollector implements VertexConsumer, IMeshCollector {

    private final int polygonSize;
    private final NativeImage texture;
    private final SimpleMeshCollector meshCollector;
    private final Vertex[] polygon;

    private int vertexIndex;

    public CulledMeshCollector(RenderLayer renderType, IMemoryLayout<VertexFormatElement> layout) {
        this.polygonSize = renderType.getDrawMode().firstVertexCount;
        this.texture = TextureUtils.downloadTexture(renderType, 0);
        this.meshCollector = new SimpleMeshCollector(layout);
        this.polygon = new Vertex[this.polygonSize];

        this.vertexIndex = -1;
    }

    public void flush() {
        if (vertexIndex >= polygonSize - 1) {
            vertexIndex = -1;

            if (!CullerUtils.shouldCull(polygon, texture)) {
                for (var vertex : polygon) {
                    var vertexPosition = vertex.getPosition();
                    var vertexUV = vertex.getUv();
                    var vertexNormal = vertex.getNormal();

                    meshCollector.vertex(
                            vertexPosition.x,
                            vertexPosition.y,
                            vertexPosition.z,
                            vertex.getPackedColor(),
                            vertexUV.x,
                            vertexUV.y,
                            OverlayTexture.DEFAULT_UV,
                            vertex.getPackedLight(),
                            vertexNormal.x,
                            vertexNormal.y,
                            vertexNormal.z
                    );
                }
            }
        }
    }

    @Override
    public VertexConsumer vertex(
            float pX,
            float pY,
            float pZ
    ) {
        flush();
        polygon[++vertexIndex] = new Vertex();
        polygon[vertexIndex].getPosition().x = pX;
        polygon[vertexIndex].getPosition().y = pY;
        polygon[vertexIndex].getPosition().z = pZ;

        return this;
    }

    @Override
    public VertexConsumer color(
            int pRed,
            int pGreen,
            int pBlue,
            int pAlpha
    ) {
        if (vertexIndex < 0) {
            throw new IllegalStateException("Vertex not building!");
        }

        polygon[vertexIndex].getColor().x = pRed;
        polygon[vertexIndex].getColor().y = pGreen;
        polygon[vertexIndex].getColor().z = pBlue;
        polygon[vertexIndex].getColor().w = pAlpha;

        return this;
    }

    @Override
    public VertexConsumer texture(float pU, float pV) {
        if (vertexIndex < 0) {
            throw new IllegalStateException("Vertex not building!");
        }

        polygon[vertexIndex].getUv().x = pU;
        polygon[vertexIndex].getUv().y = pV;

        return this;
    }

    @Override
    public VertexConsumer overlay(int pU, int pV) {
        return this;
    }

    @Override
    public VertexConsumer light(int pU, int pV) {
        if (vertexIndex < 0) {
            throw new IllegalStateException("Vertex not building!");
        }

        polygon[vertexIndex].getLight().x = pU;
        polygon[vertexIndex].getLight().y = pV;

        return this;
    }

    @Override
    public VertexConsumer normal(
            float pNormalX,
            float pNormalY,
            float pNormalZ
    ) {
        if (vertexIndex < 0) {
            throw new IllegalStateException("Vertex not building!");
        }

        polygon[vertexIndex].getNormal().x = pNormalX;
        polygon[vertexIndex].getNormal().y = pNormalY;
        polygon[vertexIndex].getNormal().z = pNormalZ;
        return this;
    }

    @Override
    public BufferAllocator getBuffer() {
        return meshCollector.getBuffer();
    }

    @Override
    public IMemoryLayout<VertexFormatElement> getLayout() {
        return meshCollector.getLayout();
    }

    @Override
    public int getVertexCount() {
        return meshCollector.getVertexCount();
    }
}
