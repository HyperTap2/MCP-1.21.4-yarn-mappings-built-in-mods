package net.irisshaders.iris.pathways;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.compat.iris.IrisCompatBuffers;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class HandRenderer {
   public static final HandRenderer INSTANCE = new HandRenderer();
   public static final float DEPTH = 0.125F;
   private final FullyBufferedMultiBufferSource bufferSource = new FullyBufferedMultiBufferSource();
   private boolean ACTIVE;
   private boolean renderingSolid;

   private MatrixStack setupGlState(GameRenderer gameRenderer, Camera camera, Matrix4fc modelMatrix, float tickDelta) {
      MatrixStack poseStack = new MatrixStack();
      Matrix4f scaleMatrix = new Matrix4f().scale(1.0F, 1.0F, 0.125F);
      scaleMatrix.mul(gameRenderer.getBasicProjectionMatrix(((GameRendererAccessor)gameRenderer).invokeGetFov(camera, tickDelta, false)));
      RenderSystem.setProjectionMatrix(scaleMatrix, ProjectionType.PERSPECTIVE);
      poseStack.loadIdentity();
      ((GameRendererAccessor)gameRenderer).invokeBobHurt(poseStack, tickDelta);
      if ((Boolean)MinecraftClient.getInstance().options.getBobView().getValue()) {
         ((GameRendererAccessor)gameRenderer).invokeBobView(poseStack, tickDelta);
      }

      return poseStack;
   }

   private boolean canRender(Camera camera, GameRenderer gameRenderer) {
      return ((GameRendererAccessor)gameRenderer).getRenderHand()
         && !camera.isThirdPerson()
         && camera.getFocusedEntity() instanceof PlayerEntity
         && !((GameRendererAccessor)gameRenderer).getPanoramicMode()
         && !MinecraftClient.getInstance().options.hudHidden
         && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping())
         && MinecraftClient.getInstance().interactionManager.getCurrentGameMode() != GameMode.SPECTATOR;
   }

   public boolean isHandTranslucent(Hand hand) {
      Item item = MinecraftClient.getInstance().player.getEquippedStack(hand == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND).getItem();
      return item instanceof BlockItem ? RenderLayers.getBlockLayer(((BlockItem)item).getBlock().getDefaultState()) == RenderLayer.getTranslucent() : false;
   }

   public boolean isAnyHandTranslucent() {
      return this.isHandTranslucent(Hand.MAIN_HAND) || this.isHandTranslucent(Hand.OFF_HAND);
   }

   public void renderSolid(Matrix4fc modelMatrix, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
      if (Iris.isPackInUseQuick() && this.canRender(camera, gameRenderer)) {
         this.ACTIVE = true;
         MatrixStack poseStack = this.setupGlState(gameRenderer, camera, modelMatrix, tickDelta);
         pipeline.setPhase(WorldRenderingPhase.HAND_SOLID);
         poseStack.push();
         Profilers.get().push("iris_hand");
         this.renderingSolid = true;
         RenderSystem.getModelViewStack().pushMatrix();
         RenderSystem.getModelViewStack().set(poseStack.peek().getPositionMatrix());
         gameRenderer.firstPersonRenderer
            .renderItem(
               tickDelta,
               new MatrixStack(),
               this.bufferSource.getUnflushableWrapper(),
               MinecraftClient.getInstance().player,
               MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(camera.getFocusedEntity(), tickDelta)
            );
         Profilers.get().pop();
         this.bufferSource.readyUp();
         if (AcceleratedRendering.isAvailable()) {
            IrisCompatBuffers.drawHandBuffers();
         }
         this.bufferSource.draw();
         RenderSystem.setProjectionMatrix(new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection()), ProjectionType.PERSPECTIVE);
         poseStack.pop();
         RenderSystem.getModelViewStack().popMatrix();
         this.renderingSolid = false;
         pipeline.setPhase(WorldRenderingPhase.NONE);
         this.ACTIVE = false;
      }
   }

   public void renderTranslucent(Matrix4fc modelMatrix, float tickDelta, Camera camera, GameRenderer gameRenderer, WorldRenderingPipeline pipeline) {
      if (Iris.isPackInUseQuick() && this.canRender(camera, gameRenderer) && this.isAnyHandTranslucent()) {
         this.ACTIVE = true;
         pipeline.setPhase(WorldRenderingPhase.HAND_TRANSLUCENT);
         MatrixStack poseStack = this.setupGlState(gameRenderer, camera, modelMatrix, tickDelta);
         poseStack.push();
         Profilers.get().push("iris_hand_translucent");
         RenderSystem.getModelViewStack().pushMatrix();
         RenderSystem.getModelViewStack().set(poseStack.peek().getPositionMatrix());
         gameRenderer.firstPersonRenderer
            .renderItem(
               tickDelta,
               new MatrixStack(),
               this.bufferSource,
               MinecraftClient.getInstance().player,
               MinecraftClient.getInstance().getEntityRenderDispatcher().getLight(camera.getFocusedEntity(), tickDelta)
            );
         poseStack.pop();
         Profilers.get().pop();
         RenderSystem.setProjectionMatrix(new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection()), ProjectionType.PERSPECTIVE);
         if (AcceleratedRendering.isAvailable()) {
            IrisCompatBuffers.drawHandBuffers();
         }
         this.bufferSource.draw();
         RenderSystem.getModelViewStack().popMatrix();
         pipeline.setPhase(WorldRenderingPhase.NONE);
         this.ACTIVE = false;
      }
   }

   public boolean isActive() {
      return this.ACTIVE;
   }

   public boolean isRenderingSolid() {
      return this.renderingSolid;
   }

   public FullyBufferedMultiBufferSource getBufferSource() {
      return this.bufferSource;
   }
}
