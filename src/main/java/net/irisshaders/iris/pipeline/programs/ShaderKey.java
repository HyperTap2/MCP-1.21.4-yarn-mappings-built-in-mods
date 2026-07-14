package net.irisshaders.iris.pipeline.programs;

import java.util.Locale;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public enum ShaderKey {
   BASIC(ProgramId.Basic, AlphaTests.OFF, VertexFormats.POSITION, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   BASIC_COLOR(ProgramId.Basic, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   TEXTURED(ProgramId.Textured, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_TEXTURE, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   TEXTURED_COLOR(ProgramId.Textured, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SPS(ProgramId.SpiderEyes, AlphaTests.OFF, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.PER_FRAGMENT, ShaderKey.LightingModel.FULLBRIGHT),
   SKY_BASIC(ProgramId.SkyBasic, AlphaTests.OFF, VertexFormats.POSITION, FogMode.OFF, ShaderKey.LightingModel.FULLBRIGHT),
   SKY_BASIC_COLOR(ProgramId.SkyBasic, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SKY_TEXTURED(ProgramId.SkyTextured, AlphaTests.OFF, VertexFormats.POSITION_TEXTURE, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SKY_TEXTURED_COLOR(ProgramId.SkyTextured, AlphaTests.OFF, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   CLOUDS(ProgramId.Clouds, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   CLOUDS_SODIUM(ProgramId.Clouds, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.CLOUDS, FogMode.PER_FRAGMENT, ShaderKey.LightingModel.LIGHTMAP),
   TERRAIN_SOLID(ProgramId.TerrainSolid, AlphaTests.OFF, IrisVertexFormats.TERRAIN, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TERRAIN_CUTOUT(ProgramId.TerrainCutout, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.TERRAIN, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TERRAIN_TRANSLUCENT(ProgramId.Water, AlphaTests.OFF, IrisVertexFormats.TERRAIN, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   MOVING_BLOCK(ProgramId.Block, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.TERRAIN, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   ENTITIES_ALPHA(ProgramId.Entities, AlphaTests.VERTEX_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   ENTITIES_SOLID(ProgramId.Entities, AlphaTests.OFF, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   ENTITIES_SOLID_DIFFUSE(ProgramId.Entities, AlphaTests.OFF, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   ENTITIES_SOLID_BRIGHT(ProgramId.Entities, AlphaTests.OFF, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   ENTITIES_CUTOUT(ProgramId.Entities, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   ENTITIES_CUTOUT_DIFFUSE(ProgramId.Entities, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   ENTITIES_TRANSLUCENT(ProgramId.EntitiesTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   ENTITIES_EYES(ProgramId.SpiderEyes, AlphaTests.NON_ZERO_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   ENTITIES_EYES_TRANS(ProgramId.SpiderEyes, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   HAND_CUTOUT(ProgramId.Hand, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   HAND_CUTOUT_BRIGHT(ProgramId.Hand, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   HAND_CUTOUT_DIFFUSE(ProgramId.Hand, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   HAND_TEXT(ProgramId.Hand, AlphaTests.NON_ZERO_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   HAND_TEXT_INTENSITY(ProgramId.Hand, AlphaTests.NON_ZERO_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   HAND_TRANSLUCENT(ProgramId.HandWater, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   HAND_WATER_BRIGHT(ProgramId.HandWater, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   HAND_WATER_DIFFUSE(ProgramId.HandWater, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   LIGHTNING(ProgramId.Lightning, AlphaTests.OFF, VertexFormats.POSITION_COLOR, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   LEASH(ProgramId.Basic, AlphaTests.OFF, VertexFormats.POSITION_COLOR_LIGHT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TEXT_BG(ProgramId.EntitiesTrans, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_COLOR_LIGHT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   PARTICLES(ProgramId.Particles, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   PARTICLES_TRANS(
      ProgramId.ParticlesTrans, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP
   ),
   WEATHER(ProgramId.Weather, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   CRUMBLING(ProgramId.DamagedBlock, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.TERRAIN, FogMode.OFF, ShaderKey.LightingModel.FULLBRIGHT),
   TEXT(ProgramId.EntitiesTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TEXT_INTENSITY(ProgramId.EntitiesTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TEXT_BE(ProgramId.BlockTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   TEXT_INTENSITY_BE(ProgramId.BlockTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.GLYPH, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   BLOCK_ENTITY(ProgramId.Block, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   BLOCK_ENTITY_BRIGHT(ProgramId.Block, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.FULLBRIGHT),
   BLOCK_ENTITY_DIFFUSE(ProgramId.Block, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   BE_TRANSLUCENT(ProgramId.BlockTrans, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.PER_VERTEX, ShaderKey.LightingModel.DIFFUSE_LM),
   BEACON(ProgramId.BeaconBeam, AlphaTests.OFF, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, FogMode.PER_FRAGMENT, ShaderKey.LightingModel.FULLBRIGHT),
   GLINT(ProgramId.ArmorGlint, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_TEXTURE, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   LINES(ProgramId.Line, AlphaTests.OFF, VertexFormats.LINES, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   IE_COMPAT(ProgramId.Block, AlphaTests.ONE_TENTH_ALPHA, ShaderAccess.IE_FORMAT, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   MEKANISM_FLAME(ProgramId.SpiderEyes, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.PER_VERTEX, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TERRAIN_CUTOUT(ProgramId.ShadowCutout, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.TERRAIN, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TRANSLUCENT(ProgramId.ShadowWater, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.TERRAIN, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_ENTITIES_CUTOUT(ProgramId.ShadowEntities, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_BLOCK(ProgramId.ShadowBlock, AlphaTests.ONE_TENTH_ALPHA, IrisVertexFormats.ENTITY, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_BEACON_BEAM(
      ProgramId.ShadowEntities, AlphaTests.OFF, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL, FogMode.OFF, ShaderKey.LightingModel.FULLBRIGHT
   ),
   SHADOW_BASIC(ProgramId.Shadow, AlphaTests.OFF, VertexFormats.POSITION, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_BASIC_COLOR(ProgramId.Shadow, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TEX(ProgramId.Shadow, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_TEXTURE, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TEX_COLOR(ProgramId.Shadow, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_CLOUDS(ProgramId.Shadow, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_LINES(ProgramId.Shadow, AlphaTests.OFF, VertexFormats.LINES, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_LEASH(ProgramId.Shadow, AlphaTests.OFF, VertexFormats.POSITION_COLOR_LIGHT, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_LIGHTNING(ProgramId.ShadowLightning, AlphaTests.OFF, VertexFormats.POSITION_COLOR, FogMode.OFF, ShaderKey.LightingModel.FULLBRIGHT),
   SHADOW_PARTICLES(ProgramId.Shadow, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TEXT(ProgramId.ShadowEntities, AlphaTests.NON_ZERO_ALPHA, IrisVertexFormats.GLYPH, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TEXT_BG(ProgramId.ShadowEntities, AlphaTests.NON_ZERO_ALPHA, VertexFormats.POSITION_COLOR_LIGHT, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   SHADOW_TEXT_INTENSITY(ProgramId.ShadowEntities, AlphaTests.NON_ZERO_ALPHA, IrisVertexFormats.GLYPH, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   IE_COMPAT_SHADOW(ProgramId.ShadowEntities, AlphaTests.ONE_TENTH_ALPHA, ShaderAccess.IE_FORMAT, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP),
   MEKANISM_FLAME_SHADOW(
      ProgramId.ShadowEntities, AlphaTests.ONE_TENTH_ALPHA, VertexFormats.POSITION_TEXTURE_COLOR, FogMode.OFF, ShaderKey.LightingModel.LIGHTMAP
   );

   private final ProgramId program;
   private final AlphaTest alphaTest;
   private final VertexFormat vertexFormat;
   private final FogMode fogMode;
   private final ShaderKey.LightingModel lightingModel;

   ShaderKey(ProgramId program, AlphaTest alphaTest, VertexFormat vertexFormat, FogMode fogMode, ShaderKey.LightingModel lightingModel) {
      this.program = program;
      this.alphaTest = alphaTest;
      this.vertexFormat = vertexFormat;
      this.fogMode = fogMode;
      this.lightingModel = lightingModel;
   }

   public ProgramId getProgram() {
      return this.program;
   }

   public AlphaTest getAlphaTest() {
      return this.alphaTest;
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public FogMode getFogMode() {
      return this.fogMode;
   }

   public boolean isIntensity() {
      return this == TEXT_INTENSITY || this == TEXT_INTENSITY_BE || this == SHADOW_TEXT_INTENSITY;
   }

   public String getName() {
      return this.toString().toLowerCase(Locale.ROOT);
   }

   public boolean isShadow() {
      return this.getProgram() == ProgramId.Shadow
         || this.getProgram() == ProgramId.ShadowCutout
         || this.getProgram() == ProgramId.ShadowWater
         || this.getProgram() == ProgramId.ShadowSolid
         || this.getProgram() == ProgramId.ShadowEntities
         || this.getProgram() == ProgramId.ShadowLightning
         || this.getProgram() == ProgramId.ShadowBlock;
   }

   public boolean hasDiffuseLighting() {
      return this.lightingModel == ShaderKey.LightingModel.DIFFUSE || this.lightingModel == ShaderKey.LightingModel.DIFFUSE_LM;
   }

   public boolean shouldIgnoreLightmap() {
      return this.lightingModel == ShaderKey.LightingModel.FULLBRIGHT || this.lightingModel == ShaderKey.LightingModel.DIFFUSE;
   }

   public boolean isGlint() {
      return this == GLINT;
   }

   public boolean isText() {
      return this.name().contains("TEXT");
   }

   enum LightingModel {
      FULLBRIGHT,
      LIGHTMAP,
      DIFFUSE,
      DIFFUSE_LM;
   }
}
