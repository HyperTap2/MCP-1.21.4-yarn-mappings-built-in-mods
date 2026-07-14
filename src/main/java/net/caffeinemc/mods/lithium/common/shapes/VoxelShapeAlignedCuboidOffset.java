package net.caffeinemc.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.caffeinemc.mods.lithium.common.shapes.lists.OffsetFractionalDoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeAlignedCuboidOffset extends VoxelShapeAlignedCuboid {
   private final double xOffset;
   private final double yOffset;
   private final double zOffset;

   public VoxelShapeAlignedCuboidOffset(VoxelShapeAlignedCuboid original, VoxelSet voxels, double xOffset, double yOffset, double zOffset) {
      super(
         voxels,
         original.minX + xOffset,
         original.minY + yOffset,
         original.minZ + zOffset,
         original.maxX + xOffset,
         original.maxY + yOffset,
         original.maxZ + zOffset,
         original.xyzResolution
      );
      if (original instanceof VoxelShapeAlignedCuboidOffset offsetShape) {
         this.xOffset = offsetShape.xOffset + xOffset;
         this.yOffset = offsetShape.yOffset + yOffset;
         this.zOffset = offsetShape.zOffset + zOffset;
      } else {
         this.xOffset = xOffset;
         this.yOffset = yOffset;
         this.zOffset = zOffset;
      }
   }

   @Override
   public VoxelShape offset(double x, double y, double z) {
      return new VoxelShapeAlignedCuboidOffset(this, this.voxels, x, y, z);
   }

   @Override
   protected double calculateMaxDistance(AxisCycleDirection cycleDirection, Box box, double maxDist) {
      if (Math.abs(maxDist) < EPSILON) {
         return 0.0;
      }
      double penetration = this.calculatePenetration(cycleDirection, box, maxDist);
      return penetration != maxDist && this.intersects(cycleDirection, box) ? penetration : maxDist;
   }

   private double calculatePenetration(AxisCycleDirection direction, Box box, double maxDist) {
      return switch (direction) {
         case NONE -> calculatePenetration(this.minX, this.maxX, this.getXSegments(), this.xOffset, box.minX, box.maxX, maxDist);
         case FORWARD -> calculatePenetration(this.minZ, this.maxZ, this.getZSegments(), this.zOffset, box.minZ, box.maxZ, maxDist);
         case BACKWARD -> calculatePenetration(this.minY, this.maxY, this.getYSegments(), this.yOffset, box.minY, box.maxY, maxDist);
      };
   }

   private static double calculatePenetration(
      double min, double max, int segments, double shapeOffset, double boxMin, double boxMax, double maxDist
   ) {
      if (maxDist > 0.0) {
         double gap = min - boxMax;
         if (gap >= -EPSILON) {
            return Math.min(gap, maxDist);
         }
         if (segments == 1) {
            return maxDist;
         }
         int segment = MathHelper.ceil((boxMax - LARGE_EPSILON - shapeOffset) * segments);
         double wall = (double)segment / segments + shapeOffset;
         if (wall < boxMax - EPSILON) {
            wall = (double)++segment / segments + shapeOffset;
         }
         return wall < max - LARGE_EPSILON ? Math.min(maxDist, wall - boxMax) : maxDist;
      }

      double gap = max - boxMin;
      if (gap <= EPSILON) {
         return Math.max(gap, maxDist);
      }
      if (segments == 1) {
         return maxDist;
      }
      int segment = MathHelper.floor((boxMin + LARGE_EPSILON - shapeOffset) * segments);
      double wall = (double)segment / segments + shapeOffset;
      if (wall > boxMin + EPSILON) {
         wall = (double)--segment / segments + shapeOffset;
      }
      return wall > min + LARGE_EPSILON ? Math.max(maxDist, wall - boxMin) : maxDist;
   }

   @Override
   public DoubleList getPointPositions(Direction.Axis axis) {
      return switch (axis) {
         case X -> new OffsetFractionalDoubleList(this.getXSegments(), this.xOffset);
         case Y -> new OffsetFractionalDoubleList(this.getYSegments(), this.yOffset);
         case Z -> new OffsetFractionalDoubleList(this.getZSegments(), this.zOffset);
      };
   }

   @Override
   protected double getPointPosition(Direction.Axis axis, int index) {
      return switch (axis) {
         case X -> this.xOffset + (double)index / this.getXSegments();
         case Y -> this.yOffset + (double)index / this.getYSegments();
         case Z -> this.zOffset + (double)index / this.getZSegments();
      };
   }

   @Override
   protected int getCoordIndex(Direction.Axis axis, double coord) {
      int segments = this.getSegments(axis);
      double shifted = switch (axis) {
         case X -> coord - this.xOffset;
         case Y -> coord - this.yOffset;
         case Z -> coord - this.zOffset;
      };
      return MathHelper.clamp(MathHelper.floor(shifted * segments), -1, segments);
   }
}
