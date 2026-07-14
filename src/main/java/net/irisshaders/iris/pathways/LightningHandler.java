package net.irisshaders.iris.pathways;

import java.util.function.Function;
import net.irisshaders.iris.layer.InnerWrappedRenderType;
import net.irisshaders.iris.layer.LightningRenderStateShard;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.ShaderProgram;
import net.minecraft.client.render.RenderPhase.Texture;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;

public class LightningHandler extends RenderLayer {
   public static final RenderLayer IRIS_LIGHTNING = new InnerWrappedRenderType(
      "iris_lightning2",
      RenderLayer.of(
         "iris_lightning",
         VertexFormats.POSITION_COLOR,
         DrawMode.QUADS,
         256,
         false,
         true,
         MultiPhaseParameters.builder()
            .program(LIGHTNING_PROGRAM)
            .writeMaskState(ALL_MASK)
            .transparency(LIGHTNING_TRANSPARENCY)
            .target(WEATHER_TARGET)
            .build(false)
      ),
      new LightningRenderStateShard()
   );
   public static final Function<Identifier, RenderLayer> MEKANISM_FLAME = Util.memoize(
      resourceLocation -> {
         MultiPhaseParameters state = MultiPhaseParameters.builder()
            .program(new ShaderProgram(ShaderAccess.MEKANISM_FLAME))
            .texture(new Texture(resourceLocation, TriState.DEFAULT, false))
            .transparency(TRANSLUCENT_TRANSPARENCY)
            .build(true);
         return of("mek_flame", VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS, 256, true, false, state);
      }
   );
   public static final RenderLayer MEKASUIT = of(
      "mekasuit",
      VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
      DrawMode.QUADS,
      131072,
      true,
      false,
      MultiPhaseParameters.builder()
         .program(new ShaderProgram(ShaderAccess.MEKASUIT))
         .texture(BLOCK_ATLAS_TEXTURE)
         .lightmap(ENABLE_LIGHTMAP)
         .overlay(ENABLE_OVERLAY_COLOR)
         .build(true)
   );
   public static final Function<Identifier, RenderLayer> SPS = Util.memoize(
      r -> of(
         "sps",
         VertexFormats.POSITION_TEXTURE_COLOR,
         DrawMode.QUADS,
         1536,
         true,
         false,
         MultiPhaseParameters.builder()
            .program(new ShaderProgram(ShaderAccess.MEKANISM_FLAME))
            .texture(new Texture(r, TriState.DEFAULT, false))
            .transparency(RenderPhase.LIGHTNING_TRANSPARENCY)
            .build(true)
      )
   );

   public LightningHandler(
      String pRenderType0,
      VertexFormat pVertexFormat1,
      DrawMode pVertexFormat$Mode2,
      int pInt3,
      boolean pBoolean4,
      boolean pBoolean5,
      Runnable pRunnable6,
      Runnable pRunnable7
   ) {
      super(pRenderType0, pVertexFormat1, pVertexFormat$Mode2, pInt3, pBoolean4, pBoolean5, pRunnable6, pRunnable7);
   }
}
