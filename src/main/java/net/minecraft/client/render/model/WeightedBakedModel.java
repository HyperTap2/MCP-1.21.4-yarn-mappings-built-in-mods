package net.minecraft.client.render.model;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderContext;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.collection.Weighted.Present;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public class WeightedBakedModel extends WrapperBakedModel {
   private final DataPool<BakedModel> models;
   private final boolean accelerated;
   private final boolean acceleratedInHand;
   private final boolean acceleratedInGui;
   private final boolean vanillaAdapter;

   public WeightedBakedModel(DataPool<BakedModel> models) {
      super((BakedModel)((Present)models.getEntries().getFirst()).data());
      this.models = models;
      this.accelerated = models.getEntries().stream().allMatch(entry -> entry.data().isAccelerated());
      this.acceleratedInHand = models.getEntries().stream().allMatch(entry -> entry.data().isAcceleratedInHand());
      this.acceleratedInGui = models.getEntries().stream().allMatch(entry -> entry.data().isAcceleratedInGui());
      this.vanillaAdapter = models.getEntries().stream().allMatch(entry -> entry.data().isVanillaAdapter());
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
      return this.models.getDataOrEmpty(random).map(model -> model.getQuads(state, face, random)).orElse(Collections.emptyList());
   }

   @Override
   public boolean isVanillaAdapter() {
      return this.vanillaAdapter;
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
      this.models.getDataOrEmpty(randomSupplier.get()).ifPresent(model -> model.emitBlockQuads(
         emitter,
         blockView,
         state,
         pos,
         () -> {
            Random random = randomSupplier.get();
            random.nextInt();
            return random;
         },
         cullTest
      ));
   }

   @Override
   public void emitItemQuads(QuadEmitter emitter, Supplier<Random> randomSupplier) {
      this.models.getDataOrEmpty(randomSupplier.get()).ifPresent(model -> model.emitItemQuads(emitter, () -> {
         Random random = randomSupplier.get();
         random.nextInt();
         return random;
      }));
   }

   @Override
   public void renderItemFast(
      AcceleratedItemRenderContext context,
      MatrixStack.Entry pose,
      IAcceleratedVertexConsumer extension,
      int combinedLight,
      int combinedOverlay
   ) {
      this.models.getDataOrEmpty(context.getRandom()).ifPresent(
         model -> model.renderItemFast(context.withBakedModel(model), pose, extension, combinedLight, combinedOverlay)
      );
   }

   @Override
   public boolean isAccelerated() {
      return this.accelerated;
   }

   @Override
   public boolean isAcceleratedInHand() {
      return this.acceleratedInHand;
   }

   @Override
   public boolean isAcceleratedInGui() {
      return this.acceleratedInGui;
   }
}
