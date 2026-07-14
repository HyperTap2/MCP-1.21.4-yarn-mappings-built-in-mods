package net.minecraft.client.render.model;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.CulledMeshCollector;
import com.github.argon4w.acceleratedrendering.core.utils.DirectionUtils;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderContext;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderingFeature;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import malte0811.ferritecore.FerriteCoreDeduplicator;
import net.minecraft.block.BlockState;
import net.minecraft.client.model.SpriteGetter;
import net.minecraft.client.render.model.json.ModelElement;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

public class BasicBakedModel implements BakedModel, IAcceleratedRenderer<AcceleratedItemRenderContext> {
   public static final String PARTICLE_TEXTURE_ID = "particle";
   private final List<BakedQuad> quads;
   private final Map<Direction, List<BakedQuad>> faceQuads;
   private final boolean usesAo;
   private final boolean hasDepth;
   private final boolean isSideLit;
   private final Sprite sprite;
   private final ModelTransformation transformation;
   private final Map<IBufferGraph, Int2ObjectMap<IMesh>> acceleratedRendering$meshes = new Object2ObjectOpenHashMap<>();
   private long acceleratedRendering$resourceGeneration = -1L;

   public BasicBakedModel(
      List<BakedQuad> quads,
      Map<Direction, List<BakedQuad>> faceQuads,
      boolean usesAo,
      boolean isSideLit,
      boolean hasDepth,
      Sprite sprite,
      ModelTransformation transformation
   ) {
      this.quads = FerriteCoreDeduplicator.minimizeQuads(quads);
      this.faceQuads = FerriteCoreDeduplicator.minimizeSides(faceQuads);
      this.usesAo = usesAo;
      this.hasDepth = hasDepth;
      this.isSideLit = isSideLit;
      this.sprite = sprite;
      this.transformation = transformation;
   }

   public static BakedModel bake(
      List<ModelElement> elements,
      ModelTextures textures,
      SpriteGetter spriteGetter,
      ModelBakeSettings settings,
      boolean ambientOcclusion,
      boolean isSideLit,
      boolean hasDepth,
      ModelTransformation transformation
   ) {
      Sprite sprite = getSprite(spriteGetter, textures, "particle");
      BasicBakedModel.Builder builder = new BasicBakedModel.Builder(ambientOcclusion, isSideLit, hasDepth, transformation).setParticle(sprite);

      for (ModelElement modelElement : elements) {
         for (Direction direction : modelElement.faces.keySet()) {
            ModelElementFace modelElementFace = modelElement.faces.get(direction);
            Sprite sprite2 = getSprite(spriteGetter, textures, modelElementFace.textureId());
            if (modelElementFace.cullFace() == null) {
               builder.addQuad(bake(modelElement, modelElementFace, sprite2, direction, settings));
            } else {
               builder.addQuad(
                  Direction.transform(settings.getRotation().getMatrix(), modelElementFace.cullFace()),
                  bake(modelElement, modelElementFace, sprite2, direction, settings)
               );
            }
         }
      }

      return builder.build();
   }

   private static BakedQuad bake(ModelElement element, ModelElementFace face, Sprite sprite, Direction direction, ModelBakeSettings settings) {
      return BakedQuadFactory.bake(element.from, element.to, face, sprite, direction, settings, element.rotation, element.shade, element.lightEmission);
   }

   private static Sprite getSprite(SpriteGetter spriteGetter, ModelTextures textures, String textureId) {
      SpriteIdentifier spriteIdentifier = textures.get(textureId);
      return spriteIdentifier != null ? spriteGetter.get(spriteIdentifier) : spriteGetter.getMissing(textureId);
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
      return face == null ? this.quads : this.faceQuads.get(face);
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.usesAo;
   }

   @Override
   public boolean hasDepth() {
      return this.hasDepth;
   }

   @Override
   public boolean isSideLit() {
      return this.isSideLit;
   }

   @Override
   public Sprite getParticleSprite() {
      return this.sprite;
   }

   @Override
   public ModelTransformation getTransformation() {
      return this.transformation;
   }

   @Override
   public void renderItemFast(
      AcceleratedItemRenderContext context,
      MatrixStack.Entry pose,
      IAcceleratedVertexConsumer extension,
      int combinedLight,
      int combinedOverlay
   ) {
      extension.doRender(
         this,
         context,
         pose.getPositionMatrix(),
         pose.getNormalMatrix(),
         combinedLight,
         combinedOverlay,
         -1
      );
   }

