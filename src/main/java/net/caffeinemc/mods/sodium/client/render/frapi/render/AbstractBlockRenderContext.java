package net.caffeinemc.mods.sodium.client.render.frapi.render;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBlockRenderContext extends AbstractRenderContext {
   private static final RenderMaterial[] STANDARD_MATERIALS = new RenderMaterial[AmbientOcclusionMode.values().length];
   private static final RenderMaterial TRANSLUCENT_MATERIAL = SodiumRenderer.INSTANCE.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
   private final AbstractBlockRenderContext.BlockEmitter editorQuad = new AbstractBlockRenderContext.BlockEmitter();
   protected BlockRenderView level;
   protected LevelSlice slice;
   protected BlockState state;
   protected BlockPos pos;
   protected RenderLayer type;
   protected SodiumModelData modelData;
   private final BlockOcclusionCache occlusionCache = new BlockOcclusionCache();
   private boolean enableCulling = true;
   private int cullCompletionFlags;
   private int cullResultFlags;
   protected Random random;
   protected long randomSeed;
   protected final Supplier<Random> randomSupplier = () -> {
      this.random.setSeed(this.randomSeed);
      return this.random;
   };
   protected LightPipelineProvider lighters;
   protected final QuadLightData quadLightData = new QuadLightData();
   protected boolean useAmbientOcclusion;
   protected LightMode defaultLightMode;

   @Override
   public QuadEmitter getEmitter() {
      this.editorQuad.clear();
      return this.editorQuad;
   }

   public boolean isFaceCulled(@Nullable Direction face) {
      if (face != null && this.enableCulling) {
         int mask = 1 << face.getId();
         if ((this.cullCompletionFlags & mask) == 0) {
            this.cullCompletionFlags |= mask;
            if (this.occlusionCache.shouldDrawSide(this.state, this.level, this.pos, face)) {
               this.cullResultFlags |= mask;
               return false;
            } else {
               return true;
            }
         } else {
            return (this.cullResultFlags & mask) == 0;
         }
      } else {
         return false;
      }
   }

   private void renderQuad(MutableQuadViewImpl quad) {
      if (!this.isFaceCulled(quad.cullFace())) {
         this.processQuad(quad);
      }
   }

   protected abstract void processQuad(MutableQuadViewImpl var1);

   protected void prepareCulling(boolean enableCulling) {
      this.enableCulling = enableCulling;
      this.cullCompletionFlags = 0;
      this.cullResultFlags = 0;
   }

   protected void prepareAoInfo(boolean modelAo) {
      this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
      this.defaultLightMode = this.useAmbientOcclusion && modelAo && PlatformBlockAccess.getInstance().getLightEmission(this.state, this.level, this.pos) == 0
         ? LightMode.SMOOTH
         : LightMode.FLAT;
   }

   protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode) {
      LightPipeline lighter = this.lighters.getLighter(lightMode);
      QuadLightData data = this.quadLightData;
      lighter.calculate(quad, this.pos, data, quad.cullFace(), quad.lightFace(), quad.hasShade(), shadeMode == ShadeMode.ENHANCED);
      if (emissive) {
         for (int i = 0; i < 4; i++) {
            quad.lightmap(i, 15728880);
         }
      } else {
         int[] lightmaps = data.lm;

         for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmaps[i]));
         }
      }
   }

   public void bufferDefaultModel(BakedModel model, @Nullable BlockState state, Predicate<Direction> cullTest) {
      MutableQuadViewImpl editorQuad = this.editorQuad;

      for (int i = 0; i <= 6; i++) {
         Direction cullFace = ModelHelper.faceFromIndex(i);
         if (!cullTest.test(cullFace)) {
            Random random = this.randomSupplier.get();
            AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(model, state, this.modelData, this.type, this.slice, this.pos);
            List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(this.level, this.pos, model, state, cullFace, random, this.type, this.modelData);
            int count = quads.size();

            for (int j = 0; j < count; j++) {
               BakedQuad q = quads.get(j);
               editorQuad.fromVanilla(
                  q,
                  this.type != RenderLayer.getTripwire() && this.type != RenderLayer.getTranslucent() ? STANDARD_MATERIALS[ao.ordinal()] : TRANSLUCENT_MATERIAL,
                  cullFace
               );
               editorQuad.transformAndEmit();
            }
         }
      }

      editorQuad.clear();
   }

   public SodiumModelData getModelData() {
      return this.modelData;
   }

   public RenderLayer getRenderType() {
      return this.type;
   }

   static {
      AmbientOcclusionMode[] values = AmbientOcclusionMode.values();

      for (int i = 0; i < values.length; i++) {
         TriState state = switch (values[i]) {
            case ENABLED -> TriState.TRUE;
            case DISABLED -> TriState.FALSE;
            case DEFAULT -> TriState.DEFAULT;
         };
         STANDARD_MATERIALS[i] = SodiumRenderer.INSTANCE.materialFinder().ambientOcclusion(state).find();
      }
   }

   public class BlockEmitter extends MutableQuadViewImpl {
      public BlockEmitter() {
         this.data = new int[EncodingFormat.TOTAL_STRIDE];
         this.clear();
      }

      public void bufferDefaultModel(BakedModel model, BlockState state, Predicate<Direction> cullTest) {
         AbstractBlockRenderContext.this.bufferDefaultModel(model, state, cullTest);
      }

      @Override
      public void emitDirectly() {
         if (AbstractBlockRenderContext.this.type == null) {
            throw new IllegalStateException("No render type is set but an FRAPI object was asked to render!");
         } else {
            AbstractBlockRenderContext.this.renderQuad(this);
         }
      }
   }
}
