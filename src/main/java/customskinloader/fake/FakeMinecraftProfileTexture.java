package customskinloader.fake;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import customskinloader.utils.HttpTextureUtil;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.math.ColorHelper;

public class FakeMinecraftProfileTexture extends MinecraftProfileTexture {
   private static final Map<String, String> MODEL_CACHE = new ConcurrentHashMap<>();
   private final HttpTextureUtil.HttpTextureInfo info;
   private final Map<String, String> metadata;
   private final CompletableFuture<Void> model = new CompletableFuture<>();

   public FakeMinecraftProfileTexture(String url, Map<String, String> metadata) {
      super(url, metadata);
      this.info = HttpTextureUtil.toHttpTextureInfo(url);
      this.metadata = metadata;
   }

   @Override
   public String getUrl() {
      return this.info.url;
   }

   @Override
   public String getMetadata(String key) {
      String value = super.getMetadata(key);
      if ("model".equals(key) && "auto".equals(value)) {
         String detected = MODEL_CACHE.get(this.getHash());
         if (detected != null) return detected;
      }
      return value;
   }

   public void setModel(String model) {
      if (this.metadata != null) {
         MODEL_CACHE.put(this.getHash(), model);
         this.metadata.put("model", model);
         this.model.complete(null);
      }
   }

   public boolean needsModelDetection() {
      return "auto".equals(super.getMetadata("model")) && !MODEL_CACHE.containsKey(this.getHash());
   }

   public void detectModel(NativeImage image) {
      if (!this.needsModelDetection() || image.getWidth() < 64 || image.getHeight() < 32) return;
      int ratio = Math.max(1, image.getWidth() / 64);
      int background = image.getColorArgb(63 * ratio, 20 * ratio);
      boolean transparentBackground = ColorHelper.getAlpha(background) == 0;
      for (int x = 54 * ratio; x <= 55 * ratio; x++) {
         for (int y = 20 * ratio; y <= 31 * ratio; y++) {
            int color = image.getColorArgb(x, y);
            if (transparentBackground ? ColorHelper.getAlpha(color) != 0 : color != background) {
               this.setModel("default");
               return;
            }
         }
      }
      this.setModel("slim");
   }

   @Override
   public String getHash() {
      return this.info.hash == null ? super.getHash() : this.info.hash;
   }

   public File getCacheFile() {
      return this.info.cacheFile;
   }
}
