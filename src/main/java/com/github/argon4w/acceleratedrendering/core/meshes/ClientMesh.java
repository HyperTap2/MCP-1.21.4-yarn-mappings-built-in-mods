package com.github.argon4w.acceleratedrendering.core.meshes;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.IMeshCollector;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import lombok.AllArgsConstructor;
import net.minecraft.client.util.BufferAllocator;
import java.nio.ByteBuffer;
import java.util.Set;

@AllArgsConstructor
public class ClientMesh implements IMesh {

    private final int size;
    private final ByteBuffer vertexBuffer;

    @Override
    public void write(
            IAcceleratedVertexConsumer extension,
            int color,
            int light,
            int overlay
    ) {
        extension.addClientMesh(
                vertexBuffer,
                size,
                color,
                light,
                overlay
        );
    }

    public static class Builder implements IMesh.Builder {

        public static final com.github.argon4w.acceleratedrendering.core.meshes.ClientMesh.Builder INSTANCE = new com.github.argon4w.acceleratedrendering.core.meshes.ClientMesh.Builder();

        private final Set<BufferAllocator> builders;

        private Builder() {
            this.builders = new ReferenceLinkedOpenHashSet<>();
        }

        @Override
        public IMesh build(IMeshCollector collector) {
            var vertexCount = collector.getVertexCount();

            if (vertexCount == 0) {
                return EmptyMesh.INSTANCE;
            }

            var builder = collector.getBuffer();
            var result = builder.getAllocated();

            if (result == null) {
                builder.close();
                return EmptyMesh.INSTANCE;
            }

            builders.add(builder);
            return new ClientMesh(vertexCount, result.getBuffer());
        }

        @Override
        public void delete() {
            for (var builder : builders) {
                builder.close();
            }

            builders.clear();
        }
    }
}
