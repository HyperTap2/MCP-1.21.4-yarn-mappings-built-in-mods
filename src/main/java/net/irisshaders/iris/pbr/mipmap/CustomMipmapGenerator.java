package net.irisshaders.iris.pbr.mipmap;

import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;

public interface CustomMipmapGenerator {
   NativeImage[] generateMipLevels(NativeImage[] var1, int var2);

   interface Provider {
      @Nullable
      CustomMipmapGenerator getMipmapGenerator();
   }
}
