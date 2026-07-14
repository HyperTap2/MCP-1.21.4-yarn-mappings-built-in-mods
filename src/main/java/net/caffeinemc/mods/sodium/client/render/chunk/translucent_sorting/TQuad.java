package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.Arrays;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class TQuad {
   private static final int QUANTIZATION_FACTOR = 4;
   private ModelQuadFacing facing;
   private final float[] extents;
   private float[] vertexPositions;
   private final int packedNormal;
   private final float accurateDotProduct;
   private float quantizedDotProduct;
   private Vector3fc center;
   private Vector3fc quantizedNormal;
   private Vector3fc accurateNormal;

   private TQuad(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center, int packedNormal) {
      this.facing = facing;
      this.extents = extents;
      this.vertexPositions = vertexPositions;
      this.center = center;
      this.packedNormal = packedNormal;
      if (this.facing.isAligned()) {
         this.accurateDotProduct = getAlignedDotProduct(this.facing, this.extents);
      } else {
         float normX = NormI8.unpackX(this.packedNormal);
         float normY = NormI8.unpackY(this.packedNormal);
         float normZ = NormI8.unpackZ(this.packedNormal);
         this.accurateDotProduct = this.getCenter().dot(normX, normY, normZ);
      }

      this.quantizedDotProduct = this.accurateDotProduct;
   }

   private static float getAlignedDotProduct(ModelQuadFacing facing, float[] extents) {
      return extents[facing.ordinal()] * facing.getSign();
   }

   static TQuad fromAligned(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center) {
      return new TQuad(facing, extents, vertexPositions, center, ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()]);
   }

   static TQuad fromUnaligned(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center, int packedNormal) {
      return new TQuad(facing, extents, vertexPositions, center, packedNormal);
   }

   public ModelQuadFacing getFacing() {
      return this.facing;
   }

   public ModelQuadFacing useQuantizedFacing() {
      if (!this.facing.isAligned()) {
         this.getQuantizedNormal();
         this.facing = ModelQuadFacing.fromNormal(this.quantizedNormal.x(), this.quantizedNormal.y(), this.quantizedNormal.z());
         if (this.facing.isAligned()) {
            this.quantizedDotProduct = getAlignedDotProduct(this.facing, this.extents);
         } else {
            this.quantizedDotProduct = this.getCenter().dot(this.quantizedNormal);
         }
      }

      return this.facing;
   }

   public float[] getExtents() {
      return this.extents;
   }

   public float[] getVertexPositions() {
      if (this.vertexPositions == null) {
         this.vertexPositions = new float[12];
         int facingAxis = this.facing.getAxis();
         int xRange = facingAxis == 0 ? 0 : 3;
         int yRange = facingAxis == 1 ? 0 : 3;
         int zRange = facingAxis == 2 ? 0 : 3;
         int itemIndex = 0;

         for (int x = 0; x <= xRange; x += 3) {
            for (int y = 0; y <= yRange; y += 3) {
               for (int z = 0; z <= zRange; z += 3) {
                  this.vertexPositions[itemIndex++] = this.extents[x];
                  this.vertexPositions[itemIndex++] = this.extents[y + 1];
                  this.vertexPositions[itemIndex++] = this.extents[z + 2];
               }
            }
         }
      }

      return this.vertexPositions;
   }

   public Vector3fc getCenter() {
      if (this.center == null) {
         this.center = new Vector3f(
            (this.extents[0] + this.extents[3]) / 2.0F, (this.extents[1] + this.extents[4]) / 2.0F, (this.extents[2] + this.extents[5]) / 2.0F
         );
      }

      return this.center;
   }

   public float getAccurateDotProduct() {
      return this.accurateDotProduct;
   }

   public float getQuantizedDotProduct() {
      return this.quantizedDotProduct;
   }

   public int getPackedNormal() {
      return this.packedNormal;
   }

   public Vector3fc getQuantizedNormal() {
      if (this.quantizedNormal == null) {
         if (this.facing.isAligned()) {
            this.quantizedNormal = this.facing.getAlignedNormal();
         } else {
            this.computeQuantizedNormal();
         }
      }

      return this.quantizedNormal;
   }

   public Vector3fc getAccurateNormal() {
      if (this.facing.isAligned()) {
         return this.facing.getAlignedNormal();
      } else {
         if (this.accurateNormal == null) {
            this.accurateNormal = new Vector3f(NormI8.unpackX(this.packedNormal), NormI8.unpackY(this.packedNormal), NormI8.unpackZ(this.packedNormal));
         }

         return this.accurateNormal;
      }
   }

   private void computeQuantizedNormal() {
      float normX = NormI8.unpackX(this.packedNormal);
      float normY = NormI8.unpackY(this.packedNormal);
      float normZ = NormI8.unpackZ(this.packedNormal);
      float infNormLength = Math.max(Math.abs(normX), Math.max(Math.abs(normY), Math.abs(normZ)));
      if (infNormLength != 0.0F && infNormLength != 1.0F) {
         normX /= infNormLength;
         normY /= infNormLength;
         normZ /= infNormLength;
      }

      Vector3f normal = new Vector3f((int)(normX * 4.0F), (int)(normY * 4.0F), (int)(normZ * 4.0F));
      normal.normalize();
      this.quantizedNormal = normal;
   }

   int getQuadHash() {
      int result = 1;
      result = 31 * result + Arrays.hashCode(this.extents);
      if (this.facing.isAligned()) {
         result = 31 * result + this.facing.hashCode();
      } else {
         result = 31 * result + this.packedNormal;
      }

      return 31 * result + Float.hashCode(this.quantizedDotProduct);
   }

   public boolean extentsEqual(float[] other) {
      return extentsEqual(this.extents, other);
   }

   public static boolean extentsEqual(float[] a, float[] b) {
      for (int i = 0; i < 6; i++) {
         if (a[i] != b[i]) {
            return false;
         }
      }

      return true;
   }

   public static boolean extentsIntersect(float[] extentsA, float[] extentsB) {
      for (int axis = 0; axis < 3; axis++) {
         int opposite = axis + 3;
         if (extentsA[axis] <= extentsB[opposite] || extentsB[axis] <= extentsA[opposite]) {
            return false;
         }
      }

      return true;
   }

   public static boolean extentsIntersect(TQuad a, TQuad b) {
      return extentsIntersect(a.extents, b.extents);
   }
}
