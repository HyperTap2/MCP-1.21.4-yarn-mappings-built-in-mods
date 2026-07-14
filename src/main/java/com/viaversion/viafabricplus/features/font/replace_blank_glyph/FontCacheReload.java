package com.viaversion.viafabricplus.features.font.replace_blank_glyph;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;

public final class FontCacheReload {
   public static void reload() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null) {
         for (FontStorage storage : client.fontManager.fontStorages.values()) {
            storage.bakedGlyphCache.clear();
            storage.glyphCache.clear();
         }
      }
   }
}
