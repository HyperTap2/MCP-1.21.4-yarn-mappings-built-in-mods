package net.caffeinemc.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.FractionalDoubleList;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeAlignedCuboid extends VoxelShapeSimpleCube {
   static final double LARGE_EPSILON = 1.0E-6;
   protected final byte xyzResolution;

   public VoxelShapeAlignedCuboid(
      double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int xResolution, int yResolution, int zResolution
   ) {
      super(
         new CuboidVoxelSet(1 << xResolution, 1 << yResolution, 1 << zResolution, minX, minY, minZ, maxX, maxY, maxZ),
         minX,
         minY,
         minZ,
         maxX,
         maxY,
         maxZ
      );
      if (xResolution < 0 || xResolution > 3 || yResolution < 0 || yResolution > 3 || zResolution < 0 || zResolution > 3) {
         throw new IllegalArgumentException("Resolution must be between 0 and 3");
      }
      this.xyzResolution = (byte)(xResolution << 4 | yResolution << 2 | zResolution);
   }

   protected VoxelShapeAlignedCuboid(
      VoxelSet voxels, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, byte xyzResolution
   ) {
      super(voxels, minX, minY, minZ, maxX, maxY, maxZ);
      this.xyzResolution = xyzResolution;
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
         case NONE -> calculatePenetration(this.minX, this.maxX, this.getXSegments(), box.minX, box.maxX, maxDist);
         case FORWARD -> calculatePenetration(this.minZ, this.maxZ, this.getZSegments(), box.minZ, box.maxZ, maxDist);
         case BACKWARD -> calculatePenetration(this.minY, this.maxY, this.getYSegments(), box.minY, box.maxY, maxDist);
      };
   }

   private static double calculatePenetration(double min, double max, int segments, double boxMin, double boxMax, double maxDist) {
      if (maxDist > 0.0) {
         double gap = min - boxMax;
         if (gap >= -EPSILON) {
            return Math.min(gap, maxDist);
         }
         if (segments == 1) {
            return maxDist;
         }
         double wall = (double)MathHelper.ceil((boxMax - EPSILON) * segments) / segments;
         return wall < max - LARGE_EPSILON ? Math.min(maxDist, wall - boxMax) : maxDist;
      }

      double gap = max - boxMin;
      if (gap <= EPSILON) {
         return Math.max(gap, maxDist);
      }
      if (segments == 1) {
         return maxDist;
      }
      double wall = (double)MathHelper.floor((boxMin + EPSILON) * segments) / segments;
      return wall > min + LARGE_EPSILON ? Math.max(maxDist, wall - boxMin) : maxDist;
   }

   @Override
   public DoubleList getPointPositions(Direction.Axis axis) {
      return new FractionalDoubleList(this.getSegments(axis));
   }

   @Override
   protected double getPointPosition(Direction.Axis axis, int index) {
      return (double)index / this.getSegments(axis);
   }

   @Override
   protected int getCoordIndex(Direction.Axis axis, double coord) {
      int segments = this.getSegments(axis);
      return MathHelper.clamp(MathHelper.floor(coord * segments), -1, segments);
   }

   protected int getSegments(Direction.Axis axis) {
      return switch (axis) {
         case X -> this.getXSegments();
         case Y -> this.getYSegments();
         case Z -> this.getZSegments();
      };
   }

   protected int getXSegments() {
      return 1 << (this.xyzResolution >>> 4);
   }

   protected int getYSegments() {
      return 1 << (this.xyzResolution >>> 2 & 3);
   }

   protected int getZSegments() {
      return 1 << (this.xyzResolution & 3);
   }
}
