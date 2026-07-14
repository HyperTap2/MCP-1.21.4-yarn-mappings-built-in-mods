package net.minecraft.block;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

public class PaneBlock extends HorizontalConnectingBlock {
   public static final MapCodec<PaneBlock> CODEC = createCodec(PaneBlock::new);
   private final VoxelShape[] viaShapes1_8;

   @Override
   public MapCodec<? extends PaneBlock> getCodec() {
      return CODEC;
   }

   public PaneBlock(AbstractBlock.Settings settings) {
      super(1.0F, 1.0F, 16.0F, 16.0F, 16.0F, settings);
      this.setDefaultState(
         this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false)
      );
      this.viaShapes1_8 = createViaShapes1_8();
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)
         ? this.viaShapes1_8[this.getShapeIndex(state)]
         : super.getOutlineShape(state, world, pos, context);
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)
         ? this.viaShapes1_8[this.getShapeIndex(state)]
         : super.getCollisionShape(state, world, pos, context);
   }

   private static VoxelShape[] createViaShapes1_8() {
      VoxelShape baseShape = Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
      VoxelShape northShape = Block.createCuboidShape(7.0, 0.0, 0.0, 9.0, 16.0, 9.0);
      VoxelShape southShape = Block.createCuboidShape(7.0, 0.0, 7.0, 9.0, 16.0, 16.0);
      VoxelShape westShape = Block.createCuboidShape(0.0, 0.0, 7.0, 9.0, 16.0, 9.0);
      VoxelShape eastShape = Block.createCuboidShape(7.0, 0.0, 7.0, 16.0, 16.0, 9.0);
      VoxelShape northEastCornerShape = VoxelShapes.union(northShape, eastShape);
      VoxelShape southWestCornerShape = VoxelShapes.union(southShape, westShape);
      VoxelShape[] shapes = new VoxelShape[]{
         VoxelShapes.empty(),
         Block.createCuboidShape(7.0, 0.0, 8.0, 9.0, 16.0, 16.0),
         Block.createCuboidShape(0.0, 0.0, 7.0, 8.0, 16.0, 9.0),
         southWestCornerShape,
         Block.createCuboidShape(7.0, 0.0, 0.0, 9.0, 16.0, 8.0),
         VoxelShapes.union(southShape, northShape),
         VoxelShapes.union(westShape, northShape),
         VoxelShapes.union(southWestCornerShape, northShape),
         Block.createCuboidShape(8.0, 0.0, 7.0, 16.0, 16.0, 9.0),
         VoxelShapes.union(southShape, eastShape),
         VoxelShapes.union(westShape, eastShape),
         VoxelShapes.union(southWestCornerShape, eastShape),
         northEastCornerShape,
         VoxelShapes.union(southShape, northEastCornerShape),
         VoxelShapes.union(westShape, northEastCornerShape),
         VoxelShapes.union(southWestCornerShape, northEastCornerShape)
      };

      for (int i = 0; i < shapes.length; i++) {
         if (i != 1 && i != 2 && i != 4 && i != 8) {
            shapes[i] = VoxelShapes.union(baseShape, shapes[i]);
         }
      }

      return shapes;
   }

   @Override
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      BlockView blockView = ctx.getWorld();
      BlockPos blockPos = ctx.getBlockPos();
      FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
      BlockPos blockPos2 = blockPos.north();
      BlockPos blockPos3 = blockPos.south();
      BlockPos blockPos4 = blockPos.west();
      BlockPos blockPos5 = blockPos.east();
      BlockState blockState = blockView.getBlockState(blockPos2);
      BlockState blockState2 = blockView.getBlockState(blockPos3);
      BlockState blockState3 = blockView.getBlockState(blockPos4);
      BlockState blockState4 = blockView.getBlockState(blockPos5);
      return this.getDefaultState()
         .with(NORTH, this.connectsTo(blockState, blockState.isSideSolidFullSquare(blockView, blockPos2, Direction.SOUTH)))
         .with(SOUTH, this.connectsTo(blockState2, blockState2.isSideSolidFullSquare(blockView, blockPos3, Direction.NORTH)))
         .with(WEST, this.connectsTo(blockState3, blockState3.isSideSolidFullSquare(blockView, blockPos4, Direction.EAST)))
         .with(EAST, this.connectsTo(blockState4, blockState4.isSideSolidFullSquare(blockView, blockPos5, Direction.WEST)))
         .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
   }

   @Override
   protected BlockState getStateForNeighborUpdate(
      BlockState state,
      WorldView world,
      ScheduledTickView tickView,
      BlockPos pos,
      Direction direction,
      BlockPos neighborPos,
      BlockState neighborState,
      Random random
   ) {
      if (state.get(WATERLOGGED)) {
         tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return direction.getAxis().isHorizontal()
         ? state.with(
            FACING_PROPERTIES.get(direction), this.connectsTo(neighborState, neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite()))
         )
         : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return VoxelShapes.empty();
   }

   @Override
   protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
      if (stateFrom.isOf(this)) {
         if (!direction.getAxis().isHorizontal()) {
            return true;
         }

         if (state.get(FACING_PROPERTIES.get(direction)) && stateFrom.get(FACING_PROPERTIES.get(direction.getOpposite()))) {
            return true;
         }
      }

      return super.isSideInvisible(state, stateFrom, direction);
   }

   public final boolean connectsTo(BlockState state, boolean sideSolidFullSquare) {
      return !cannotConnect(state) && sideSolidFullSquare || state.getBlock() instanceof PaneBlock || state.isIn(BlockTags.WALLS);
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
