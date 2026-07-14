package net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.NotNull;

public interface ChunkModelBuilder {
   ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing var1);

   void addSprite(@NotNull Sprite var1);

   VertexConsumer asFallbackVertexConsumer(Material var1, TranslucentGeometryCollector var2);
}
