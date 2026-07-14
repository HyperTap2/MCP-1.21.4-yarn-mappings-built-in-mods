package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;

public interface ChunkRenderer {
   void render(ChunkRenderMatrices var1, CommandList var2, ChunkRenderListIterable var3, TerrainRenderPass var4, CameraTransform var5);

   void delete(CommandList var1);
}
