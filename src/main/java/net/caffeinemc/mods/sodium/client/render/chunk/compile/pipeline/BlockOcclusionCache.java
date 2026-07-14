package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

public class BlockOcclusionCache {
   private static final int CACHE_SIZE = 512;
   private static final int ENTRY_ABSENT = -1;
   private static final int ENTRY_FALSE = 0;
   private static final int ENTRY_TRUE = 1;
   private final Object2IntLinkedOpenCustomHashMap<BlockOcclusionCache.ShapeComparison> comparisonLookupTable;
   private final BlockOcclusionCache.ShapeComparison cachedComparisonObject = new BlockOcclusionCache.ShapeComparison();
   private final Mutable cachedPositionObject = new Mutable();

   public BlockOcclusionCache() {
      this.comparisonLookupTable = new Object2IntLinkedOpenCustomHashMap(512, 0.5F, new BlockOcclusionCache.ShapeComparison.ShapeComparisonStrategy());
      this.comparisonLookupTable.defaultReturnValue(-1);
   }

   public boolean shouldDrawSide(BlockState selfBlockState, BlockView view, BlockPos selfPos, Direction facing) {
      Mutable neighborPos = this.cachedPositionObject;
      neighborPos.set(selfPos, facing);
      BlockState neighborBlockState = view.getBlockState(neighborPos);
      VoxelShape neighborShape = neighborBlockState.getCullingFace(DirectionUtil.getOpposite(facing));
      if (isFullShape(neighborShape)) {
         return false;
      } else if (selfBlockState.isSideInvisible(neighborBlockState, facing)) {
         return false;
      } else if (PlatformBlockAccess.getInstance().shouldSkipRender(view, selfBlockState, neighborBlockState, selfPos, neighborPos, facing)) {
         return false;
      } else if (!isEmptyShape(neighborShape) && neighborBlockState.isOpaque()) {
         VoxelShape selfShape = selfBlockState.getCullingFace(facing);
         return isEmptyShape(selfShape) ? true : this.lookup(selfShape, neighborShape);
      } else {
         return true;
      }
   }

   private static boolean isFullShape(VoxelShape selfShape) {
      return selfShape == VoxelShapes.fullCube();
   }

   private static boolean isEmptyShape(VoxelShape voxelShape) {
      return voxelShape == VoxelShapes.empty() || voxelShape.isEmpty();
   }

   public boolean shouldDrawFullBlockFluidSide(
      BlockState selfBlockState, BlockView view, BlockPos selfPos, Direction facing, FluidState fluid, VoxelShape fluidShape
   ) {
      boolean fluidShapeIsBlock = fluidShape == VoxelShapes.fullCube();
      if (selfBlockState.isOpaque()) {
         VoxelShape selfShape = selfBlockState.getCullingFace(facing);
         if (!isEmptyShape(selfShape)) {
            if (isFullShape(selfShape) && fluidShapeIsBlock) {
               return false;
            }

            if (!this.lookup(fluidShape, selfShape)) {
               return false;
            }
         }
      }

      Mutable otherPos = this.cachedPositionObject;
      otherPos.set(selfPos.getX() + facing.getOffsetX(), selfPos.getY() + facing.getOffsetY(), selfPos.getZ() + facing.getOffsetZ());
      BlockState otherState = view.getBlockState(otherPos);
      if (otherState.getFluidState() == fluid) {
         return false;
      } else if (PlatformBlockAccess.getInstance().shouldOccludeFluid(facing.getOpposite(), otherState, fluid)) {
         return false;
      } else if (facing == Direction.UP) {
         return true;
      } else if (!otherState.isOpaque()) {
         return true;
      } else {
         VoxelShape otherShape = otherState.getCullingFace(facing.getOpposite());
         return isEmptyShape(otherShape) ? true : !isFullShape(otherShape) || !fluidShapeIsBlock;
      }
   }

   private boolean lookup(VoxelShape self, VoxelShape other) {
      BlockOcclusionCache.ShapeComparison comparison = this.cachedComparisonObject;
      comparison.self = self;
      comparison.other = other;

      return switch (this.comparisonLookupTable.getAndMoveToFirst(comparison)) {
         case 0 -> false;
         case 1 -> true;
         default -> this.calculate(comparison);
      };
   }

   private boolean calculate(BlockOcclusionCache.ShapeComparison comparison) {
      boolean result = VoxelShapes.matchesAnywhere(comparison.self, comparison.other, BooleanBiFunction.ONLY_FIRST);

      while (this.comparisonLookupTable.size() >= 512) {
         this.comparisonLookupTable.removeLastInt();
      }

      this.comparisonLookupTable.putAndMoveToFirst(comparison.copy(), result ? 1 : 0);
      return result;
   }

   private static final class ShapeComparison {
      private VoxelShape self;
      private VoxelShape other;

      private ShapeComparison() {
      }

      private ShapeComparison(VoxelShape self, VoxelShape other) {
         this.self = self;
         this.other = other;
      }

      public BlockOcclusionCache.ShapeComparison copy() {
         return new BlockOcclusionCache.ShapeComparison(this.self, this.other);
      }

      public static class ShapeComparisonStrategy implements Strategy<BlockOcclusionCache.ShapeComparison> {
         public int hashCode(BlockOcclusionCache.ShapeComparison value) {
            int result = System.identityHashCode(value.self);
            return 31 * result + System.identityHashCode(value.other);
         }

         public boolean equals(BlockOcclusionCache.ShapeComparison a, BlockOcclusionCache.ShapeComparison b) {
            if (a == b) {
               return true;
            } else {
               return a != null && b != null ? a.self == b.self && a.other == b.other : false;
            }
         }
      }
   }
}
