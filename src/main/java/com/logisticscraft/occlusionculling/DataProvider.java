package com.logisticscraft.occlusionculling;

import com.logisticscraft.occlusionculling.util.Vec3d;

public interface DataProvider {
   boolean prepareChunk(int var1, int var2);

   boolean isOpaqueFullCube(int var1, int var2, int var3);

   default void cleanup() {
   }

   default void checkingPosition(Vec3d[] targetPoints, int size, Vec3d viewerPosition) {
   }
}

