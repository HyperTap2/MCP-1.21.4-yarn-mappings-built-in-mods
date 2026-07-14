package net.minecraft.client.render.item;

import java.util.List;
import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderContext;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.github.argon4w.acceleratedrendering.features.items.IAcceleratedBakedQuad;
import com.github.argon4w.acceleratedrendering.features.items.IAcceleratedBakedModel;
import com.github.argon4w.acceleratedrendering.core.utils.DirectionUtils;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemModelCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ItemRenderContext;
import org.jetbrains.annotations.Nullable;

public class ItemRenderer {
   private static final IAcceleratedRenderer<AcceleratedItemRenderContext> ACCELERATED_ITEM_RENDERER = ItemRenderer::renderAcceleratedItemModel;
   private static final ThreadLocal<ItemRenderContext> CONTINUITY_CONTEXTS = ThreadLocal.withInitial(
      () -> new ItemRenderContext(ItemRenderer::renderBakedItemModel)
   );
   public static final Identifier ENTITY_ENCHANTMENT_GLINT = Identifier.ofVanilla("textures/misc/enchanted_glint_entity.png");
   public static final Identifier ITEM_ENCHANTMENT_GLINT = Identifier.ofVanilla("textures/misc/enchanted_glint_item.png");
   public static final int field_32937 = 8;
   public static final int field_32938 = 8;
   public static final int field_32934 = 200;
   public static final float COMPASS_WITH_GLINT_GUI_MODEL_MULTIPLIER = 0.5F;
   public static final float COMPASS_WITH_GLINT_FIRST_PERSON_MODEL_MULTIPLIER = 0.75F;
   public static final float field_41120 = 0.0078125F;
   public static final int field_55295 = -1;
   private final ItemModelManager itemModelManager;
   private final ItemRenderState itemRenderState = new ItemRenderState();

   public ItemRenderer(ItemModelManager itemModelManager) {
      this.itemModelManager = itemModelManager;
   }

   private static void renderBakedItemModel(BakedModel model, int[] tints, int light, int overlay, MatrixStack matrices, VertexConsumer vertexConsumer) {
      IAcceleratedBakedModel acceleratedModel = model;
      boolean acceleratedContext = CoreFeature.isRenderingLevel()
         || CoreFeature.isRenderingHand()
            && (acceleratedModel.isAcceleratedInHand() || AcceleratedItemRenderingFeature.shouldAccelerateInHand())
         || CoreFeature.isRenderingGui()
            && (acceleratedModel.isAcceleratedInGui() || AcceleratedItemRenderingFeature.shouldAccelerateInGui());
      if (AcceleratedRendering.isAvailable()
         && acceleratedContext
         && AcceleratedItemRenderingFeature.isEnabled()
         && AcceleratedItemRenderingFeature.shouldUseAcceleratedPipeline()
         && vertexConsumer.isAccelerated()) {
         MatrixStack.Entry entry = matrices.peek();
         AcceleratedItemRenderContext context = new AcceleratedItemRenderContext(ItemStack.EMPTY, model, Random.create(42L), tints);
         if (acceleratedModel.isAccelerated()) {
            acceleratedModel.renderItemFast(context, entry, vertexConsumer, light, overlay);
         } else {
            vertexConsumer.doRender(
               ACCELERATED_ITEM_RENDERER,
               context,
               entry.getPositionMatrix(),
               entry.getNormalMatrix(),
               light,
               overlay,
               -1
            );
         }
         return;
      }

      List<BakedQuad> cachedQuads = AcceleratedItemModelCache.getQuads(model);
      if (cachedQuads != null) {
         renderBakedItemQuads(matrices, vertexConsumer, cachedQuads, tints, light, overlay);
         return;
      }

      Random random = Random.create();
      long l = 42L;

      for (Direction direction : Direction.values()) {
         random.setSeed(42L);
         renderBakedItemQuads(matrices, vertexConsumer, model.getQuads(null, direction, random), tints, light, overlay);
      }

      random.setSeed(42L);
      renderBakedItemQuads(matrices, vertexConsumer, model.getQuads(null, null, random), tints, light, overlay);
   }

   private static void renderAcceleratedItemModel(
      VertexConsumer vertexConsumer,
      AcceleratedItemRenderContext context,
      org.joml.Matrix4f transform,
      org.joml.Matrix3f normal,
      int light,
      int overlay,
      int color
   ) {
      vertexConsumer.beginTransform(transform, normal);
      try {
         Random random = context.getRandom();
         for (Direction direction : DirectionUtils.FULL) {
            random.setSeed(42L);
            for (BakedQuad quad : context.getBakedModel().getQuads(null, direction, random)) {
               int tint = quad.hasTint() ? context.getTint(quad.getTintIndex()) : -1;
               ((IAcceleratedBakedQuad)quad).renderFast(transform, normal, vertexConsumer, light, overlay, tint);
            }
         }
      } finally {
         vertexConsumer.endTransform();
      }
   }

