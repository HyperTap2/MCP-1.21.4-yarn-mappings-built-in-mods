package net.caffeinemc.mods.sodium.client.render.frapi.render;

import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class NonTerrainBlockRenderContext extends AbstractBlockRenderContext {
   private final BlockColors colorMap;
   private final SingleBlockLightDataCache lightDataCache = new SingleBlockLightDataCache();
   private VertexConsumer vertexConsumer;
   private Matrix4f matPosition;
   private boolean trustedNormals;
   private Matrix3f matNormal;
   private int overlay;

   public NonTerrainBlockRenderContext(BlockColors colorMap) {
      this.colorMap = colorMap;
      this.lighters = new LightPipelineProvider(this.lightDataCache);
   }

   public void renderModel(
      BlockRenderView blockView,
      BakedModel model,
      BlockState state,
      BlockPos pos,
      MatrixStack poseStack,
      VertexConsumer buffer,
      boolean cull,
      Random random,
      long seed,
      int overlay
   ) {
      this.level = blockView;
      this.state = state;
      this.pos = pos;
      this.random = random;
      this.randomSeed = seed;
      this.vertexConsumer = buffer;
      this.matPosition = poseStack.peek().getPositionMatrix();
      this.trustedNormals = poseStack.peek().canSkipNormalization;
      this.matNormal = poseStack.peek().getNormalMatrix();
      this.overlay = overlay;
      this.type = RenderLayers.getBlockLayer(state);
      this.modelData = SodiumModelData.EMPTY;
      this.lightDataCache.reset(pos, blockView);
      this.prepareCulling(cull);
      this.prepareAoInfo(model.useAmbientOcclusion());
      ((FabricBakedModel)model).emitBlockQuads(this.getEmitter(), blockView, state, pos, this.randomSupplier, this::isFaceCulled);
      this.level = null;
      this.type = null;
      this.modelData = null;
      this.lightDataCache.release();
      this.random = null;
      this.vertexConsumer = null;
   }

   @Override
   protected void processQuad(MutableQuadViewImpl quad) {
      RenderMaterial mat = quad.material();
      TriState aoMode = mat.ambientOcclusion();
      ShadeMode shadeMode = mat.shadeMode();
      LightMode lightMode;
      if (aoMode == TriState.DEFAULT) {
         lightMode = this.defaultLightMode;
      } else {
         lightMode = this.useAmbientOcclusion && aoMode.get() ? LightMode.SMOOTH : LightMode.FLAT;
      }

      boolean emissive = mat.emissive();
      this.tintQuad(quad);
      this.shadeQuad(quad, lightMode, emissive, shadeMode);
      this.bufferQuad(quad);
   }

   private void tintQuad(MutableQuadViewImpl quad) {
      if (quad.tintIndex() != -1) {
         int blockColor = 0xFF000000 | this.colorMap.getColor(this.state, this.level, this.pos, quad.tintIndex());

         for (int i = 0; i < 4; i++) {
            quad.color(i, ColorMixer.mulComponentWise(blockColor, quad.color(i)));
         }
      }
   }

   @Override
   protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode) {
      super.shadeQuad(quad, lightMode, emissive, shadeMode);
      float[] brightnesses = this.quadLightData.br;

      for (int i = 0; i < 4; i++) {
         quad.color(i, ColorARGB.mulRGB(quad.color(i), brightnesses[i]));
      }
   }

   private void bufferQuad(MutableQuadViewImpl quad) {
      QuadEncoder.writeQuadVertices(quad, this.vertexConsumer, this.overlay, this.matPosition, this.trustedNormals, this.matNormal);
      Sprite sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
      if (sprite != null) {
         SpriteUtil.INSTANCE.markSpriteActive(sprite);
      }
   }
}
