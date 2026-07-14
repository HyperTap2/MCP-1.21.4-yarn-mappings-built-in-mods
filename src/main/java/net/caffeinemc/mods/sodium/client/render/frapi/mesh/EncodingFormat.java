package net.caffeinemc.mods.sodium.client.render.frapi.mesh;

import com.google.common.base.Preconditions;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.frapi.material.RenderMaterialImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public abstract class EncodingFormat {
   static final int HEADER_BITS = 0;
   static final int HEADER_FACE_NORMAL = 1;
   static final int HEADER_TINT_INDEX = 2;
   static final int HEADER_TAG = 3;
   public static final int HEADER_STRIDE = 4;
   static final int VERTEX_X = 4;
   static final int VERTEX_Y = 5;
   static final int VERTEX_Z = 6;
   static final int VERTEX_COLOR = 7;
   static final int VERTEX_U = 8;
   static final int VERTEX_V = VERTEX_U + 1;
   static final int VERTEX_LIGHTMAP = 10;
   static final int VERTEX_NORMAL = 11;
   public static final int VERTEX_STRIDE;
   public static final int QUAD_STRIDE;
   public static final int QUAD_STRIDE_BYTES;
   public static final int TOTAL_STRIDE;
   static final int[] EMPTY;
   private static final int DIRECTION_MASK = MathHelper.smallestEncompassingPowerOfTwo(7) - 1;
   private static final int DIRECTION_BIT_COUNT = Integer.bitCount(DIRECTION_MASK);
   private static final int FACING_MASK = MathHelper.smallestEncompassingPowerOfTwo(ModelQuadFacing.COUNT) - 1;
   private static final int FACING_BIT_COUNT = Integer.bitCount(FACING_MASK);
   private static final int MATERIAL_MASK = MathHelper.smallestEncompassingPowerOfTwo(RenderMaterialImpl.VALUE_COUNT) - 1;
   private static final int MATERIAL_BIT_COUNT = Integer.bitCount(MATERIAL_MASK);
   private static final int CULL_SHIFT = 0;
   private static final int CULL_INVERSE_MASK = ~(DIRECTION_MASK << 0);
   private static final int LIGHT_SHIFT = 0 + DIRECTION_BIT_COUNT;
   private static final int LIGHT_INVERSE_MASK = ~(DIRECTION_MASK << LIGHT_SHIFT);
   private static final int NORMAL_FACE_SHIFT = LIGHT_SHIFT + DIRECTION_BIT_COUNT;
   private static final int NORMAL_FACE_INVERSE_MASK = ~(FACING_MASK << NORMAL_FACE_SHIFT);
   private static final int NORMALS_SHIFT = NORMAL_FACE_SHIFT + FACING_BIT_COUNT;
   private static final int NORMALS_COUNT = 4;
   private static final int NORMALS_MASK = 15;
   private static final int NORMALS_INVERSE_MASK = ~(15 << NORMALS_SHIFT);
   private static final int GEOMETRY_SHIFT = NORMALS_SHIFT + 4;
   private static final int GEOMETRY_MASK = 7;
   private static final int GEOMETRY_INVERSE_MASK = ~(7 << GEOMETRY_SHIFT);
   private static final int MATERIAL_SHIFT = GEOMETRY_SHIFT + 3;
   private static final int MATERIAL_INVERSE_MASK = ~(MATERIAL_MASK << MATERIAL_SHIFT);

   private EncodingFormat() {
   }

   static Direction cullFace(int bits) {
      return ModelHelper.faceFromIndex(bits >>> 0 & DIRECTION_MASK);
   }

   static int cullFace(int bits, Direction face) {
      return bits & CULL_INVERSE_MASK | ModelHelper.toFaceIndex(face) << 0;
   }

   static Direction lightFace(int bits) {
      return ModelHelper.faceFromIndex(bits >>> LIGHT_SHIFT & DIRECTION_MASK);
   }

   static int lightFace(int bits, Direction face) {
      return bits & LIGHT_INVERSE_MASK | ModelHelper.toFaceIndex(face) << LIGHT_SHIFT;
   }

   static ModelQuadFacing normalFace(int bits) {
      return ModelQuadFacing.VALUES[bits >>> NORMAL_FACE_SHIFT & FACING_MASK];
   }

   static int normalFace(int bits, ModelQuadFacing face) {
      return bits & NORMAL_FACE_INVERSE_MASK | face.ordinal() << NORMAL_FACE_SHIFT;
   }

   static int normalFlags(int bits) {
      return bits >>> NORMALS_SHIFT & 15;
   }

   static int normalFlags(int bits, int normalFlags) {
      return bits & NORMALS_INVERSE_MASK | (normalFlags & 15) << NORMALS_SHIFT;
   }

   static int geometryFlags(int bits) {
      return bits >>> GEOMETRY_SHIFT & 7;
   }

   static int geometryFlags(int bits, int geometryFlags) {
      return bits & GEOMETRY_INVERSE_MASK | (geometryFlags & 7) << GEOMETRY_SHIFT;
   }

   static RenderMaterialImpl material(int bits) {
      return RenderMaterialImpl.byIndex(bits >>> MATERIAL_SHIFT & MATERIAL_MASK);
   }

   static int material(int bits, RenderMaterialImpl material) {
      return bits & MATERIAL_INVERSE_MASK | material.index() << MATERIAL_SHIFT;
   }

   static {
      VertexFormat format = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
      VERTEX_STRIDE = format.getVertexSizeByte() / 4;
      QUAD_STRIDE = VERTEX_STRIDE * 4;
      QUAD_STRIDE_BYTES = QUAD_STRIDE * 4;
      TOTAL_STRIDE = 4 + QUAD_STRIDE;
      Preconditions.checkState(
         VERTEX_STRIDE == QuadView.VANILLA_VERTEX_STRIDE,
         "Sodium FRAPI vertex stride (%s) mismatched with rendering API (%s)",
         VERTEX_STRIDE,
         QuadView.VANILLA_VERTEX_STRIDE
      );
      Preconditions.checkState(
         QUAD_STRIDE == QuadView.VANILLA_QUAD_STRIDE,
         "Sodium FRAPI quad stride (%s) mismatched with rendering API (%s)",
         QUAD_STRIDE,
         QuadView.VANILLA_QUAD_STRIDE
      );
      EMPTY = new int[TOTAL_STRIDE];
      Preconditions.checkArgument(
         MATERIAL_SHIFT + MATERIAL_BIT_COUNT <= 32, "Sodium FRAPI header encoding bit count (%s) exceeds integer bit length)", TOTAL_STRIDE
      );
   }
}
