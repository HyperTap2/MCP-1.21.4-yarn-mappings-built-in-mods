package net.minecraft.client.render.model;

import java.util.List;
import com.github.argon4w.acceleratedrendering.features.items.IAcceleratedBakedModel;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ItemRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public interface BakedModel extends FabricBakedModel, IAcceleratedBakedModel {
   List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random);

   boolean useAmbientOcclusion();

   boolean hasDepth();

   boolean isSideLit();

   Sprite getParticleSprite();

   ModelTransformation getTransformation();

   @Override
   default void emitItemQuads(QuadEmitter emitter, Supplier<Random> randomSupplier) {
      if (emitter instanceof ItemRenderContext.ItemEmitter itemEmitter && !itemEmitter.hasTransforms()) {
         itemEmitter.bufferDefaultModel(this);
      } else {
         FabricBakedModel.super.emitItemQuads(emitter, randomSupplier);
      }
   }

   @Override
   default void emitBlockQuads(
      QuadEmitter emitter, BlockRenderView blockView, BlockState state, BlockPos pos,
      Supplier<Random> randomSupplier, Predicate<@Nullable Direction> cullTest
   ) {
      if (emitter instanceof AbstractBlockRenderContext.BlockEmitter blockEmitter) {
         blockEmitter.bufferDefaultModel(this, state, cullTest);
      } else if (emitter instanceof ItemRenderContext.ItemEmitter itemEmitter && !itemEmitter.hasTransforms()) {
         itemEmitter.bufferDefaultModel(this);
      } else {
         FabricBakedModel.super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
      }
   }
}
