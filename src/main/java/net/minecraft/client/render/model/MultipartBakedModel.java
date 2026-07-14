package net.minecraft.client.render.model;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.features.items.AcceleratedItemRenderContext;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;

public class MultipartBakedModel extends WrapperBakedModel {
   private final List<MultipartBakedModel.Selector> selectors;
   private final Map<BlockState, BitSet> stateCache = new Reference2ObjectOpenHashMap();
   private final boolean accelerated;
   private final boolean vanillaAdapter;

   private static BakedModel getFirst(List<MultipartBakedModel.Selector> selectors) {
      if (selectors.isEmpty()) {
         throw new IllegalArgumentException("Model must have at least one selector");
      } else {
         return selectors.getFirst().model();
      }
   }

   public MultipartBakedModel(List<MultipartBakedModel.Selector> selectors) {
      super(getFirst(selectors));
      this.selectors = selectors;
      this.accelerated = selectors.stream().allMatch(selector -> selector.model().isAccelerated());
      this.vanillaAdapter = selectors.stream().allMatch(selector -> selector.model().isVanillaAdapter());
   }

   @Override
   public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
      if (state == null) {
         return Collections.emptyList();
      }

      BitSet bitSet;
      synchronized (this.stateCache) {
         bitSet = this.stateCache.get(state);
         if (bitSet == null) {
            bitSet = new BitSet();

            for (int i = 0; i < this.selectors.size(); i++) {
               if (this.selectors.get(i).condition.test(state)) {
                  bitSet.set(i);
               }
            }

            this.stateCache.put(state, bitSet);
         }
      }

      List<BakedQuad> list = new ArrayList<>();
      long l = random.nextLong();

      for (int j = 0; j < bitSet.length(); j++) {
         if (bitSet.get(j)) {
            random.setSeed(l);
            list.addAll(this.selectors.get(j).model.getQuads(state, face, random));
         }
      }

      return list;
   }

   @Override
   public boolean isAccelerated() {
      return this.accelerated;
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
      BitSet bitSet;
      synchronized (this.stateCache) {
         bitSet = this.stateCache.get(state);
         if (bitSet == null) {
            bitSet = new BitSet();
            for (int i = 0; i < this.selectors.size(); i++) {
               if (this.selectors.get(i).condition().test(state)) {
                  bitSet.set(i);
               }
            }
            this.stateCache.put(state, bitSet);
         }
      }

      Random random = randomSupplier.get();
      long randomSeed = random.nextLong();
      Supplier<Random> subModelRandomSupplier = () -> {
         random.setSeed(randomSeed);
         return random;
      };
      for (int i = 0; i < this.selectors.size(); i++) {
         if (bitSet.get(i)) {
            this.selectors.get(i).model().emitBlockQuads(emitter, blockView, state, pos, subModelRandomSupplier, cullTest);
         }
      }
   }

   @Override
   public void emitItemQuads(QuadEmitter emitter, Supplier<Random> randomSupplier) {
      // Vanilla does not use multipart baked models for items.
   }

   @Override
   public void renderItemFast(
      AcceleratedItemRenderContext context,
      MatrixStack.Entry pose,
      IAcceleratedVertexConsumer extension,
      int combinedLight,
      int combinedOverlay
   ) {
      // Multipart block models intentionally emit no item quads when queried with a null state.
   }

   public record Selector(Predicate<BlockState> condition, BakedModel model) {
   }
}
