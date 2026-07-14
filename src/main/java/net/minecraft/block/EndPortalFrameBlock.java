package net.minecraft.block;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.predicate.block.BlockStatePredicate;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class EndPortalFrameBlock extends Block {
   public static final MapCodec<EndPortalFrameBlock> CODEC = createCodec(EndPortalFrameBlock::new);
   public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
   public static final BooleanProperty EYE = Properties.EYE;
   protected static final VoxelShape FRAME_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);
   protected static final VoxelShape EYE_SHAPE = Block.createCuboidShape(4.0, 13.0, 4.0, 12.0, 16.0, 12.0);
   protected static final VoxelShape FRAME_WITH_EYE_SHAPE = VoxelShapes.union(FRAME_SHAPE, EYE_SHAPE);
   private static final VoxelShape VIA_EYE_SHAPE_1_12_2 = Block.createCuboidShape(5.0, 13.0, 5.0, 11.0, 16.0, 11.0);
   private static final VoxelShape VIA_FRAME_WITH_EYE_SHAPE_1_12_2 = VoxelShapes.union(FRAME_SHAPE, VIA_EYE_SHAPE_1_12_2);
   private static BlockPattern COMPLETED_FRAME;

   @Override
   public MapCodec<EndPortalFrameBlock> getCodec() {
      return CODEC;
   }

   public EndPortalFrameBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH).with(EYE, false));
   }

   @Override
   protected boolean hasSidedTransparency(BlockState state) {
      return true;
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)
         ? FRAME_SHAPE
         : state.get(EYE) ? FRAME_WITH_EYE_SHAPE : FRAME_SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return state.get(EYE) ? VIA_FRAME_WITH_EYE_SHAPE_1_12_2 : FRAME_SHAPE;
      }

      return super.getCollisionShape(state, world, pos, context);
   }

   @Override
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()).with(EYE, false);
   }

   @Override
   protected boolean hasComparatorOutput(BlockState state) {
      return true;
   }

   @Override
   protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
      return state.get(EYE) ? 15 : 0;
   }

   @Override
   protected BlockState rotate(BlockState state, BlockRotation rotation) {
      return state.with(FACING, rotation.rotate(state.get(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation(state.get(FACING)));
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, EYE);
   }

   public static BlockPattern getCompletedFramePattern() {
      if (COMPLETED_FRAME == null) {
         COMPLETED_FRAME = BlockPatternBuilder.start()
            .aisle("?vvv?", ">???<", ">???<", ">???<", "?^^^?")
            .where('?', CachedBlockPosition.matchesBlockState(BlockStatePredicate.ANY))
            .where(
               '^',
               CachedBlockPosition.matchesBlockState(
                  BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.SOUTH))
               )
            )
            .where(
               '>',
               CachedBlockPosition.matchesBlockState(
                  BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.WEST))
               )
            )
            .where(
               'v',
               CachedBlockPosition.matchesBlockState(
                  BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.NORTH))
               )
            )
            .where(
               '<',
               CachedBlockPosition.matchesBlockState(
                  BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME).with(EYE, Predicates.equalTo(true)).with(FACING, Predicates.equalTo(Direction.EAST))
               )
            )
            .build();
      }

      return COMPLETED_FRAME;
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }
}
