package com.github.argon4w.acceleratedrendering.core.programs;

import com.github.argon4w.acceleratedrendering.core.backends.programs.BarrierFlags;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import net.minecraft.util.Identifier;

@Getter
public class LoadComputeShaderEvent {

    private final ImmutableMap.Builder<Identifier, ComputeShaderProgramLoader.ShaderDefinition> shaderLocations;

    public LoadComputeShaderEvent(ImmutableMap.Builder<Identifier, ComputeShaderProgramLoader.ShaderDefinition> builder) {
        this.shaderLocations = builder;
    }

    public ImmutableMap.Builder<Identifier, ComputeShaderProgramLoader.ShaderDefinition> getShaderLocations() {
        return shaderLocations;
    }

    public void loadComputeShader(
            Identifier key,
            Identifier location,
            BarrierFlags... barrierFlags
    ) {
        shaderLocations.put(key, new ComputeShaderProgramLoader.ShaderDefinition(location, BarrierFlags.getFlags(barrierFlags)));
    }
}
