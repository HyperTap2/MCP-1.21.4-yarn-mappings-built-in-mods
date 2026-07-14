package net.irisshaders.iris.fantastic;

import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.SpriteAtlasTexture;

public class IrisParticleRenderTypes {
   public static final ParticleTextureSheet TERRAIN_OPAQUE = new ParticleTextureSheet(
      "TERRAIN_OPAQUE", RenderLayer.getOpaqueParticle(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
   );
}