   public static void renderItem(
      ModelTransformationMode transformationMode,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      int[] tints,
      BakedModel model,
      RenderLayer layer,
      ItemRenderState.Glint glint
   ) {
      if (!model.isVanillaAdapter()) {
         CONTINUITY_CONTEXTS.get().renderModel(transformationMode, matrices, vertexConsumers, light, overlay, model, tints, layer, glint);
         return;
      }
      VertexConsumer vertexConsumer;
      if (glint == ItemRenderState.Glint.SPECIAL) {
         MatrixStack.Entry entry = matrices.peek().copy();
         if (transformationMode == ModelTransformationMode.GUI) {
            MatrixUtil.scale(entry.getPositionMatrix(), 0.5F);
         } else if (transformationMode.isFirstPerson()) {
            MatrixUtil.scale(entry.getPositionMatrix(), 0.75F);
         }

         vertexConsumer = getDynamicDisplayGlintConsumer(vertexConsumers, layer, entry);
      } else {
         vertexConsumer = getItemGlintConsumer(vertexConsumers, layer, true, glint != ItemRenderState.Glint.NONE);
      }

      renderBakedItemModel(model, tints, light, overlay, matrices, vertexConsumer);
   }

   public static VertexConsumer getArmorGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, boolean glint) {
      return glint ? VertexConsumers.union(provider.getBuffer(RenderLayer.getArmorEntityGlint()), provider.getBuffer(layer)) : provider.getBuffer(layer);
   }

   private static VertexConsumer getDynamicDisplayGlintConsumer(VertexConsumerProvider provider, RenderLayer layer, MatrixStack.Entry entry) {
      return VertexConsumers.union(new OverlayVertexConsumer(provider.getBuffer(RenderLayer.getGlint()), entry, 0.0078125F), provider.getBuffer(layer));
   }

   public static VertexConsumer getItemGlintConsumer(VertexConsumerProvider vertexConsumers, RenderLayer layer, boolean solid, boolean glint) {
      if (glint) {
         return MinecraftClient.isFabulousGraphicsOrBetter() && layer == TexturedRenderLayers.getItemEntityTranslucentCull()
            ? VertexConsumers.union(vertexConsumers.getBuffer(RenderLayer.getGlintTranslucent()), vertexConsumers.getBuffer(layer))
            : VertexConsumers.union(vertexConsumers.getBuffer(solid ? RenderLayer.getGlint() : RenderLayer.getEntityGlint()), vertexConsumers.getBuffer(layer));
      } else {
         return vertexConsumers.getBuffer(layer);
      }
   }

   private static int getTint(int[] tints, int index) {
      return index >= tints.length ? -1 : tints[index];
   }

   private static void renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, int light, int overlay) {
      MatrixStack.Entry entry = matrices.peek();

      for (BakedQuad bakedQuad : quads) {
         float f;
         float g;
         float h;
         float j;
         if (bakedQuad.hasTint()) {
            int i = getTint(tints, bakedQuad.getTintIndex());
            f = ColorHelper.getAlpha(i) / 255.0F;
            g = ColorHelper.getRed(i) / 255.0F;
            h = ColorHelper.getGreen(i) / 255.0F;
            j = ColorHelper.getBlue(i) / 255.0F;
         } else {
            f = 1.0F;
            g = 1.0F;
            h = 1.0F;
            j = 1.0F;
         }

         vertexConsumer.quad(entry, bakedQuad, g, h, j, f, light, overlay);
      }
   }

   public void renderItem(
      ItemStack stack,
      ModelTransformationMode transformationMode,
      int light,
      int overlay,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      @Nullable World world,
      int seed
   ) {
      this.renderItem(null, stack, transformationMode, false, matrices, vertexConsumers, world, light, overlay, seed);
   }

   public void renderItem(
      @Nullable LivingEntity entity,
      ItemStack stack,
      ModelTransformationMode transformationMode,
      boolean leftHanded,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      @Nullable World world,
      int light,
      int overlay,
      int seed
   ) {
      this.itemModelManager.update(this.itemRenderState, stack, transformationMode, leftHanded, world, entity, seed);
      this.itemRenderState.render(matrices, vertexConsumers, light, overlay);
   }
}
