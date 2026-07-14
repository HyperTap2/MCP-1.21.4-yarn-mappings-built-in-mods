package net.caffeinemc.mods.sodium.api.vertex.format.common;

import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public final class ParticleVertex {
   public static final VertexFormat FORMAT = VertexFormats.POSITION_TEXTURE_COLOR_LIGHT;
   public static final int STRIDE = 28;
   private static final int OFFSET_POSITION = 0;
   private static final int OFFSET_TEXTURE = 12;
   private static final int OFFSET_COLOR = 20;
   private static final int OFFSET_LIGHT = 24;

   public static void put(long ptr, float x, float y, float z, float u, float v, int color, int light) {
      PositionAttribute.put(ptr + 0L, x, y, z);
      TextureAttribute.put(ptr + 12L, u, v);
      ColorAttribute.set(ptr + 20L, color);
      LightAttribute.set(ptr + 24L, light);
   }
}
