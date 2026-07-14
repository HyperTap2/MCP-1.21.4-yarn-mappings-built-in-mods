package com.github.argon4w.acceleratedrendering.core.buffers.accelerated;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedBufferBuilder;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.pools.DrawContextPool;
import com.github.argon4w.acceleratedrendering.core.buffers.environments.IBufferEnvironment;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Map;
import java.util.Set;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

import static org.lwjgl.opengl.GL46.*;

public class AcceleratedBufferSource extends VertexConsumerProvider.Immediate implements IAcceleratedBufferSource {

    private final IBufferEnvironment bufferEnvironment;
    private final Map<RenderLayer, DrawContextPool.IndirectDrawContext> drawContexts;
    private final AcceleratedBufferSetPool acceleratedBufferSetPool;
    private final Set<AcceleratedBufferSetPool.BufferSet> bufferSets;

    private AcceleratedBufferSetPool.BufferSet currentBufferSet;
    private boolean used;

    public AcceleratedBufferSource(IBufferEnvironment bufferEnvironment) {
        super(null, null);

        this.bufferEnvironment = bufferEnvironment;
        this.drawContexts = new Object2ObjectLinkedOpenHashMap<>();
        this.acceleratedBufferSetPool = new AcceleratedBufferSetPool(this.bufferEnvironment);
        this.currentBufferSet = this.acceleratedBufferSetPool.get(false);
        this.bufferSets = ObjectLinkedOpenHashSet.of(this.currentBufferSet);
        this.used = false;
    }

    public void delete() {
        acceleratedBufferSetPool.delete();
    }

    @Override
    public void drawCurrentLayer() {

    }

    @Override
    public void draw() {

    }

    @Override
    public void draw(RenderLayer pRenderType) {

    }

    @Override
    public IBufferEnvironment getBufferEnvironment() {
        return bufferEnvironment;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderType) {
        var builders = currentBufferSet.getBuilders();
        var builder = builders.get(renderType);

        if (builder != null) {
            return builder;
        }

        var vertexBuffer = currentBufferSet.getVertexBuffer();
        var varyingBuffer = currentBufferSet.getVaryingBuffer();
        var elementSegment = currentBufferSet.getElementSegment();

        if (vertexBuffer == null) {
            currentBufferSet = acceleratedBufferSetPool.get(true);
            builders = currentBufferSet.getBuilders();
            vertexBuffer = currentBufferSet.getVertexBuffer();
            varyingBuffer = currentBufferSet.getVaryingBuffer();
            elementSegment = currentBufferSet.getElementSegment();

            bufferSets.add(currentBufferSet);
        }

        RenderLayer builderRenderType = renderType instanceof WrappableRenderType wrapped ? wrapped.unwrap() : renderType;
        builder = new AcceleratedBufferBuilder(
                vertexBuffer,
                varyingBuffer,
                elementSegment,
                currentBufferSet,
                builderRenderType
        );

        used = true;
        builders.put(renderType, builder);

        return builder;
    }

    @Override
    public void drawBuffers() {
        if (!used) {
            return;
        }

        for (var bufferSet : bufferSets) {
            var builders = bufferSet.getBuilders();
            var program = glGetInteger(GL_CURRENT_PROGRAM);
            var barrier = 0;

            if (builders.isEmpty()) {
                continue;
            }

            bufferEnvironment.selectMeshUploadingProgramDispatcher().dispatch(builders.values(), bufferSet);
            bufferEnvironment.selectTransformProgramDispatcher().dispatch(builders.values());

            for (var renderType : builders.keySet()) {
                var builder = builders.get(renderType);
                var elementSegment = builder.getElementSegment();

                if (builder.isEmpty()) {
                    continue;
                }

                var mode = renderType.getDrawMode();
                var drawContext = bufferSet.getDrawContext();

                elementSegment.allocateOffset();
                bufferSet.bindElementBuffer(elementSegment);
                drawContext.bindComputeBuffers(elementSegment);
                drawContexts.put(renderType, drawContext);

                barrier |= bufferEnvironment.selectProcessingProgramDispatcher(mode).dispatch(builder);
                barrier |= builder.getCullingProgramDispatcher().dispatch(builder);
            }

            glMemoryBarrier(barrier);
            glUseProgram(program);
            BufferRenderer.resetCurrentVertexBuffer();
            bufferSet.bindDrawBuffers();

            for (var renderType : drawContexts.keySet()) {
                renderType.startDrawing();

                var drawContext = drawContexts.get(renderType);
                var shader = RenderSystem.getShader();
                var mode = renderType.getDrawMode();

                shader.initializeUniforms(
                        mode,
                        RenderSystem.getModelViewMatrix(),
                        RenderSystem.getProjectionMatrix(),
                        MinecraftClient.getInstance().getWindow()
                );

                shader.bind();
                drawContext.drawElements(mode);
                shader.unbind();
                renderType.endDrawing();
            }

            glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT | GL_COMMAND_BARRIER_BIT);
            bufferSet.reset();
            bufferSet.setInFlight();
            drawContexts.clear();
        }

        used = false;
        currentBufferSet = acceleratedBufferSetPool.get(false);
        bufferSets.clear();
        bufferSets.add(currentBufferSet);
    }
}
