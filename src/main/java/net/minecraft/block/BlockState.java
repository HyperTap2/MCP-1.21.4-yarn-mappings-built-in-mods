package net.minecraft.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class BlockState extends AbstractBlock.AbstractBlockState {
   public static final Codec<BlockState> CODEC = createCodec(Registries.BLOCK.getCodec(), Block::getDefaultState).stable();

   public BlockState(Block block, Reference2ObjectArrayMap<Property<?>, Comparable<?>> reference2ObjectArrayMap, MapCodec<BlockState> mapCodec) {
      super(block, reference2ObjectArrayMap, mapCodec);
   }

   @Override
   protected BlockState asBlockState() {
      return this;
   }

   public BlockState getAppearance(
      BlockRenderView blockView, BlockPos pos, Direction face, BlockState sourceState, BlockPos sourcePos
   ) {
      return this;
   }
}
