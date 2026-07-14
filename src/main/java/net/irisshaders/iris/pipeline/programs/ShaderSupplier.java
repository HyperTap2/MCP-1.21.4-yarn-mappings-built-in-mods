package net.irisshaders.iris.pipeline.programs;

import java.util.function.Supplier;
import net.minecraft.client.gl.ShaderProgram;

public record ShaderSupplier(ShaderKey key, int id, Supplier<ShaderProgram> shader) {
}
