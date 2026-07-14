package net.irisshaders.iris.pipeline;

import net.minecraft.client.render.RenderLayer;

public enum WorldRenderingPhase {
   NONE,
   SKY,
   SUNSET,
   CUSTOM_SKY,
   SUN,
   MOON,
   STARS,
   VOID,
   TERRAIN_SOLID,
   TERRAIN_CUTOUT_MIPPED,
   TERRAIN_CUTOUT,
   ENTITIES,
   BLOCK_ENTITIES,
   DESTROY,
   OUTLINE,
   DEBUG,
   HAND_SOLID,
   TERRAIN_TRANSLUCENT,
   TRIPWIRE,
   PARTICLES,
   CLOUDS,
   RAIN_SNOW,
   WORLD_BORDER,
   HAND_TRANSLUCENT;

   public static WorldRenderingPhase fromTerrainRenderType(RenderLayer renderType) {
      if (renderType == RenderLayer.getSolid()) {
         return TERRAIN_SOLID;
      } else if (renderType == RenderLayer.getCutout()) {
         return TERRAIN_CUTOUT;
      } else if (renderType == RenderLayer.getCutoutMipped()) {
         return TERRAIN_CUTOUT_MIPPED;
      } else if (renderType == RenderLayer.getTranslucent()) {
         return TERRAIN_TRANSLUCENT;
      } else if (renderType == RenderLayer.getTripwire()) {
         return TRIPWIRE;
      } else {
         throw new IllegalStateException("Illegal render type!");
      }
   }
}
