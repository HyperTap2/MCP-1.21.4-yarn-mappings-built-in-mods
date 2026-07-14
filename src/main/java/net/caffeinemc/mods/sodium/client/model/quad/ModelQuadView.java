package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public interface ModelQuadView {
   float getX(int var1);

   float getY(int var1);

   float getZ(int var1);

   int getColor(int var1);

   float getTexU(int var1);

   float getTexV(int var1);

   int getVertexNormal(int var1);

   int getFaceNormal();

   int getLight(int var1);

   int getFlags();

   int getTintIndex();

   Sprite getSprite();

   Direction getLightFace();

   default boolean hasColor() {
      return this.getTintIndex() != -1;
   }

   default int calculateNormal() {
      float x0 = this.getX(0);
      float y0 = this.getY(0);
      float z0 = this.getZ(0);
      float x1 = this.getX(1);
      float y1 = this.getY(1);
      float z1 = this.getZ(1);
      float x2 = this.getX(2);
      float y2 = this.getY(2);
      float z2 = this.getZ(2);
      float x3 = this.getX(3);
      float y3 = this.getY(3);
      float z3 = this.getZ(3);
      float dx0 = x2 - x0;
      float dy0 = y2 - y0;
      float dz0 = z2 - z0;
      float dx1 = x3 - x1;
      float dy1 = y3 - y1;
      float dz1 = z3 - z1;
      float normX = dy0 * dz1 - dz0 * dy1;
      float normY = dz0 * dx1 - dx0 * dz1;
      float normZ = dx0 * dy1 - dy0 * dx1;
      float length = (float)Math.sqrt(normX * normX + normY * normY + normZ * normZ);
      if (length != 0.0 && length != 1.0) {
         normX /= length;
         normY /= length;
         normZ /= length;
      }

      return NormI8.pack(normX, normY, normZ);
   }

   default int getAccurateNormal(int i) {
      int normal = this.getVertexNormal(i);
      return normal == 0 ? this.getFaceNormal() : normal;
   }

   int getMaxLightQuad(int var1);
}
