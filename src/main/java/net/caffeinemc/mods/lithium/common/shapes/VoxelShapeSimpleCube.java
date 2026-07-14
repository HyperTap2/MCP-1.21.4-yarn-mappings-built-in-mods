package net.caffeinemc.mods.lithium.common.shapes;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.List;
import net.minecraft.util.math.AxisCycleDirection;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class VoxelShapeSimpleCube extends VoxelShape implements VoxelShapeCaster {
   static final double EPSILON = 1.0E-7;
   final double minX;
   final double minY;
   final double minZ;
   final double maxX;
   final double maxY;
   final double maxZ;
   public final boolean isTiny;

   public VoxelShapeSimpleCube(VoxelSet voxels, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      super(voxels);
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
      this.isTiny = minX + 3.0E-7 >= maxX || minY + 3.0E-7 >= maxY || minZ + 3.0E-7 >= maxZ;
   }

   @Override
   public VoxelShape offset(double x, double y, double z) {
      return new VoxelShapeSimpleCube(
         this.voxels, this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z
      );
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
         case NONE -> calculatePenetration(this.minX, this.maxX, box.minX, box.maxX, maxDist);
         case FORWARD -> calculatePenetration(this.minZ, this.maxZ, box.minZ, box.maxZ, maxDist);
         case BACKWARD -> calculatePenetration(this.minY, this.maxY, box.minY, box.maxY, maxDist);
      };
   }

   boolean intersects(AxisCycleDirection direction, Box box) {
      return switch (direction) {
         case NONE -> lessThan(this.minY, box.maxY)
            && lessThan(box.minY, this.maxY)
            && lessThan(this.minZ, box.maxZ)
            && lessThan(box.minZ, this.maxZ);
         case FORWARD -> lessThan(this.minX, box.maxX)
            && lessThan(box.minX, this.maxX)
            && lessThan(this.minY, box.maxY)
            && lessThan(box.minY, this.maxY);
         case BACKWARD -> lessThan(this.minZ, box.maxZ)
            && lessThan(box.minZ, this.maxZ)
            && lessThan(this.minX, box.maxX)
            && lessThan(box.minX, this.maxX);
      };
   }

   private static double calculatePenetration(double shapeMin, double shapeMax, double boxMin, double boxMax, double maxDist) {
      double penetration;
      if (maxDist > 0.0) {
         penetration = shapeMin - boxMax;
         if (penetration < -EPSILON || maxDist < penetration) {
            return maxDist;
         }
      } else {
         penetration = shapeMax - boxMin;
         if (penetration > EPSILON || maxDist > penetration) {
            return maxDist;
         }
      }
      return penetration;
   }

   @Override
   public List<Box> getBoundingBoxes() {
      return Lists.newArrayList(this.getBoundingBox());
   }

   @Override
   public Box getBoundingBox() {
      return new Box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   @Override
   public double getMin(Direction.Axis axis) {
      return axis.choose(this.minX, this.minY, this.minZ);
   }

   @Override
   public double getMax(Direction.Axis axis) {
      return axis.choose(this.maxX, this.maxY, this.maxZ);
   }

   @Override
   protected double getPointPosition(Direction.Axis axis, int index) {
      if (index < 0 || index > 1) {
         throw new IndexOutOfBoundsException(index);
      }
      return index == 0 ? this.getMin(axis) : this.getMax(axis);
   }

   @Override
   public DoubleList getPointPositions(Direction.Axis axis) {
      return DoubleArrayList.wrap(new double[]{this.getMin(axis), this.getMax(axis)});
   }

   @Override
   public boolean isEmpty() {
      return this.minX >= this.maxX || this.minY >= this.maxY || this.minZ >= this.maxZ;
   }

   @Override
   protected int getCoordIndex(Direction.Axis axis, double coord) {
      return coord < this.getMin(axis) ? -1 : coord >= this.getMax(axis) ? 1 : 0;
   }

   private static boolean lessThan(double first, double second) {
      return first + EPSILON < second;
   }

   @Override
   public boolean intersects(Box box, double blockX, double blockY, double blockZ) {
      return box.minX + EPSILON < this.maxX + blockX
         && box.maxX - EPSILON > this.minX + blockX
         && box.minY + EPSILON < this.maxY + blockY
         && box.maxY - EPSILON > this.minY + blockY
         && box.minZ + EPSILON < this.maxZ + blockZ
         && box.maxZ - EPSILON > this.minZ + blockZ;
   }

   @Override
   public void forEachBox(VoxelShapes.BoxConsumer consumer) {
      consumer.consume(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }
}
