package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public class ModelQuad implements ModelQuadViewMutable {
   private final int[] data = new int[32];
   private int flags;
   private Sprite sprite;
   private Direction direction;
   private int tintIdx;
   private int faceNormal;

   @Override
   public void setX(int idx, float x) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 0] = Float.floatToRawIntBits(x);
   }

   @Override
   public void setY(int idx, float y) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 0 + 1] = Float.floatToRawIntBits(y);
   }

   @Override
   public void setZ(int idx, float z) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 0 + 2] = Float.floatToRawIntBits(z);
   }

   @Override
   public void setColor(int idx, int color) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 3] = color;
   }

   @Override
   public void setTexU(int idx, float u) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 4] = Float.floatToRawIntBits(u);
   }

   @Override
   public void setTexV(int idx, float v) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 4 + 1] = Float.floatToRawIntBits(v);
   }

   @Override
   public void setLight(int idx, int light) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 6] = light;
   }

   @Override
   public void setNormal(int idx, int normal) {
      this.data[ModelQuadUtil.vertexOffset(idx) + 7] = normal;
   }

   @Override
   public void setFaceNormal(int normal) {
      this.faceNormal = normal;
   }

   @Override
   public void setFlags(int flags) {
      this.flags = flags;
   }

   @Override
   public void setSprite(Sprite sprite) {
      this.sprite = sprite;
   }

   @Override
   public void setTintIndex(int index) {
      this.tintIdx = index;
   }

   @Override
   public void setLightFace(Direction direction) {
      this.direction = direction;
   }

   @Override
   public int getTintIndex() {
      return this.tintIdx;
   }

   @Override
   public float getX(int idx) {
      return Float.intBitsToFloat(this.data[ModelQuadUtil.vertexOffset(idx) + 0]);
   }

   @Override
   public float getY(int idx) {
      return Float.intBitsToFloat(this.data[ModelQuadUtil.vertexOffset(idx) + 0 + 1]);
   }

   @Override
   public float getZ(int idx) {
      return Float.intBitsToFloat(this.data[ModelQuadUtil.vertexOffset(idx) + 0 + 2]);
   }

   @Override
   public int getColor(int idx) {
      return this.data[ModelQuadUtil.vertexOffset(idx) + 3];
   }

   @Override
   public float getTexU(int idx) {
      return Float.intBitsToFloat(this.data[ModelQuadUtil.vertexOffset(idx) + 4]);
   }

   @Override
   public float getTexV(int idx) {
      return Float.intBitsToFloat(this.data[ModelQuadUtil.vertexOffset(idx) + 4 + 1]);
   }

   @Override
   public int getVertexNormal(int idx) {
      return this.data[ModelQuadUtil.vertexOffset(idx) + 7];
   }

   @Override
   public int getFaceNormal() {
      return this.faceNormal;
   }

   @Override
   public int getLight(int idx) {
      return this.data[ModelQuadUtil.vertexOffset(idx) + 6];
   }

   @Override
   public int getFlags() {
      return this.flags;
   }

   @Override
   public Sprite getSprite() {
      return this.sprite;
   }

   @Override
   public Direction getLightFace() {
      return this.direction;
   }

   @Override
   public int getMaxLightQuad(int idx) {
      return this.getLight(idx);
   }
}
