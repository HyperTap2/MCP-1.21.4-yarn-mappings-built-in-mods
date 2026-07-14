package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.client.gl.GlObject;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.gl.shader.GlShader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram.Builder;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.sodium.terrain.FormatAnalyzer;
import net.minecraft.util.Identifier;

public class SodiumPrograms {
   private final EnumMap<SodiumPrograms.Pass, GlFramebuffer> framebuffers = new EnumMap<>(SodiumPrograms.Pass.class);
   private final EnumMap<SodiumPrograms.Pass, GlProgram<ChunkShaderInterface>> shaders = new EnumMap<>(SodiumPrograms.Pass.class);
   private boolean hasBlockId;
   private boolean hasMidUv;
   private boolean hasNormal;
   private boolean hasMidBlock;

   public SodiumPrograms(
      IrisRenderingPipeline pipeline,
      ProgramSet programSet,
      ProgramFallbackResolver resolver,
      RenderTargets renderTargets,
      Supplier<ShadowRenderTargets> shadowRenderTargets,
      CustomUniforms customUniforms
   ) {
      for (SodiumPrograms.Pass pass : SodiumPrograms.Pass.values()) {
         ProgramSource source = resolver.resolveNullable(pass.getOriginalId());
         Supplier<ImmutableSet<Integer>> flipState = this.getFlipState(
            pipeline, pass, pass == SodiumPrograms.Pass.SHADOW || pass == SodiumPrograms.Pass.SHADOW_CUTOUT
         );
         GlFramebuffer framebuffer = this.createFramebuffer(pass, source, shadowRenderTargets, renderTargets, flipState);
         this.framebuffers.put(pass, framebuffer);
         if (source != null) {
            AlphaTest alphaTest = this.getAlphaTest(pass, source);
            Map<PatchShaderType, String> transformed = this.transformShaders(source, alphaTest, programSet);
            GlProgram<ChunkShaderInterface> shader = this.createShader(
               pipeline, pass, source, alphaTest, customUniforms, flipState, this.createGlShaders(pass.name().toLowerCase(Locale.ROOT), transformed)
            );
            this.shaders.put(pass, shader);
         }
      }

      WorldRenderingSettings.INSTANCE.setVertexFormat(FormatAnalyzer.createFormat(this.hasBlockId, this.hasNormal, this.hasMidUv, this.hasMidBlock));
   }

   private AlphaTest getAlphaTest(SodiumPrograms.Pass pass, ProgramSource source) {
      return source.getDirectives()
         .getAlphaTestOverride()
         .orElse(pass != SodiumPrograms.Pass.TERRAIN_CUTOUT && pass != SodiumPrograms.Pass.SHADOW_CUTOUT ? AlphaTest.ALWAYS : AlphaTests.ONE_TENTH_ALPHA);
   }

   private Map<PatchShaderType, String> transformShaders(ProgramSource source, AlphaTest alphaTest, ProgramSet programSet) {
      Map<PatchShaderType, String> transformed = TransformPatcher.patchSodium(
         source.getName(),
         source.getVertexSource().orElse(null),
         source.getGeometrySource().orElse(null),
         source.getTessControlSource().orElse(null),
         source.getTessEvalSource().orElse(null),
         source.getFragmentSource().orElse(null),
         alphaTest,
         programSet.getPackDirectives().getTextureMap()
      );
      ShaderPrinter.printProgram("sodium_" + source.getName()).addSources(transformed).print();
      return transformed;
   }

   private Map<PatchShaderType, GlShader> createGlShaders(String passName, Map<PatchShaderType, String> transformed) {
      Map<PatchShaderType, GlShader> newMap = new EnumMap<>(PatchShaderType.class);

      for (Entry<PatchShaderType, String> entry : transformed.entrySet()) {
         if (entry.getValue() != null) {
            newMap.put(
               entry.getKey(),
               new GlShader(ShaderType.fromGlShaderType(entry.getKey().glShaderType.id), Identifier.of("iris", "sodium-shader-" + passName), entry.getValue())
            );
         }
      }

      return newMap;
   }

   private Supplier<ImmutableSet<Integer>> getFlipState(IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, boolean isShadowPass) {
      return isShadowPass
         ? pipeline::getFlippedBeforeShadow
         : () -> pass == SodiumPrograms.Pass.TRANSLUCENT ? pipeline.getFlippedAfterTranslucent() : pipeline.getFlippedAfterPrepare();
   }

   private GlProgram<ChunkShaderInterface> createShader(
      IrisRenderingPipeline pipeline,
      SodiumPrograms.Pass pass,
      ProgramSource source,
      AlphaTest alphaTest,
      CustomUniforms customUniforms,
      Supplier<ImmutableSet<Integer>> flipState,
      Map<PatchShaderType, GlShader> transformed
   ) {
      Builder builder = GlProgram.builder(Identifier.of("sodium", "chunk_shader_for_" + pass.name().toLowerCase(Locale.ROOT)));

      for (GlShader shader : transformed.values()) {
         builder.attachShader(shader);
      }

      boolean containsTessellation = source.getTessEvalSource().isPresent();

      try {
         return this.buildProgram(builder, pipeline, pass, source, alphaTest, customUniforms, flipState, containsTessellation);
      } finally {
         transformed.values().forEach(GlShader::delete);
      }
   }

