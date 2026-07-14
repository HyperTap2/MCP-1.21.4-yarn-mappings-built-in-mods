package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class SimpleVoxelShape extends VoxelShape {
   private static final Direction.Axis[] AXES = Direction.Axis.values();
   private final DoubleList[] pointPositions;

   protected SimpleVoxelShape(VoxelSet voxelSet) {
      super(voxelSet);
      this.pointPositions = new DoubleList[AXES.length];
      for (Direction.Axis axis : AXES) {
         this.pointPositions[axis.ordinal()] = new FractionalDoubleList(voxelSet.getSize(axis));
      }
   }

   @Override
   public DoubleList getPointPositions(Direction.Axis axis) {
      return this.pointPositions[axis.ordinal()];
   }

   @Override
   protected int getCoordIndex(Direction.Axis axis, double coord) {
      int i = this.voxels.getSize(axis);
      return MathHelper.floor(MathHelper.clamp(coord * i, -1.0, i));
   }
}
