package net.irisshaders.iris.pbr.loader;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.jetbrains.annotations.Nullable;

public class PBRTextureLoaderRegistry {
   public static final PBRTextureLoaderRegistry INSTANCE = new PBRTextureLoaderRegistry();
   private final Map<Class<?>, PBRTextureLoader<?>> loaderMap = new HashMap<>();

   public <T extends AbstractTexture> void register(Class<? extends T> clazz, PBRTextureLoader<T> loader) {
      this.loaderMap.put(clazz, loader);
   }

   @Nullable
   public <T extends AbstractTexture> PBRTextureLoader<T> getLoader(Class<? extends T> clazz) {
      return (PBRTextureLoader<T>)this.loaderMap.get(clazz);
   }

   static {
      INSTANCE.register(ResourceTexture.class, new SimplePBRLoader());
      INSTANCE.register(SpriteAtlasTexture.class, new AtlasPBRLoader());
   }
}
