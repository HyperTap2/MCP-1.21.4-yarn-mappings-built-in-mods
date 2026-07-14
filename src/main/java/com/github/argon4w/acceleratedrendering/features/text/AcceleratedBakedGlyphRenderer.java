package com.github.argon4w.acceleratedrendering.features.text;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.VertexConsumerExtension;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.SimpleMeshCollector;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.ExtensionMethod;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Map;

@ExtensionMethod(VertexConsumerExtension.class)
public class AcceleratedBakedGlyphRenderer implements IAcceleratedRenderer<Vector3f> {

    private static final Matrix4f TRANSFORM = new Matrix4f();
    private static final Matrix3f NORMAL = new Matrix3f();

    private final Map<IBufferGraph, IMesh> meshes;
    private final BakedGlyph bakedGlyph;
    private final boolean italic;
    private final boolean bold;
    private long resourceGeneration = -1L;

    public AcceleratedBakedGlyphRenderer(BakedGlyph bakedGlyph, boolean italic) {
        this(bakedGlyph, italic, false);
    }

    public AcceleratedBakedGlyphRenderer(BakedGlyph bakedGlyph, boolean italic, boolean bold) {
        this.meshes = new Object2ObjectOpenHashMap<>();
        this.bakedGlyph = bakedGlyph;
        this.italic = italic;
        this.bold = bold;
    }

    @Override
    public void render(
            VertexConsumer vertexConsumer,
            Vector3f context,
            Matrix4f transform,
            Matrix3f normal,
            int light,
            int overlay,
            int color
    ) {
        long generation = AcceleratedRendering.getResourceGeneration();
        if (resourceGeneration != generation) {
            meshes.clear();
            resourceGeneration = generation;
        }

        var extension = vertexConsumer.getAccelerated();
        var mesh = meshes.get(extension);

        TRANSFORM.set(transform);
        TRANSFORM.translate(
                context.x,
                context.y,
                context.z
        );

        extension.beginTransform(TRANSFORM, NORMAL);
        try {
            if (mesh != null) {
                mesh.write(extension, color, light, overlay);
                return;
            }

            var meshCollector = new SimpleMeshCollector(extension.getBufferSet().getBufferEnvironment().getLayout());
            var meshBuilder = extension.decorate(meshCollector);

            var italicOffsetUp = italic ? 1.0f - 0.25f * bakedGlyph.acceleratedRendering$getMinY() : 0.0f;
            var italicOffsetDown = italic ? 1.0f - 0.25f * bakedGlyph.acceleratedRendering$getMaxY() : 0.0f;
            var boldOffset = bold ? 0.1f : 0.0f;

            var positions = new Vector2f[]{
                    new Vector2f(bakedGlyph.acceleratedRendering$getMinX() + italicOffsetUp - boldOffset, bakedGlyph.acceleratedRendering$getMinY() - boldOffset),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMinX() + italicOffsetDown - boldOffset, bakedGlyph.acceleratedRendering$getMaxY() + boldOffset),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMaxX() + italicOffsetDown + boldOffset, bakedGlyph.acceleratedRendering$getMaxY() + boldOffset),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMaxX() + italicOffsetUp + boldOffset, bakedGlyph.acceleratedRendering$getMinY() - boldOffset)
            };

            var texCoords = new Vector2f[]{
                    new Vector2f(bakedGlyph.acceleratedRendering$getMinU(), bakedGlyph.acceleratedRendering$getMinV()),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMinU(), bakedGlyph.acceleratedRendering$getMaxV()),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMaxU(), bakedGlyph.acceleratedRendering$getMaxV()),
                    new Vector2f(bakedGlyph.acceleratedRendering$getMaxU(), bakedGlyph.acceleratedRendering$getMinV()),
            };

            for (var i = 0; i < 4; i++) {
                var position = new Vector3f(positions[i], 0.0f);
                var texCoord = texCoords[i];
                meshBuilder.vertex(position).color(-1).texture(texCoord.x, texCoord.y).light(0);
            }

            mesh = AcceleratedTextRenderingFeature.getMeshType().getBuilder().build(meshCollector);
            meshes.put(extension, mesh);
            mesh.write(extension, color, light, overlay);
        } finally {
            extension.endTransform();
        }
    }
}
