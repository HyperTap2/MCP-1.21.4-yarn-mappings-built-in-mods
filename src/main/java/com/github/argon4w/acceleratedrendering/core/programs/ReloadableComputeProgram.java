package com.github.argon4w.acceleratedrendering.core.programs;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.backends.programs.ComputeProgram;
import com.github.argon4w.acceleratedrendering.core.backends.programs.Uniform;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

/** Keeps dispatcher references synchronized with compute shader resource reloads. */
public final class ReloadableComputeProgram {
    private final Identifier key;
    private final Map<String, Uniform> uniforms = new Object2ObjectOpenHashMap<>();
    private long generation = Long.MIN_VALUE;
    private ComputeProgram program;

    public ReloadableComputeProgram(Identifier key) {
        this.key = key;
    }

    public ComputeProgram get() {
        long currentGeneration = AcceleratedRendering.getResourceGeneration();
        if (this.program == null || this.generation != currentGeneration) {
            this.program = ComputeShaderProgramLoader.getProgram(this.key);
            this.uniforms.clear();
            this.generation = currentGeneration;
        }
        return this.program;
    }

    public Uniform uniform(String name) {
        ComputeProgram currentProgram = this.get();
        return this.uniforms.computeIfAbsent(name, currentProgram::getUniform);
    }
}
