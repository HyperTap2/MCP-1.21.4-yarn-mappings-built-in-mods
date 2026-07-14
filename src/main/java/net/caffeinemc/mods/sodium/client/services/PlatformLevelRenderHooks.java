package net.caffeinemc.mods.sodium.client.services;

import java.util.List;
import java.util.function.Function;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public interface PlatformLevelRenderHooks {
   PlatformLevelRenderHooks INSTANCE = Services.load(PlatformLevelRenderHooks.class);

   static PlatformLevelRenderHooks getInstance() {
      return INSTANCE;
   }

   void runChunkLayerEvents(RenderLayer var1, World var2, WorldRenderer var3, Matrix4f var4, Matrix4f var5, int var6, Camera var7, Frustum var8);

   List<?> retrieveChunkMeshAppenders(World var1, BlockPos var2);

   void runChunkMeshAppenders(List<?> var1, Function<RenderLayer, VertexConsumer> var2, LevelSlice var3);
}
