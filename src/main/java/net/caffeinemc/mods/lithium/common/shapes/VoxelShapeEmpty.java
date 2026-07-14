package net.caffeinemc.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;

public class VoxelShapeEmpty extends VoxelShape implements VoxelShapeCaster {
   private static final DoubleList EMPTY_LIST = DoubleArrayList.wrap(new double[]{0.0});

   public VoxelShapeEmpty(VoxelSet voxels) {
      super(voxels);
   }

   @Override
   protected double calculateMaxDistance(AxisCycleDirection axisCycle, Box box, double maxDist) {
      return maxDist;
   }

   @Override
   public DoubleList getPointPositions(Direction.Axis axis) {
      return EMPTY_LIST;
   }

   @Override
   public double getMin(Direction.Axis axis) {
      return Double.POSITIVE_INFINITY;
   }

   @Override
   public double getMax(Direction.Axis axis) {
      return Double.NEGATIVE_INFINITY;
   }

   @Override
   public boolean isEmpty() {
      return true;
   }

   @Override
   public boolean intersects(Box box, double blockX, double blockY, double blockZ) {
      return false;
   }
}
