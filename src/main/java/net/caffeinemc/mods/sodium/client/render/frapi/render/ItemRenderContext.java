package net.caffeinemc.mods.sodium.client.render.frapi.render;

import java.util.Arrays;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.GlintMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.ItemRenderState.Glint;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ItemRenderContext extends AbstractRenderContext {
   private static final long ITEM_RANDOM_SEED = 42L;
   private static final int GLINT_COUNT = Glint.values().length;
   private final MutableQuadViewImpl editorQuad = new ItemRenderContext.ItemEmitter();
   private final ItemRenderContext.VanillaModelBufferer vanillaBufferer;
   private final Random random = new LocalRandom(42L);
   private final Supplier<Random> randomSupplier = () -> {
      this.random.setSeed(42L);
      return this.random;
   };
   private ModelTransformationMode transformMode;
   private MatrixStack poseStack;
   private Matrix4f matPosition;
   private boolean trustedNormals;
   private Matrix3f matNormal;
   private VertexConsumerProvider bufferSource;
   private int lightmap;
   private int overlay;
   private int[] colors;
   private RenderLayer defaultLayer;
   private Glint defaultGlint;
   private Entry specialGlintEntry;
   private final VertexConsumer[] vertexConsumerCache = new VertexConsumer[3 * GLINT_COUNT];

   public ItemRenderContext(ItemRenderContext.VanillaModelBufferer vanillaBufferer) {
      this.vanillaBufferer = vanillaBufferer;
   }

   @Override
   public QuadEmitter getEmitter() {
      this.editorQuad.clear();
      return this.editorQuad;
   }

   public void renderModel(
      ModelTransformationMode transformMode,
      MatrixStack poseStack,
      VertexConsumerProvider bufferSource,
      int lightmap,
      int overlay,
      BakedModel model,
      int[] colors,
      RenderLayer layer,
      Glint glint
   ) {
      this.transformMode = transformMode;
      this.poseStack = poseStack;
      this.matPosition = poseStack.peek().getPositionMatrix();
      this.trustedNormals = poseStack.peek().canSkipNormalization;
      this.matNormal = poseStack.peek().getNormalMatrix();
      this.bufferSource = bufferSource;
      this.lightmap = lightmap;
      this.overlay = overlay;
      this.colors = colors;
      this.defaultLayer = layer;
      this.defaultGlint = glint;
      ((FabricBakedModel)model).emitItemQuads(this.getEmitter(), this.randomSupplier);
      this.poseStack = null;
      this.bufferSource = null;
      this.colors = null;
      this.specialGlintEntry = null;
      Arrays.fill(this.vertexConsumerCache, null);
   }

   private void renderQuad(MutableQuadViewImpl quad) {
      RenderMaterial mat = quad.material();
      boolean emissive = mat.emissive();
      VertexConsumer vertexConsumer = this.getVertexConsumer(mat.blendMode(), mat.glintMode());
      this.tintQuad(quad);
      this.shadeQuad(quad, emissive);
      this.bufferQuad(quad, vertexConsumer);
   }

   private void tintQuad(MutableQuadViewImpl quad) {
      int tintIndex = quad.tintIndex();
      if (tintIndex != -1 && tintIndex < this.colors.length) {
         int color = this.colors[tintIndex];

         for (int i = 0; i < 4; i++) {
            quad.color(i, ColorMixer.mulComponentWise(color, quad.color(i)));
         }
      }
   }

   private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
      if (emissive) {
         for (int i = 0; i < 4; i++) {
            quad.lightmap(i, 15728880);
         }
      } else {
         int lightmap = this.lightmap;

         for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
         }
      }
   }

   private void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
      QuadEncoder.writeQuadVertices(quad, vertexConsumer, this.overlay, this.matPosition, this.trustedNormals, this.matNormal);
      Sprite sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
      if (sprite != null) {
         SpriteUtil.INSTANCE.markSpriteActive(sprite);
      }
   }

   private VertexConsumer getVertexConsumer(BlendMode blendMode, GlintMode glintMode) {
      RenderLayer type;
      if (blendMode == BlendMode.DEFAULT) {
         type = this.defaultLayer;
      } else {
         type = blendMode == BlendMode.TRANSLUCENT ? TexturedRenderLayers.getItemEntityTranslucentCull() : TexturedRenderLayers.getEntityCutout();
      }

      Glint glint;
      if (glintMode == GlintMode.DEFAULT) {
         glint = this.defaultGlint;
      } else {
         glint = glintMode.glint;
      }

      int cacheIndex;
      if (type == TexturedRenderLayers.getItemEntityTranslucentCull()) {
         cacheIndex = 0;
      } else if (type == TexturedRenderLayers.getEntityCutout()) {
         cacheIndex = GLINT_COUNT;
      } else {
         cacheIndex = 2 * GLINT_COUNT;
      }

      cacheIndex += glint.ordinal();
      VertexConsumer vertexConsumer = this.vertexConsumerCache[cacheIndex];
      if (vertexConsumer == null) {
         vertexConsumer = this.createVertexConsumer(type, glint);
         this.vertexConsumerCache[cacheIndex] = vertexConsumer;
      }

      return vertexConsumer;
   }

   private VertexConsumer createVertexConsumer(RenderLayer type, Glint glint) {
      if (glint == Glint.SPECIAL) {
         if (this.specialGlintEntry == null) {
            this.specialGlintEntry = this.poseStack.peek().copy();
            if (this.transformMode == ModelTransformationMode.GUI) {
               MatrixUtil.scale(this.specialGlintEntry.getPositionMatrix(), 0.5F);
            } else if (this.transformMode.isFirstPerson()) {
               MatrixUtil.scale(this.specialGlintEntry.getPositionMatrix(), 0.75F);
            }
         }

         return getDynamicDisplayGlintConsumer(this.bufferSource, type, this.specialGlintEntry);
      } else {
         return ItemRenderer.getItemGlintConsumer(this.bufferSource, type, true, glint != Glint.NONE);
      }
   }

   public void bufferDefaultModel(QuadEmitter quadEmitter, BakedModel model, @Nullable BlockState state) {
      if (this.vanillaBufferer == null) {
         VanillaModelEncoder.emitItemQuads(quadEmitter, model, null, this.randomSupplier);
      } else {
         VertexConsumer vertexConsumer;
         if (this.defaultGlint == Glint.SPECIAL) {
            Entry pose = this.poseStack.peek().copy();
            if (this.transformMode == ModelTransformationMode.GUI) {
               MatrixUtil.scale(pose.getPositionMatrix(), 0.5F);
            } else if (this.transformMode.isFirstPerson()) {
               MatrixUtil.scale(pose.getPositionMatrix(), 0.75F);
            }

            vertexConsumer = getDynamicDisplayGlintConsumer(this.bufferSource, this.defaultLayer, pose);
         } else {
            vertexConsumer = ItemRenderer.getItemGlintConsumer(this.bufferSource, this.defaultLayer, true, this.defaultGlint != Glint.NONE);
         }

         this.vanillaBufferer.accept(model, this.colors, this.lightmap, this.overlay, this.poseStack, vertexConsumer);
      }
   }

   private static VertexConsumer getDynamicDisplayGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, Entry entry) {
      return VertexConsumers.union(new OverlayVertexConsumer(provider.getBuffer(RenderLayer.getGlint()), entry, 0.0078125F), provider.getBuffer(layer));
   }

   public class ItemEmitter extends MutableQuadViewImpl {
      public ItemEmitter() {
         this.data = new int[EncodingFormat.TOTAL_STRIDE];
         this.clear();
      }

      public void bufferDefaultModel(BakedModel model) {
         ItemRenderContext.this.bufferDefaultModel(this, model, null);
      }

      @Override
      public void emitDirectly() {
         ItemRenderContext.this.renderQuad(this);
      }

      public boolean hasTransforms() {
         return this.activeTransform != NO_TRANSFORM;
      }
   }

   @FunctionalInterface
   public interface VanillaModelBufferer {
      void accept(BakedModel var1, int[] var2, int var3, int var4, MatrixStack var5, VertexConsumer var6);
   }
}
