package net.minecraft.client.render.block.entity;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;

public class EndPortalBlockEntityRenderer<T extends EndPortalBlockEntity> implements BlockEntityRenderer<T> {
   public static final Identifier SKY_TEXTURE = Identifier.ofVanilla("textures/environment/end_sky.png");
   public static final Identifier PORTAL_TEXTURE = Identifier.ofVanilla("textures/entity/end_portal.png");

   public EndPortalBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
   }

   public void render(T endPortalBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j) {
      if (!Iris.getCurrentPack().isEmpty()) {
         this.iris$renderPortal(endPortalBlockEntity, matrixStack, vertexConsumerProvider, i, j);
         return;
      }

      Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
      this.renderSides(endPortalBlockEntity, matrix4f, vertexConsumerProvider.getBuffer(this.getLayer()));
   }

   private void iris$renderPortal(
      T entity, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay
   ) {
      VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(PORTAL_TEXTURE));
      MatrixStack.Entry matrix = matrices.peek();
      float progress = SystemTimeUniforms.TIMER.getFrameTimeCounter() * 0.01F % 1.0F;
      float top = this.getTopYOffset();
      float bottom = this.getBottomYOffset();
      this.iris$quad(entity, vertices, matrix, Direction.UP, progress, overlay, light,
         0.0F, top, 1.0F, 1.0F, top, 1.0F, 1.0F, top, 0.0F, 0.0F, top, 0.0F);
      this.iris$quad(entity, vertices, matrix, Direction.DOWN, progress, overlay, light,
         0.0F, bottom, 1.0F, 0.0F, bottom, 0.0F, 1.0F, bottom, 0.0F, 1.0F, bottom, 1.0F);
      this.iris$quad(entity, vertices, matrix, Direction.NORTH, progress, overlay, light,
         0.0F, top, 0.0F, 1.0F, top, 0.0F, 1.0F, bottom, 0.0F, 0.0F, bottom, 0.0F);
      this.iris$quad(entity, vertices, matrix, Direction.WEST, progress, overlay, light,
         0.0F, top, 1.0F, 0.0F, top, 0.0F, 0.0F, bottom, 0.0F, 0.0F, bottom, 1.0F);
      this.iris$quad(entity, vertices, matrix, Direction.SOUTH, progress, overlay, light,
         0.0F, top, 1.0F, 0.0F, bottom, 1.0F, 1.0F, bottom, 1.0F, 1.0F, top, 1.0F);
      this.iris$quad(entity, vertices, matrix, Direction.EAST, progress, overlay, light,
         1.0F, top, 1.0F, 1.0F, bottom, 1.0F, 1.0F, bottom, 0.0F, 1.0F, top, 0.0F);
   }

   private void iris$quad(
      T entity,
      VertexConsumer vertices,
      MatrixStack.Entry matrix,
      Direction direction,
      float progress,
      int overlay,
      int light,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4
   ) {
      if (!entity.shouldDrawSide(direction)) {
         return;
      }

      float normalX = direction.getOffsetX();
      float normalY = direction.getOffsetY();
      float normalZ = direction.getOffsetZ();
      vertices.vertex(matrix, x1, y1, z1)
         .color(0.075F, 0.15F, 0.2F, 1.0F)
         .texture(progress, progress)
         .overlay(overlay)
         .light(light)
         .normal(matrix, normalX, normalY, normalZ);
      vertices.vertex(matrix, x2, y2, z2)
         .color(0.075F, 0.15F, 0.2F, 1.0F)
         .texture(progress, 0.2F + progress)
         .overlay(overlay)
         .light(light)
         .normal(matrix, normalX, normalY, normalZ);
      vertices.vertex(matrix, x3, y3, z3)
         .color(0.075F, 0.15F, 0.2F, 1.0F)
         .texture(0.2F + progress, 0.2F + progress)
         .overlay(overlay)
         .light(light)
         .normal(matrix, normalX, normalY, normalZ);
      vertices.vertex(matrix, x4, y4, z4)
         .color(0.075F, 0.15F, 0.2F, 1.0F)
         .texture(0.2F + progress, progress)
         .overlay(overlay)
         .light(light)
         .normal(matrix, normalX, normalY, normalZ);
   }

   private void renderSides(T entity, Matrix4f matrix, VertexConsumer vertexConsumer) {
      float f = this.getBottomYOffset();
      float g = this.getTopYOffset();
      this.renderSide(entity, matrix, vertexConsumer, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
      this.renderSide(entity, matrix, vertexConsumer, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
      this.renderSide(entity, matrix, vertexConsumer, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
      this.renderSide(entity, matrix, vertexConsumer, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
      this.renderSide(entity, matrix, vertexConsumer, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
      this.renderSide(entity, matrix, vertexConsumer, 0.0F, 1.0F, g, g, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
   }

   private void renderSide(
      T entity, Matrix4f model, VertexConsumer vertices, float x1, float x2, float y1, float y2, float z1, float z2, float z3, float z4, Direction side
   ) {
      if (entity.shouldDrawSide(side)) {
         vertices.vertex(model, x1, y1, z1);
         vertices.vertex(model, x2, y1, z2);
         vertices.vertex(model, x2, y2, z3);
         vertices.vertex(model, x1, y2, z4);
      }
   }

   protected float getTopYOffset() {
      return 0.75F;
   }

   protected float getBottomYOffset() {
      return 0.375F;
   }

   protected RenderLayer getLayer() {
      return RenderLayer.getEndPortal();
   }
}
