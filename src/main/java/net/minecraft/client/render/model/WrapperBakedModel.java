package net.minecraft.client.render.model;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderContext;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public abstract class WrapperBakedModel implements BakedModel {
   protected final BakedModel wrapped;

   public WrapperBakedModel(BakedModel wrapped) {
      this.wrapped = wrapped;
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
      return this.wrapped.getQuads(state, face, random);
   }

   @Override
   public boolean useAmbientOcclusion() {
      return this.wrapped.useAmbientOcclusion();
   }

   @Override
   public boolean hasDepth() {
      return this.wrapped.hasDepth();
   }

   @Override
   public boolean isSideLit() {
      return this.wrapped.isSideLit();
   }

   @Override
   public Sprite getParticleSprite() {
      return this.wrapped.getParticleSprite();
   }

   @Override
   public ModelTransformation getTransformation() {
      return this.wrapped.getTransformation();
   }

   @Override
   public boolean isVanillaAdapter() {
      return this.wrapped.isVanillaAdapter();
   }

   @Override
   public void emitBlockQuads(
      QuadEmitter emitter,
      BlockRenderView blockView,
      BlockState state,
      BlockPos pos,
      Supplier<Random> randomSupplier,
      Predicate<@Nullable Direction> cullTest
   ) {
      this.wrapped.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
   }

   @Override
   public void emitItemQuads(QuadEmitter emitter, Supplier<Random> randomSupplier) {
      this.wrapped.emitItemQuads(emitter, randomSupplier);
   }

   @Override
   public void renderItemFast(
      AcceleratedItemRenderContext context,
      MatrixStack.Entry pose,
      IAcceleratedVertexConsumer extension,
      int combinedLight,
      int combinedOverlay
   ) {
      this.wrapped.renderItemFast(context.withBakedModel(this.wrapped), pose, extension, combinedLight, combinedOverlay);
   }

   @Override
   public int getCustomColor(int layer, int color) {
      return this.wrapped.getCustomColor(layer, color);
   }

   @Override
   public boolean isAccelerated() {
      return this.wrapped.isAccelerated();
   }

   @Override
   public boolean isAcceleratedInHand() {
      return this.wrapped.isAcceleratedInHand();
   }

   @Override
   public boolean isAcceleratedInGui() {
      return this.wrapped.isAcceleratedInGui();
   }
}