   @Override
   public void render(
      VertexConsumer vertexConsumer,
      AcceleratedItemRenderContext context,
      Matrix4f transform,
      Matrix3f normal,
      int light,
      int overlay,
      int color
   ) {
      long generation = AcceleratedRendering.getResourceGeneration();
      if (this.acceleratedRendering$resourceGeneration != generation) {
         this.acceleratedRendering$meshes.clear();
         this.acceleratedRendering$resourceGeneration = generation;
      }

      IAcceleratedVertexConsumer extension = (IAcceleratedVertexConsumer)vertexConsumer;
      Int2ObjectMap<IMesh> layers = this.acceleratedRendering$meshes.get(extension);
      extension.beginTransform(transform, normal);

      try {
         if (layers == null) {
            layers = this.acceleratedRendering$buildMeshes(extension, overlay);
            this.acceleratedRendering$meshes.put(extension, layers);
         }

         for (Int2ObjectMap.Entry<IMesh> entry : layers.int2ObjectEntrySet()) {
            int tintIndex = entry.getIntKey();
            int tint = tintIndex == -1 ? -1 : context.getTint(tintIndex);
            entry.getValue().write(extension, tint, light, overlay);
         }
      } finally {
         extension.endTransform();
      }
   }

   private Int2ObjectMap<IMesh> acceleratedRendering$buildMeshes(IAcceleratedVertexConsumer extension, int overlay) {
      Int2ObjectMap<CulledMeshCollector> collectors = new Int2ObjectAVLTreeMap<>();
      Random random = Random.create();

      for (Direction direction : DirectionUtils.FULL) {
         random.setSeed(42L);
         for (BakedQuad quad : this.getQuads(null, direction, random)) {
            CulledMeshCollector collector = collectors.computeIfAbsent(
               quad.getTintIndex(),
               ignored -> new CulledMeshCollector(
                  extension.getRenderType(),
                  extension.getBufferSet().getBufferEnvironment().getLayout()
               )
            );
            VertexConsumer builder = extension.decorate(collector);
            int[] data = quad.getVertexData();

            for (int vertex = 0; vertex < data.length / 8; vertex++) {
               int offset = vertex * 8;
               int packedNormal = data[offset + 7];
               builder.vertex(
                  Float.intBitsToFloat(data[offset]),
                  Float.intBitsToFloat(data[offset + 1]),
                  Float.intBitsToFloat(data[offset + 2]),
                  ColorHelper.fromAbgr(data[offset + 3]),
                  Float.intBitsToFloat(data[offset + 4]),
                  Float.intBitsToFloat(data[offset + 5]),
                  overlay,
                  data[offset + 6],
                  (byte)(packedNormal & 0xFF) / 127.0F,
                  (byte)(packedNormal >> 8 & 0xFF) / 127.0F,
                  (byte)(packedNormal >> 16 & 0xFF) / 127.0F
               );
            }
         }
      }

      Int2ObjectMap<IMesh> layers = new Int2ObjectAVLTreeMap<>();
      for (Int2ObjectMap.Entry<CulledMeshCollector> entry : collectors.int2ObjectEntrySet()) {
         CulledMeshCollector collector = entry.getValue();
         collector.flush();
         layers.put(entry.getIntKey(), AcceleratedItemRenderingFeature.getMeshType().getBuilder().build(collector));
      }
      return layers;
   }

   @Override
   public boolean isAccelerated() {
      return true;
   }

   @Override
   public int getCustomColor(int layer, int color) {
      return layer == -1 ? -1 : color;
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
      private final EnumMap<Direction, com.google.common.collect.ImmutableList.Builder<BakedQuad>> faceQuads = Maps.newEnumMap(Direction.class);
      private final boolean usesAo;
      @Nullable
      private Sprite particleTexture;
      private final boolean isSideLit;
      private final boolean hasDepth;
      private final ModelTransformation transformation;

      public Builder(boolean usesAo, boolean isSideLit, boolean hasDepth, ModelTransformation transformation) {
         this.usesAo = usesAo;
         this.isSideLit = isSideLit;
         this.hasDepth = hasDepth;
         this.transformation = transformation;

         for (Direction direction : Direction.values()) {
            this.faceQuads.put(direction, ImmutableList.builder());
         }
      }

      public BasicBakedModel.Builder addQuad(Direction side, BakedQuad quad) {
         this.faceQuads.get(side).add(quad);
         return this;
      }

      public BasicBakedModel.Builder addQuad(BakedQuad quad) {
         this.quads.add(quad);
         return this;
      }

      public BasicBakedModel.Builder setParticle(Sprite sprite) {
         this.particleTexture = sprite;
         return this;
      }

      public BasicBakedModel.Builder method_35809() {
         return this;
      }

      public BakedModel build() {
         if (this.particleTexture == null) {
            throw new RuntimeException("Missing particle!");
         }

         Map<Direction, List<BakedQuad>> map = Maps.transformValues(this.faceQuads, com.google.common.collect.ImmutableList.Builder::build);
         return new BasicBakedModel(
            this.quads.build(), new EnumMap<>(map), this.usesAo, this.isSideLit, this.hasDepth, this.particleTexture, this.transformation
         );
      }
   }
}