   private GlFramebuffer createFramebuffer(
      SodiumPrograms.Pass pass,
      ProgramSource source,
      Supplier<ShadowRenderTargets> shadowRenderTargets,
      RenderTargets renderTargets,
      Supplier<ImmutableSet<Integer>> flipState
   ) {
      return pass != SodiumPrograms.Pass.SHADOW && pass != SodiumPrograms.Pass.SHADOW_CUTOUT && pass != SodiumPrograms.Pass.SHADOW_TRANS
         ? renderTargets.createGbufferFramebuffer(
            flipState.get(),
            source == null ? new int[]{0, 1} : (source.getDirectives().hasUnknownDrawBuffers() ? new int[]{0} : source.getDirectives().getDrawBuffers())
         )
         : shadowRenderTargets.get()
            .createShadowFramebuffer(
               ImmutableSet.of(),
               source == null ? new int[]{0, 1} : (source.getDirectives().hasUnknownDrawBuffers() ? new int[]{0, 1} : source.getDirectives().getDrawBuffers())
            );
   }

   private List<BufferBlendOverride> createBufferBlendOverrides(ProgramSource source) {
      List<BufferBlendOverride> overrides = new ArrayList<>();
      source.getDirectives().getBufferBlendOverrides().forEach(information -> {
         int index = Ints.indexOf(source.getDirectives().getDrawBuffers(), information.index());
         if (index > -1) {
            overrides.add(new BufferBlendOverride(index, information.blendMode()));
         }
      });
      return overrides;
   }

   private GlProgram<ChunkShaderInterface> buildProgram(
      Builder builder,
      IrisRenderingPipeline pipeline,
      SodiumPrograms.Pass pass,
      ProgramSource source,
      AlphaTest alphaTest,
      CustomUniforms customUniforms,
      Supplier<ImmutableSet<Integer>> flipState,
      boolean containsTessellation
   ) {
      return builder.bindAttribute("a_Position", 0)
         .bindAttribute("a_Color", 1)
         .bindAttribute("a_TexCoord", 2)
         .bindAttribute("a_LightAndData", 3)
         .bindAttribute("mc_Entity", 11)
         .bindAttribute("mc_midTexCoord", 12)
         .bindAttribute("at_tangent", 13)
         .bindAttribute("iris_Normal", 10)
         .bindAttribute("at_midBlock", 14)
         .link(
            shader -> {
               int handle = ((GlObject)shader).handle();
               GLDebug.nameObject(33506, handle, "sodium-terrain-" + pass.toString().toLowerCase(Locale.ROOT));
               if (!this.hasNormal) {
                  this.hasNormal = GlStateManager._glGetAttribLocation(handle, "iris_Normal") != -1;
               }

               if (!this.hasMidBlock) {
                  this.hasMidBlock = GlStateManager._glGetAttribLocation(handle, "at_midBlock") != -1;
               }

               if (!this.hasBlockId) {
                  this.hasBlockId = GlStateManager._glGetAttribLocation(handle, "mc_Entity") != -1;
               }

               if (!this.hasMidUv) {
                  this.hasMidUv = GlStateManager._glGetAttribLocation(handle, "mc_midTexCoord") != -1;
               }

               return new SodiumShader(
                  pipeline,
                  pass,
                  shader,
                  handle,
                  source.getDirectives().getBlendModeOverride().orElse(null),
                  this.createBufferBlendOverrides(source),
                  customUniforms,
                  flipState,
                  alphaTest.reference(),
                  containsTessellation
               );
            }
         );
   }

   public GlProgram<ChunkShaderInterface> getProgram(TerrainRenderPass pass) {
      SodiumPrograms.Pass pass2 = this.mapTerrainRenderPass(pass);
      return this.shaders.get(pass2);
   }

   public GlFramebuffer getFramebuffer(TerrainRenderPass pass) {
      SodiumPrograms.Pass pass2 = this.mapTerrainRenderPass(pass);
      return this.framebuffers.get(pass2);
   }

   private SodiumPrograms.Pass mapTerrainRenderPass(TerrainRenderPass pass) {
      if (pass == DefaultTerrainRenderPasses.SOLID) {
         return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW : SodiumPrograms.Pass.TERRAIN;
      } else if (pass == DefaultTerrainRenderPasses.CUTOUT) {
         return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW_CUTOUT : SodiumPrograms.Pass.TERRAIN_CUTOUT;
      } else if (pass == DefaultTerrainRenderPasses.TRANSLUCENT) {
         return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? SodiumPrograms.Pass.SHADOW_TRANS : SodiumPrograms.Pass.TRANSLUCENT;
      } else {
         throw new IllegalArgumentException("Unknown pass: " + pass);
      }
   }

   public enum Pass {
      SHADOW(ProgramId.ShadowSolid),
      SHADOW_CUTOUT(ProgramId.ShadowCutout),
      SHADOW_TRANS(ProgramId.ShadowWater),
      TERRAIN(ProgramId.TerrainSolid),
      TERRAIN_CUTOUT(ProgramId.TerrainCutout),
      TRANSLUCENT(ProgramId.Water);

      private final ProgramId originalId;

      Pass(ProgramId originalId) {
         this.originalId = originalId;
      }

      public ProgramId getOriginalId() {
         return this.originalId;
      }
   }
}
