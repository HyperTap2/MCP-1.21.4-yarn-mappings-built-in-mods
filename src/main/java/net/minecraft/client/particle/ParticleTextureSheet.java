package net.minecraft.client.particle;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.jetbrains.annotations.Nullable;

public record ParticleTextureSheet(String name, @Nullable RenderLayer renderType) {
   public static final ParticleTextureSheet TERRAIN_SHEET = new ParticleTextureSheet(
      "TERRAIN_SHEET", RenderLayer.getTranslucentParticle(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
   );
   public static final ParticleTextureSheet PARTICLE_SHEET_OPAQUE = new ParticleTextureSheet(
      "PARTICLE_SHEET_OPAQUE", RenderLayer.getOpaqueParticle(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
   );
   public static final ParticleTextureSheet PARTICLE_SHEET_TRANSLUCENT = new ParticleTextureSheet(
      "PARTICLE_SHEET_TRANSLUCENT", RenderLayer.getTranslucentParticle(SpriteAtlasTexture.PARTICLE_ATLAS_TEXTURE)
   );
   public static final ParticleTextureSheet CUSTOM = new ParticleTextureSheet("CUSTOM", null);
   public static final ParticleTextureSheet NO_RENDER = new ParticleTextureSheet("NO_RENDER", null);
}
