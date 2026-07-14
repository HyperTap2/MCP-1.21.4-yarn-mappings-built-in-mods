package net.irisshaders.iris.mixin.texture;

import net.minecraft.client.texture.SpriteContents.Animation;

public interface SpriteContentsTickerAccessor {
   int getFrame();

   void setFrame(int frame);

   int getSubFrame();

   void setSubFrame(int subFrame);

   Animation getAnimationInfo();
}
