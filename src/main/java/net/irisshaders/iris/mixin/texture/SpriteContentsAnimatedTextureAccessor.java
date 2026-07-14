package net.irisshaders.iris.mixin.texture;

import java.util.List;
import net.minecraft.client.texture.SpriteContents.AnimationFrame;

public interface SpriteContentsAnimatedTextureAccessor {
   List<AnimationFrame> getFrames();

   void invokeUploadFrame(int x, int y, int frame);
}
