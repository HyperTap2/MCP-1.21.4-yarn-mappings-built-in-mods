package net.caffeinemc.mods.lithium.common.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelSet;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class VoxelShapeMatchesAnywhere {
   private static final double EPSILON = 1.0E-7;

   private VoxelShapeMatchesAnywhere() {
   }

   @Nullable
   public static Boolean tryMatch(
      VoxelShape shapeA, VoxelShape shapeB, VoxelSet voxelsA, VoxelSet voxelsB, BooleanBiFunction predicate
   ) {
      if (shapeA instanceof VoxelShapeSimpleCube cubeA && shapeB instanceof VoxelShapeSimpleCube cubeB) {
         if (cubeA.isTiny || cubeB.isTiny) {
            return null;
         }

         if (predicate.apply(true, true)) {
            return intersects(cubeA, cubeB) || predicate.apply(true, false) || predicate.apply(false, true);
         }
         if (predicate.apply(true, false) && exceedsShape(cubeA, cubeB)) {
            return true;
         }
         if (predicate.apply(false, true) && exceedsShape(cubeB, cubeA)) {
            return true;
         }
         return false;
      }

      if (!(shapeA instanceof VoxelShapeSimpleCube) && !(shapeB instanceof VoxelShapeSimpleCube)) {
         return null;
      }

      VoxelShapeSimpleCube simpleCube = (VoxelShapeSimpleCube)(shapeA instanceof VoxelShapeSimpleCube ? shapeA : shapeB);
      VoxelShape otherShape = simpleCube == shapeA ? shapeB : shapeA;
      VoxelSet otherVoxels = simpleCube == shapeA ? voxelsB : voxelsA;
      if (simpleCube.isTiny || isTiny(otherShape)) {
         return null;
      }

      boolean acceptSimpleCubeAlone = predicate.apply(shapeA == simpleCube, shapeB == simpleCube);
      if (acceptSimpleCubeAlone
         && exceedsCube(
            simpleCube,
            otherShape.getMin(Direction.Axis.X),
            otherShape.getMin(Direction.Axis.Y),
            otherShape.getMin(Direction.Axis.Z),
            otherShape.getMax(Direction.Axis.X),
            otherShape.getMax(Direction.Axis.Y),
            otherShape.getMax(Direction.Axis.Z)
         )) {
         return true;
      }

      boolean acceptAnd = predicate.apply(true, true);
      boolean acceptOtherShapeAlone = predicate.apply(shapeA == otherShape, shapeB == otherShape);
      DoubleList pointPositionsX = otherShape.getPointPositions(Direction.Axis.X);
      DoubleList pointPositionsY = otherShape.getPointPositions(Direction.Axis.Y);
      DoubleList pointPositionsZ = otherShape.getPointPositions(Direction.Axis.Z);
      int xMax = otherVoxels.getMax(Direction.Axis.X);
      int yMax = otherVoxels.getMax(Direction.Axis.Y);
      int zMax = otherVoxels.getMax(Direction.Axis.Z);
      double simpleCubeMaxX = simpleCube.getMax(Direction.Axis.X);
      double simpleCubeMinX = simpleCube.getMin(Direction.Axis.X);
      double simpleCubeMaxY = simpleCube.getMax(Direction.Axis.Y);
      double simpleCubeMinY = simpleCube.getMin(Direction.Axis.Y);
      double simpleCubeMaxZ = simpleCube.getMax(Direction.Axis.Z);
      double simpleCubeMinZ = simpleCube.getMin(Direction.Axis.Z);

      for (int x = otherVoxels.getMin(Direction.Axis.X); x < xMax; x++) {
         boolean cubeIntersectsX = simpleCubeMaxX - EPSILON > pointPositionsX.getDouble(x)
            && simpleCubeMinX < pointPositionsX.getDouble(x + 1) - EPSILON;
         if (!acceptOtherShapeAlone && !cubeIntersectsX) {
            continue;
         }
         boolean xExceedsCube = acceptOtherShapeAlone
            && (simpleCubeMaxX < pointPositionsX.getDouble(x + 1) - EPSILON
               || simpleCubeMinX - EPSILON > pointPositionsX.getDouble(x));

         for (int y = otherVoxels.getMin(Direction.Axis.Y); y < yMax; y++) {
            boolean cubeIntersectsY = simpleCubeMaxY - EPSILON > pointPositionsY.getDouble(y)
               && simpleCubeMinY < pointPositionsY.getDouble(y + 1) - EPSILON;
            if (!acceptOtherShapeAlone && !cubeIntersectsY) {
               continue;
            }
            boolean yExceedsCube = acceptOtherShapeAlone
               && (simpleCubeMaxY < pointPositionsY.getDouble(y + 1) - EPSILON
                  || simpleCubeMinY - EPSILON > pointPositionsY.getDouble(y));

            for (int z = otherVoxels.getMin(Direction.Axis.Z); z < zMax; z++) {
               boolean cubeIntersectsZ = simpleCubeMaxZ - EPSILON > pointPositionsZ.getDouble(z)
                  && simpleCubeMinZ < pointPositionsZ.getDouble(z + 1) - EPSILON;
               if (!acceptOtherShapeAlone && !cubeIntersectsZ) {
                  continue;
               }
               boolean zExceedsCube = acceptOtherShapeAlone
                  && (simpleCubeMaxZ < pointPositionsZ.getDouble(z + 1) - EPSILON
                     || simpleCubeMinZ - EPSILON > pointPositionsZ.getDouble(z));
               boolean otherContains = otherVoxels.contains(x, y, z);
               boolean cubeContains = cubeIntersectsX && cubeIntersectsY && cubeIntersectsZ;
               if (acceptAnd && otherContains && cubeContains
                  || acceptSimpleCubeAlone && !otherContains && cubeContains
                  || acceptOtherShapeAlone && otherContains && (xExceedsCube || yExceedsCube || zExceedsCube)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   private static boolean isTiny(VoxelShape shape) {
      return shape.getMin(Direction.Axis.X) > shape.getMax(Direction.Axis.X) - 3.0E-7
         || shape.getMin(Direction.Axis.Y) > shape.getMax(Direction.Axis.Y) - 3.0E-7
         || shape.getMin(Direction.Axis.Z) > shape.getMax(Direction.Axis.Z) - 3.0E-7;
   }

   private static boolean exceedsCube(
      VoxelShapeSimpleCube cube, double minX, double minY, double minZ, double maxX, double maxY, double maxZ
   ) {
      return cube.getMin(Direction.Axis.X) < minX - EPSILON
         || cube.getMax(Direction.Axis.X) > maxX + EPSILON
         || cube.getMin(Direction.Axis.Y) < minY - EPSILON
         || cube.getMax(Direction.Axis.Y) > maxY + EPSILON
         || cube.getMin(Direction.Axis.Z) < minZ - EPSILON
         || cube.getMax(Direction.Axis.Z) > maxZ + EPSILON;
   }

   private static boolean exceedsShape(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
      return a.getMin(Direction.Axis.X) < b.getMin(Direction.Axis.X) - EPSILON
         || a.getMax(Direction.Axis.X) > b.getMax(Direction.Axis.X) + EPSILON
         || a.getMin(Direction.Axis.Y) < b.getMin(Direction.Axis.Y) - EPSILON
         || a.getMax(Direction.Axis.Y) > b.getMax(Direction.Axis.Y) + EPSILON
         || a.getMin(Direction.Axis.Z) < b.getMin(Direction.Axis.Z) - EPSILON
         || a.getMax(Direction.Axis.Z) > b.getMax(Direction.Axis.Z) + EPSILON;
   }

   private static boolean intersects(VoxelShapeSimpleCube a, VoxelShapeSimpleCube b) {
      return a.getMin(Direction.Axis.X) < b.getMax(Direction.Axis.X) - EPSILON
         && a.getMax(Direction.Axis.X) > b.getMin(Direction.Axis.X) + EPSILON
         && a.getMin(Direction.Axis.Y) < b.getMax(Direction.Axis.Y) - EPSILON
         && a.getMax(Direction.Axis.Y) > b.getMin(Direction.Axis.Y) + EPSILON
         && a.getMin(Direction.Axis.Z) < b.getMax(Direction.Axis.Z) - EPSILON
         && a.getMax(Direction.Axis.Z) > b.getMin(Direction.Axis.Z) + EPSILON;
   }
}
