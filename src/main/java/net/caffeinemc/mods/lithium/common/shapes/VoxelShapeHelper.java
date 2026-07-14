package net.caffeinemc.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleComparators;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.ArrayVoxelShape;
import net.minecraft.util.shape.BitSetVoxelSet;
import net.minecraft.util.shape.VoxelShape;

public final class VoxelShapeHelper {
   private VoxelShapeHelper() {
   }

   public static Optional<Vec3d> getClosestPointTo(Vec3d target, VoxelShape collidingShape, List<Box> boxes) {
      DoubleOpenHashSet xPoints = new DoubleOpenHashSet(collidingShape.getPointPositions(Direction.Axis.X));
      DoubleOpenHashSet yPoints = new DoubleOpenHashSet(collidingShape.getPointPositions(Direction.Axis.Y));
      DoubleOpenHashSet zPoints = new DoubleOpenHashSet(collidingShape.getPointPositions(Direction.Axis.Z));
      double minX = collidingShape.getMin(Direction.Axis.X);
      double maxX = collidingShape.getMax(Direction.Axis.X);
      double minY = collidingShape.getMin(Direction.Axis.Y);
      double maxY = collidingShape.getMax(Direction.Axis.Y);
      double minZ = collidingShape.getMin(Direction.Axis.Z);
      double maxZ = collidingShape.getMax(Direction.Axis.Z);

      for (Box box : boxes) {
         if (box.minX > minX) xPoints.add(box.minX);
         if (box.maxX < maxX) xPoints.add(box.maxX);
         if (box.minY > minY) yPoints.add(box.minY);
         if (box.maxY < maxY) yPoints.add(box.maxY);
         if (box.minZ > minZ) zPoints.add(box.minZ);
         if (box.maxZ < maxZ) zPoints.add(box.maxZ);
      }

      DoubleArrayList xList = new DoubleArrayList(xPoints);
      DoubleArrayList yList = new DoubleArrayList(yPoints);
      DoubleArrayList zList = new DoubleArrayList(zPoints);
      xList.sort(DoubleComparators.NATURAL_COMPARATOR);
      yList.sort(DoubleComparators.NATURAL_COMPARATOR);
      zList.sort(DoubleComparators.NATURAL_COMPARATOR);
      Double2IntMap xIndex = index(xList);
      Double2IntMap yIndex = index(yList);
      Double2IntMap zIndex = index(zList);
      int sizeX = xList.size() - 1;
      int sizeY = yList.size() - 1;
      int sizeZ = zList.size() - 1;
      BitSet storage = new BitSet(sizeX * sizeY * sizeZ);

      for (Box shapeBox : collidingShape.getBoundingBoxes()) {
         setRange(storage, xIndex.get(shapeBox.minX), xIndex.get(shapeBox.maxX), yIndex.get(shapeBox.minY), yIndex.get(shapeBox.maxY),
            zIndex.get(shapeBox.minZ), zIndex.get(shapeBox.maxZ), sizeY, sizeZ, true);
      }

      for (Box box : boxes) {
         setRange(storage, xIndex.getOrDefault(box.minX, 0), xIndex.getOrDefault(box.maxX, sizeX),
            yIndex.getOrDefault(box.minY, 0), yIndex.getOrDefault(box.maxY, sizeY),
            zIndex.getOrDefault(box.minZ, 0), zIndex.getOrDefault(box.maxZ, sizeZ), sizeY, sizeZ, false);
      }

      BitSetVoxelSet voxelSet = new BitSetVoxelSet(sizeX, sizeY, sizeZ);
      voxelSet.lithium$replaceStorage(storage);
      return ArrayVoxelShape.lithium$create(voxelSet, xList, yList, zList).getClosestPointTo(target);
   }

   private static Double2IntMap index(DoubleList values) {
      Double2IntMap result = new Double2IntOpenHashMap();
      for (int i = 0; i < values.size(); i++) result.put(values.getDouble(i), i);
      return result;
   }

   private static void setRange(BitSet storage, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
      int sizeY, int sizeZ, boolean value) {
      for (int x = minX; x < maxX; x++) {
         for (int y = minY; y < maxY; y++) {
            int from = (x * sizeY + y) * sizeZ + minZ;
            int to = (x * sizeY + y) * sizeZ + maxZ;
            storage.set(from, to, value);
         }
      }
   }
}
