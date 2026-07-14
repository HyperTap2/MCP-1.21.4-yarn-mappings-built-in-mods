package com.github.argon4w.acceleratedrendering.core.programs;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.backends.programs.ComputeProgram;
import com.github.argon4w.acceleratedrendering.core.backends.programs.ComputeShader;
import com.github.argon4w.acceleratedrendering.core.AcceleratedRenderingRegistry;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.profiler.Profiler;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ComputeShaderProgramLoader extends SinglePreparationResourceReloader<Map<Identifier, ComputeShaderProgramLoader.ShaderSource>> {

    public static final ComputeShaderProgramLoader INSTANCE = new ComputeShaderProgramLoader();
    private static final Map<Identifier, ComputeProgram> COMPUTE_SHADERS = new Object2ObjectOpenHashMap<>();
    private static boolean LOADED = false;

    public static ComputeProgram getProgram(Identifier resourceLocation) {
        var program = COMPUTE_SHADERS.get(resourceLocation);

        if (program == null) {
            throw new IllegalStateException("Get shader program \"" + resourceLocation + "\" too early! Program is not loaded yet!");
        }

        return program;
    }

    public static void delete() {
        for (var program : COMPUTE_SHADERS.values()) {
            program.delete();
        }

        COMPUTE_SHADERS.clear();
        LOADED = false;
    }

    public static boolean isProgramsLoaded() {
        return LOADED;
    }

    @Override
    protected Map<Identifier, ComputeShaderProgramLoader.ShaderSource> prepare(ResourceManager resourceManager, Profiler profiler) {
        try {
            var builder = AcceleratedRenderingRegistry.createShaderEvent().getShaderLocations();
            var shaderSources = new Object2ObjectOpenHashMap<Identifier, ShaderSource>();
            var shaderLocations = builder.build();

            for (Identifier key : shaderLocations.keySet()) {
                var definition = shaderLocations.get(key);
                var resourceLocation = definition.location;
                var barrierFlags = definition.barrierFlags;

                if (resourceLocation == null) {
                    throw new IllegalStateException("Found empty shader location on: \"" + key + "\"");
                }

                var resource = resourceManager.getResource(resourceLocation);

                if (resource.isEmpty()) {
                    throw new IllegalStateException("Cannot found compute shader: \"" + resourceLocation + "\"");
                }

                try (var stream = resource.get().getInputStream()) {
                    shaderSources.put(key, new ShaderSource(new String(stream.readAllBytes(), StandardCharsets.UTF_8), barrierFlags));
                }
            }

            return shaderSources;
        } catch (Exception e) {
            throw new CrashException(CrashReport.create(e, "Exception while loading compute shader"));
        }
    }

    @Override
    protected void apply(
            Map<Identifier, ShaderSource> shaderSources,
            ResourceManager resourceManager,
            Profiler profiler
    ) {
        RenderSystem.recordRenderCall(() -> {
            delete();
            try {
                for (var key : shaderSources.keySet()) {
                    var source = shaderSources.get(key);
                    var shaderSource = source.source;
                    var barrierFlags = source.barrierFlags;

                    var program = new ComputeProgram(barrierFlags);
                    var computeShader = new ComputeShader();

                    computeShader.setShaderSource(shaderSource);
                    computeShader.compileShader();

                    if (!computeShader.isCompiled()) {
                        throw new IllegalStateException("Shader \"" + key + "\" failed to compile because of the following errors: " + computeShader.getInfoLog());
                    }

                    program.attachShader(computeShader);
                    program.linkProgram();

                    if (!program.isLinked()) {
                        throw new IllegalStateException("Program \"" + key + "\" failed to link because of the following errors: " + program.getInfoLog());
                    }

                    computeShader.delete();
                    COMPUTE_SHADERS.put(key, program);
                }
                LOADED = true;
                AcceleratedRendering.invalidateCaches();
            } catch (Throwable throwable) {
                delete();
                AcceleratedRendering.disableAfterFailure("compute shader compilation/linking failed", throwable);
            }
        });
    }

    public record ShaderDefinition(Identifier location, int barrierFlags) {

    }

    public record ShaderSource(String source, int barrierFlags) {

    }
}
