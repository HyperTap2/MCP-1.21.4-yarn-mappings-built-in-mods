package net.caffeinemc.mods.sodium.client.render.frapi.mesh;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.GeometryHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.NormalHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.material.RenderMaterialImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class QuadViewImpl implements QuadView, ModelQuadView {
   @Nullable
   protected Direction nominalFace;
   protected boolean isGeometryInvalid = true;
   protected final Vector3f faceNormal = new Vector3f();
   protected int[] data;
   protected int baseIndex = 0;

   public void load() {
      this.isGeometryInvalid = false;
      this.nominalFace = this.lightFace();
      NormI8.unpack(this.packedFaceNormal(), this.faceNormal);
   }

   protected void computeGeometry() {
      if (this.isGeometryInvalid) {
         this.isGeometryInvalid = false;
         NormalHelper.computeFaceNormal(this.faceNormal, this);
         int packedFaceNormal = NormI8.pack(this.faceNormal);
         this.data[this.baseIndex + 1] = packedFaceNormal;
         Direction lightFace = GeometryHelper.lightFace(this);
         this.data[this.baseIndex + 0] = EncodingFormat.lightFace(this.data[this.baseIndex + 0], lightFace);
         this.data[this.baseIndex + 0] = EncodingFormat.normalFace(this.data[this.baseIndex + 0], ModelQuadFacing.fromPackedNormal(packedFaceNormal));
         this.data[this.baseIndex + 0] = EncodingFormat.geometryFlags(this.data[this.baseIndex + 0], ModelQuadFlags.getQuadFlags(this, lightFace));
      }
   }

   public int geometryFlags() {
      this.computeGeometry();
      return EncodingFormat.geometryFlags(this.data[this.baseIndex + 0]);
   }

   public boolean hasShade() {
      return !this.material().disableDiffuse();
   }

   public float x(int vertexIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_X]);
   }

   public float y(int vertexIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_Y]);
   }

   public float z(int vertexIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_Z]);
   }

   public float posByIndex(int vertexIndex, int coordinateIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_X + coordinateIndex]);
   }

   public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
      if (target == null) {
         target = new Vector3f();
      }

      int index = this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_X;
      target.set(Float.intBitsToFloat(this.data[index]), Float.intBitsToFloat(this.data[index + 1]), Float.intBitsToFloat(this.data[index + 2]));
      return target;
   }

   public int color(int vertexIndex) {
      return this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_COLOR];
   }

   public float u(int vertexIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_U]);
   }

   public float v(int vertexIndex) {
      return Float.intBitsToFloat(this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_V]);
   }

   public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
      if (target == null) {
         target = new Vector2f();
      }

      int index = this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_U;
      target.set(Float.intBitsToFloat(this.data[index]), Float.intBitsToFloat(this.data[index + 1]));
      return target;
   }

   public int lightmap(int vertexIndex) {
      return this.data[this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_LIGHTMAP];
   }

   public int normalFlags() {
      return EncodingFormat.normalFlags(this.data[this.baseIndex + 0]);
   }

   public boolean hasNormal(int vertexIndex) {
      return (this.normalFlags() & 1 << vertexIndex) != 0;
   }

   public boolean hasVertexNormals() {
      return this.normalFlags() != 0;
   }

   public boolean hasAllVertexNormals() {
      return (this.normalFlags() & 15) == 15;
   }

   protected final int normalIndex(int vertexIndex) {
      return this.baseIndex + vertexIndex * EncodingFormat.VERTEX_STRIDE + EncodingFormat.VERTEX_NORMAL;
   }

   public int packedNormal(int vertexIndex) {
      return this.data[this.normalIndex(vertexIndex)];
   }

   public float normalX(int vertexIndex) {
      return this.hasNormal(vertexIndex) ? NormI8.unpackX(this.data[this.normalIndex(vertexIndex)]) : Float.NaN;
   }

   public float normalY(int vertexIndex) {
      return this.hasNormal(vertexIndex) ? NormI8.unpackY(this.data[this.normalIndex(vertexIndex)]) : Float.NaN;
   }

   public float normalZ(int vertexIndex) {
      return this.hasNormal(vertexIndex) ? NormI8.unpackZ(this.data[this.normalIndex(vertexIndex)]) : Float.NaN;
   }

   @Nullable
   public Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
      if (this.hasNormal(vertexIndex)) {
         if (target == null) {
            target = new Vector3f();
         }

         int normal = this.data[this.normalIndex(vertexIndex)];
         NormI8.unpack(normal, target);
         return target;
      } else {
         return null;
      }
   }

   @Nullable
   public final Direction cullFace() {
      return EncodingFormat.cullFace(this.data[this.baseIndex + 0]);
   }

   @NotNull
   public final Direction lightFace() {
      this.computeGeometry();
      return EncodingFormat.lightFace(this.data[this.baseIndex + 0]);
   }

   public final ModelQuadFacing normalFace() {
      this.computeGeometry();
      return EncodingFormat.normalFace(this.data[this.baseIndex + 0]);
   }

   @Nullable
   public final Direction nominalFace() {
      return this.nominalFace;
   }

   public final int packedFaceNormal() {
      this.computeGeometry();
      return this.data[this.baseIndex + 1];
   }

   public final Vector3f faceNormal() {
      this.computeGeometry();
      return this.faceNormal;
   }

   public final RenderMaterialImpl material() {
      return EncodingFormat.material(this.data[this.baseIndex + 0]);
   }

   public final int tintIndex() {
      return this.data[this.baseIndex + 2];
   }

   public final int tag() {
      return this.data[this.baseIndex + 3];
   }

   public final void toVanilla(int[] target, int targetIndex) {
      System.arraycopy(this.data, this.baseIndex + 4, target, targetIndex, EncodingFormat.QUAD_STRIDE);
      int colorIndex = targetIndex + 3;

      for (int i = 0; i < 4; i++) {
         target[colorIndex] = ColorHelper.toVanillaColor(target[colorIndex]);
         colorIndex += QuadView.VANILLA_VERTEX_STRIDE;
      }
   }

   @Override
   public float getX(int idx) {
      return this.x(idx);
   }

   @Override
   public float getY(int idx) {
      return this.y(idx);
   }

   @Override
   public float getZ(int idx) {
      return this.z(idx);
   }

   @Override
   public int getColor(int idx) {
      return ColorHelper.toVanillaColor(this.color(idx));
   }

   @Override
   public float getTexU(int idx) {
      return this.u(idx);
   }

   @Override
   public float getTexV(int idx) {
      return this.v(idx);
   }

   @Override
   public int getVertexNormal(int idx) {
      return this.data[this.normalIndex(idx)];
   }

   @Override
   public int getFaceNormal() {
      return this.packedFaceNormal();
   }

   @Override
   public int getLight(int idx) {
      return this.lightmap(idx);
   }

   @Override
   public int getTintIndex() {
      return this.tintIndex();
   }

   @Override
   public Sprite getSprite() {
      throw new UnsupportedOperationException("Not available for QuadViewImpl.");
   }

   @Override
   public Direction getLightFace() {
      return this.lightFace();
   }

   @Override
   public int getMaxLightQuad(int idx) {
      return this.lightmap(idx);
   }

   @Override
   public int getFlags() {
      return this.geometryFlags();
   }
}
