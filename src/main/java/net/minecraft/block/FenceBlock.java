package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LeadItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public class FenceBlock extends HorizontalConnectingBlock {
   public static final MapCodec<FenceBlock> CODEC = createCodec(FenceBlock::new);
   private final VoxelShape[] cullingShapes;
   private static final VoxelShape VIA_COLLISION_SHAPE_BETA_1_8_1 = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 24.0, 16.0);
   private final VoxelShape[] viaCollisionShapes1_4_7;
   private final VoxelShape[] viaOutlineShapes1_4_7;

   @Override
   public MapCodec<FenceBlock> getCodec() {
      return CODEC;
   }

   public FenceBlock(AbstractBlock.Settings settings) {
      super(2.0F, 2.0F, 16.0F, 16.0F, 24.0F, settings);
      this.setDefaultState(
         this.stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false)
      );
      this.cullingShapes = this.createShapes(2.0F, 1.0F, 16.0F, 6.0F, 15.0F);
      this.viaCollisionShapes1_4_7 = createViaShapes1_4_7(24.0F);
      this.viaOutlineShapes1_4_7 = createViaShapes1_4_7(16.0F);
   }

   @Override
   protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return VoxelShapes.fullCube();
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
         return this.viaOutlineShapes1_4_7[this.getShapeIndex(state)];
      } else {
         return super.getOutlineShape(state, world, pos, context);
      }
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return VIA_COLLISION_SHAPE_BETA_1_8_1;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
         return this.viaCollisionShapes1_4_7[this.getShapeIndex(state)];
      } else {
         return super.getCollisionShape(state, world, pos, context);
      }
   }

   private static VoxelShape[] createViaShapes1_4_7(float height) {
      VoxelShape baseShape = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, height, 10.0);
      VoxelShape northShape = Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, height, 10.0);
      VoxelShape southShape = Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, height, 16.0);
      VoxelShape westShape = Block.createCuboidShape(0.0, 0.0, 6.0, 10.0, height, 10.0);
      VoxelShape eastShape = Block.createCuboidShape(6.0, 0.0, 6.0, 16.0, height, 10.0);
      VoxelShape[] shapes = new VoxelShape[]{
         VoxelShapes.empty(),
         Block.createCuboidShape(6.0, 0.0, 6.0, 10.0, height, 16.0),
         Block.createCuboidShape(0.0, 0.0, 6.0, 10.0, height, 10.0),
         Block.createCuboidShape(0.0, 0.0, 6.0, 10.0, height, 16.0),
         Block.createCuboidShape(6.0, 0.0, 0.0, 10.0, height, 10.0),
         VoxelShapes.union(southShape, northShape),
         Block.createCuboidShape(0.0, 0.0, 0.0, 10.0, height, 10.0),
         Block.createCuboidShape(0.0, 0.0, 1.0, 10.0, height, 16.0),
         Block.createCuboidShape(6.0, 0.0, 6.0, 16.0, height, 10.0),
         Block.createCuboidShape(6.0, 0.0, 6.0, 16.0, height, 16.0),
         VoxelShapes.union(westShape, eastShape),
         Block.createCuboidShape(1.0, 0.0, 6.0, 16.0, height, 16.0),
         Block.createCuboidShape(6.0, 0.0, 0.0, 16.0, height, 10.0),
         Block.createCuboidShape(6.0, 0.0, 0.0, 16.0, height, 15.0),
         Block.createCuboidShape(1.0, 0.0, 0.0, 16.0, height, 10.0),
         Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, height, 16.0)
      };

      for (int i = 0; i < shapes.length; i++) {
         shapes[i] = VoxelShapes.union(baseShape, shapes[i]);
      }

      return shapes;
   }

   @Override
   protected VoxelShape getCullingShape(BlockState state) {
      return this.cullingShapes[this.getShapeIndex(state)];
   }

   @Override
   protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return this.getOutlineShape(state, world, pos, context);
   }

   @Override
   protected boolean canPathfindThrough(BlockState state, NavigationType type) {
      return false;
   }

   public boolean canConnect(BlockState state, boolean neighborIsFullSquare, Direction dir) {
      Block block = state.getBlock();
      boolean bl = this.canConnectToFence(state);
      boolean bl2 = block instanceof FenceGateBlock && FenceGateBlock.canWallConnect(state, dir);
      return !cannotConnect(state) && neighborIsFullSquare || bl || bl2;
   }

   private boolean canConnectToFence(BlockState state) {
      return state.isIn(BlockTags.FENCES) && state.isIn(BlockTags.WOODEN_FENCES) == this.getDefaultState().isIn(BlockTags.WOODEN_FENCES);
   }

   @Override
   protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
      return !world.isClient() ? LeadItem.attachHeldMobsToBlock(player, world, pos) : ActionResult.PASS;
   }

   @Override
   protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)) {
         return stack.isOf(Items.LEAD) ? ActionResult.SUCCESS : ActionResult.PASS;
      }

      return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
   }

   @Override
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      BlockView blockView = ctx.getWorld();
      BlockPos blockPos = ctx.getBlockPos();
      FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
      BlockPos blockPos2 = blockPos.north();
      BlockPos blockPos3 = blockPos.east();
      BlockPos blockPos4 = blockPos.south();
      BlockPos blockPos5 = blockPos.west();
      BlockState blockState = blockView.getBlockState(blockPos2);
      BlockState blockState2 = blockView.getBlockState(blockPos3);
      BlockState blockState3 = blockView.getBlockState(blockPos4);
      BlockState blockState4 = blockView.getBlockState(blockPos5);
      return super.getPlacementState(ctx)
         .with(NORTH, this.canConnect(blockState, blockState.isSideSolidFullSquare(blockView, blockPos2, Direction.SOUTH), Direction.SOUTH))
         .with(EAST, this.canConnect(blockState2, blockState2.isSideSolidFullSquare(blockView, blockPos3, Direction.WEST), Direction.WEST))
         .with(SOUTH, this.canConnect(blockState3, blockState3.isSideSolidFullSquare(blockView, blockPos4, Direction.NORTH), Direction.NORTH))
         .with(WEST, this.canConnect(blockState4, blockState4.isSideSolidFullSquare(blockView, blockPos5, Direction.EAST), Direction.EAST))
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
            FACING_PROPERTIES.get(direction),
            this.canConnect(neighborState, neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite()), direction.getOpposite())
         )
         : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
   }

   @Override
   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
   }
}
