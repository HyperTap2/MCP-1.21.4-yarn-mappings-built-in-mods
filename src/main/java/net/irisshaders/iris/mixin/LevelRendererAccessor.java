package net.irisshaders.iris.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.BlockBreakingInfo;
import org.joml.Matrix4f;

public interface LevelRendererAccessor {
   EntityRenderDispatcher getEntityRenderDispatcher();

   void invokeRenderSectionLayer(RenderLayer layer, double x, double y, double z, Matrix4f modelView, Matrix4f projection);

   void invokeSetupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

   void invokeRenderEntity(Entity entity, double x, double y, double z, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers);

   ClientWorld getLevel();

   BufferBuilderStorage getRenderBuffers();

   void setRenderBuffers(BufferBuilderStorage buffers);

   boolean invokeMethod_43788(Camera camera);

   Long2ObjectMap<SortedSet<BlockBreakingInfo>> getField_20950();
}
