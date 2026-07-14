package net.irisshaders.iris.pbr.texture;

import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.Nullable;

public class PBRSpriteHolder {
   protected Sprite normalSprite;
   protected Sprite specularSprite;

   @Nullable
   public Sprite getNormalSprite() {
      return this.normalSprite;
   }

   public void setNormalSprite(Sprite sprite) {
      this.normalSprite = sprite;
   }

   @Nullable
   public Sprite getSpecularSprite() {
      return this.specularSprite;
   }

   public void setSpecularSprite(Sprite sprite) {
      this.specularSprite = sprite;
   }

   public void close() {
      if (this.normalSprite != null) {
         this.normalSprite.getContents().close();
      }

      if (this.specularSprite != null) {
         this.specularSprite.getContents().close();
      }
   }
}
