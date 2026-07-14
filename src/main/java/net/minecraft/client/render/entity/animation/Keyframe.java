package net.minecraft.client.render.entity.animation;

import org.joml.Vector3f;

public record Keyframe(float timestamp, Vector3f target, Transformation.Interpolation interpolation) {
}
