package com.github.argon4w.acceleratedrendering.core.utils;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

public class CullerUtils {

    public static boolean shouldCull(Vertex[] vertices, NativeImage texture) {
        if (texture == null) {
            return false;
        }

        if (vertices.length == 4) {
            var vertex0 = new Vector3f(vertices[0].getPosition());
            var vector1 = new Vector3f(vertices[1].getPosition()).sub(vertex0);
            var vector2 = new Vector3f(vertices[2].getPosition()).sub(vertex0);
            var vector3 = new Vector3f(vertices[3].getPosition()).sub(vertex0);

            var length1 = vector1.cross(vector2).length();
            var length2 = vector1.cross(vector3).length();

            if (length1 == 0 && length2 == 0) {
                return true;
            }
        }

        var minU = 1.0f;
        var minV = 1.0f;

        var maxU = 0.0f;
        var maxV = 0.0f;

        for (var vertex : vertices) {
            var uv = vertex.getUv();
            var u = uv.x;
            var v = uv.y;

            u = u < 0 ? 1.0f + u : u;
            v = v < 0 ? 1.0f + v : v;

            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);
        }

        var minX = Math.max(0, MathHelper.floor(minU * texture.getWidth()));
        var minY = Math.max(0, MathHelper.floor(minV * texture.getHeight()));

        var maxX = Math.min(texture.getWidth(), MathHelper.ceil(maxU * texture.getWidth()));
        var maxY = Math.min(texture.getHeight(), MathHelper.ceil(maxV * texture.getHeight()));

        for (var x = minX; x < maxX; x++) {
            for (var y = minY; y < maxY; y++) {
                if (ColorHelper.getAlpha(texture.getColorArgb(x, y)) != 0) {
                    return false;
                }
            }
        }

        return true;
    }
}
