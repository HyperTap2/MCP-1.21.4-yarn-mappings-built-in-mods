package net.minecraft.client.util;

import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class RawTextureDataLoader {
   @Deprecated
   public static int[] loadRawTextureData(ResourceManager resourceManager, Identifier id) throws IOException {
      try (
         InputStream inputStream = resourceManager.open(id);
         NativeImage nativeImage = NativeImage.read(inputStream);
      ) {
         return nativeImage.makePixelArray();
      }
   }
}
