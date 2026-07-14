package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

import it.unimi.dsi.fastutil.floats.FloatArrays;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class AlignableNormal extends Vector3f {
   private static final AlignableNormal[] NORMALS = new AlignableNormal[ModelQuadFacing.DIRECTIONS];
   private static final int UNASSIGNED;
   private final int alignedDirection;

   private AlignableNormal(Vector3fc v, int alignedDirection) {
      super(v);
      this.alignedDirection = alignedDirection;
   }

   public static AlignableNormal fromAligned(int alignedDirection) {
      return NORMALS[alignedDirection];
   }

   public static AlignableNormal fromUnaligned(Vector3fc v) {
      return new AlignableNormal(v, UNASSIGNED);
   }

   public int getAlignedDirection() {
      return this.alignedDirection;
   }

   public boolean isAligned() {
      return this.alignedDirection != UNASSIGNED;
   }

   public int hashCode() {
      return this.isAligned() ? this.alignedDirection : super.hashCode() + ModelQuadFacing.DIRECTIONS;
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!super.equals(obj)) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         AlignableNormal other = (AlignableNormal)obj;
         return this.alignedDirection == other.alignedDirection;
      }
   }

   public static boolean queryRange(float[] sortedDistances, float start, float end) {
      int result = FloatArrays.binarySearch(sortedDistances, start);
      if (result < 0) {
         int insertionPoint = -result - 1;
         return insertionPoint >= sortedDistances.length ? false : sortedDistances[insertionPoint] <= end;
      } else {
         return true;
      }
   }

   static {
      for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
         NORMALS[i] = new AlignableNormal(ModelQuadFacing.ALIGNED_NORMALS[i], i);
      }

      UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
   }
}
