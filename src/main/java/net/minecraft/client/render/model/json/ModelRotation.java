package net.minecraft.client.render.model.json;

import net.minecraft.util.math.Direction.Axis;
import org.joml.Vector3f;

public record ModelRotation(Vector3f origin, Axis axis, float angle, boolean rescale) {
}
