package net.caffeinemc.mods.sodium.client.model.quad;

import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public interface ModelQuadViewMutable extends ModelQuadView {
   void setX(int var1, float var2);

   void setY(int var1, float var2);

   void setZ(int var1, float var2);

   void setColor(int var1, int var2);

   void setTexU(int var1, float var2);

   void setTexV(int var1, float var2);

   void setLight(int var1, int var2);

   void setNormal(int var1, int var2);

   void setFaceNormal(int var1);

   void setFlags(int var1);

   void setSprite(Sprite var1);

   void setLightFace(Direction var1);

   void setTintIndex(int var1);
}
