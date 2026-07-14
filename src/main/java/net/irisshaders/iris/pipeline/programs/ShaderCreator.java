package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.serialization.JsonOps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.fallback.ShaderSynthesizer;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.VanillaUniforms;
import net.irisshaders.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.gl.ShaderProgramDefinition;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

public class ShaderCreator {
   public static ShaderSupplier create(
      WorldRenderingPipeline pipeline,
      String name,
      ShaderKey shaderKey,
      ProgramSource source,
      ProgramId programId,
      GlFramebuffer writingToBeforeTranslucent,
      GlFramebuffer writingToAfterTranslucent,
      AlphaTest fallbackAlpha,
      VertexFormat vertexFormat,
      ShaderAttributeInputs inputs,
      FrameUpdateNotifier updateNotifier,
      IrisRenderingPipeline parent,
      Supplier<ImmutableSet<Integer>> flipped,
      FogMode fogMode,
      boolean isIntensity,
      boolean isFullbright,
      boolean isShadowPass,
      boolean isLines,
      CustomUniforms customUniforms
   ) throws IOException {
      AlphaTest alpha = source.getDirectives().getAlphaTestOverride().orElse(fallbackAlpha);
      BlendModeOverride blendModeOverride = source.getDirectives().getBlendModeOverride().orElse(programId.getBlendModeOverride());
      Map<PatchShaderType, String> transformed = TransformPatcher.patchVanilla(
         name,
         source.getVertexSource().orElseThrow(RuntimeException::new),
         source.getGeometrySource().orElse(null),
         source.getTessControlSource().orElse(null),
         source.getTessEvalSource().orElse(null),
         source.getFragmentSource().orElseThrow(RuntimeException::new),
         alpha,
         isLines,
         true,
         inputs,
         pipeline.getTextureMap()
      );
      String vertex = transformed.get(PatchShaderType.VERTEX);
      String geometry = transformed.get(PatchShaderType.GEOMETRY);
      String tessControl = transformed.get(PatchShaderType.TESS_CONTROL);
      String tessEval = transformed.get(PatchShaderType.TESS_EVAL);
      String fragment = transformed.get(PatchShaderType.FRAGMENT);
      String shaderJsonString = String.format(
         "    {\n    \"blend\": {\n        \"func\": \"add\",\n        \"srcrgb\": \"srcalpha\",\n        \"dstrgb\": \"1-srcalpha\"\n    },\n    \"vertex\": \"%s\",\n    \"fragment\": \"%s\",\n    \"attributes\": [\n        \"Position\",\n        \"Color\",\n        \"UV0\",\n        \"UV1\",\n        \"UV2\",\n        \"Normal\"\n    ],\n    \"uniforms\": [\n        { \"name\": \"iris_TextureMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        { \"name\": \"iris_ModelViewMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        { \"name\": \"iris_ModelViewMatInverse\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        { \"name\": \"iris_ProjMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        { \"name\": \"iris_ProjMatInverse\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        { \"name\": \"iris_NormalMat\", \"type\": \"matrix3x3\", \"count\": 9, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 ] },\n        { \"name\": \"iris_ModelOffset\", \"type\": \"float\", \"count\": 3, \"values\": [ 0.0, 0.0, 0.0 ] },\n        { \"name\": \"iris_ColorModulator\", \"type\": \"float\", \"count\": 4, \"values\": [ 1.0, 1.0, 1.0, 1.0 ] },\n        { \"name\": \"iris_GlintAlpha\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        { \"name\": \"iris_FogStart\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n        { \"name\": \"iris_FogEnd\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        { \"name\": \"iris_FogColor\", \"type\": \"float\", \"count\": 4, \"values\": [ 0.0, 0.0, 0.0, 0.0 ] },\n        {\n                    \"name\": \"iris_OverlayUV\",\n                    \"type\": \"int\",\n                    \"count\": 2,\n                    \"values\": [\n                        0,\n                        0\n                    ]\n                },\n                {\n                    \"name\": \"iris_LightUV\",\n                    \"type\": \"int\",\n                    \"count\": 2,\n                    \"values\": [\n                        0,\n                        0\n                    ]\n                }\n    ]\n}",
         name,
         name
      );
      ShaderPrinter.printProgram(name).addSources(transformed).addJson(shaderJsonString).print();
      ResourceFactory shaderResourceFactory = new ShaderCreator.IrisProgramResourceFactory(shaderJsonString, vertex, geometry, tessControl, tessEval, fragment);
      List<BufferBlendOverride> overrides = new ArrayList<>();
      source.getDirectives().getBufferBlendOverrides().forEach(information -> {
         int index = Ints.indexOf(source.getDirectives().getDrawBuffers(), information.index());
         if (index > -1) {
            overrides.add(new BufferBlendOverride(index, information.blendMode()));
         }
      });
      int id = link(name, vertex, geometry, tessControl, tessEval, fragment, vertexFormat, false);
      return new ShaderSupplier(
         shaderKey,
         id,
         () -> {
            try {
               return new ExtendedShader(
                  id,
                  shaderResourceFactory,
                  name,
                  vertexFormat,
                  tessControl != null || tessEval != null,
                  writingToBeforeTranslucent,
                  writingToAfterTranslucent,
                  blendModeOverride,
                  alpha,
                  uniforms -> {
                     CommonUniforms.addDynamicUniforms(uniforms, FogMode.PER_VERTEX);
                     customUniforms.assignTo(uniforms);
                     BuiltinReplacementUniforms.addBuiltinReplacementUniforms(uniforms);
                     VanillaUniforms.addVanillaUniforms(uniforms);
                  },
                  (samplerHolder, imageHolder) -> parent.addGbufferOrShadowSamplers(
                     samplerHolder, imageHolder, flipped, isShadowPass, inputs.hasTex(), inputs.hasLight(), inputs.hasOverlay()
                  ),
                  isIntensity,
                  parent,
                  overrides,
                  customUniforms
               );
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         }
      );
   }

   public static int link(
      String name, String vertex, String geometry, String tessControl, String tessEval, String fragment, VertexFormat vertexFormat, boolean isFallback
   ) throws ShaderCompileException {
      int i = GlStateManager.glCreateProgram();
      if (i <= 0) {
         throw new RuntimeException("Could not create shader program (returned program ID " + i + ")");
      }

      int vertexS = createShader(name, ShaderType.VERTEX, vertex);
      int geometryS = createShader(name, ShaderType.GEOMETRY, geometry);
      int tessContS = createShader(name, ShaderType.TESSELATION_CONTROL, tessControl);
      int tessEvalS = createShader(name, ShaderType.TESSELATION_EVAL, tessEval);
      int fragS = createShader(name, ShaderType.FRAGMENT, fragment);
      attachIfValid(i, vertexS);
      attachIfValid(i, geometryS);
      attachIfValid(i, tessContS);
      attachIfValid(i, tessEvalS);
      attachIfValid(i, fragS);
      if (isFallback) {
         vertexFormat.bindAttributes(i);
      } else {
         ((VertexFormatExtension)vertexFormat).bindAttributesIris(i);
      }

      GlStateManager.glLinkProgram(i);
      detachIfValid(i, vertexS);
      detachIfValid(i, geometryS);
      detachIfValid(i, tessContS);
      detachIfValid(i, tessEvalS);
      detachIfValid(i, fragS);
      return i;
   }

   private static void attachIfValid(int i, int s) {
      if (s >= 0) {
         GlStateManager.glAttachShader(i, s);
      }
   }

   private static void detachIfValid(int i, int s) {
      if (s >= 0) {
         IrisRenderSystem.detachShader(i, s);
         GlStateManager.glDeleteShader(s);
      }
   }

   private static int createShader(String name, ShaderType shaderType, String source) {
      if (source == null) {
         return -1;
      }

      int shader = GlStateManager.glCreateShader(shaderType.id);
      GlStateManager.glShaderSource(shader, source);
      GlStateManager.glCompileShader(shader);
      String log = IrisRenderSystem.getShaderInfoLog(shader);
      if (!log.isEmpty()) {
         Iris.logger.warn("Shader compilation log for " + name + ": " + log);
      }

      int result = GlStateManager.glGetShaderi(shader, 35713);
      if (result != 1) {
         throw new ShaderCompileException(name, log);
      } else {
         return shader;
      }
   }

   public static ShaderSupplier createFallback(
      String name,
      ShaderKey shaderKey,
      GlFramebuffer writingToBeforeTranslucent,
      GlFramebuffer writingToAfterTranslucent,
      AlphaTest alpha,
      VertexFormat vertexFormat,
      BlendModeOverride blendModeOverride,
      IrisRenderingPipeline parent,
      FogMode fogMode,
      boolean entityLighting,
      boolean isGlint,
      boolean isText,
      boolean intensityTex,
      boolean isFullbright
   ) throws IOException {
      ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright, false, isGlint, isText, false);
      boolean isLeash = vertexFormat == VertexFormats.POSITION_COLOR_LIGHT;
      String vertex = ShaderSynthesizer.vsh(true, inputs, fogMode, entityLighting, isLeash);
      String fragment = ShaderSynthesizer.fsh(inputs, fogMode, alpha, intensityTex, isLeash);
      String shaderJsonString = String.format(
         "    {\n    \"blend\": {\n        \"func\": \"add\",\n        \"srcrgb\": \"srcalpha\",\n        \"dstrgb\": \"1-srcalpha\"\n    },\n    \"vertex\": \"%s\",\n    \"fragment\": \"%s\",\n    \"attributes\": [\n        \"Position\",\n        \"Color\",\n        \"UV0\",\n        \"UV1\",\n        \"UV2\",\n        \"Normal\"\n    ],\n    \"uniforms\": [\n        \t\t{ \"name\": \"TextureMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ModelViewMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ProjMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ModelOffset\", \"type\": \"float\", \"count\": 3, \"values\": [ 0.0, 0.0, 0.0 ] },\n        \t\t{ \"name\": \"ColorModulator\", \"type\": \"float\", \"count\": 4, \"values\": [ 1.0, 1.0, 1.0, 1.0 ] },\n        \t\t{ \"name\": \"GlintAlpha\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"Light0_Direction\", \"type\": \"float\", \"count\": 3, \"values\": [0.0, 0.0, 0.0] },\n        \t\t{ \"name\": \"Light1_Direction\", \"type\": \"float\", \"count\": 3, \"values\": [0.0, 0.0, 0.0] },\n        \t\t{ \"name\": \"FogStart\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n        \t\t{ \"name\": \"FogEnd\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"FogDensity\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"FogIsExp2\", \"type\": \"int\", \"count\": 1, \"values\": [ 0 ] },\n        \t\t{ \"name\": \"AlphaTestValue\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n        \t\t{ \"name\": \"LineWidth\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"ScreenSize\", \"type\": \"float\", \"count\": 2, \"values\": [ 1.0, 1.0 ] },\n        \t\t{ \"name\": \"FogColor\", \"type\": \"float\", \"count\": 4, \"values\": [ 0.0, 0.0, 0.0, 0.0 ] }\n    ]\n}",
         name,
         name
      );
      ShaderPrinter.printProgram(name).addSource(PatchShaderType.VERTEX, vertex).addSource(PatchShaderType.FRAGMENT, fragment).print();
      JsonElement jsonElement = JsonParser.parseString(shaderJsonString);
      ShaderProgramDefinition shaderProgramConfig = (ShaderProgramDefinition)ShaderProgramDefinition.CODEC
         .parse(JsonOps.INSTANCE, jsonElement)
         .getOrThrow(JsonSyntaxException::new);
      ResourceFactory shaderResourceFactory = new ShaderCreator.IrisProgramResourceFactory(shaderJsonString, vertex, null, null, null, fragment);
      int id = link(name, vertex, null, null, null, fragment, vertexFormat, true);
      GLDebug.nameObject(33506, id, name + "_fallback");
      return new ShaderSupplier(
         shaderKey,
         id,
         () -> {
            try {
               return new FallbackShader(
                  id,
                  shaderProgramConfig,
                  shaderResourceFactory,
                  name,
                  vertexFormat,
                  writingToBeforeTranslucent,
                  writingToAfterTranslucent,
                  blendModeOverride,
                  alpha.reference(),
                  parent
               );
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         }
      );
   }

   public static ShaderSupplier createFallbackShadow(
      String name,
      ShaderKey shaderKey,
      Supplier<ShadowRenderTargets> shadowSupplier,
      AlphaTest alpha,
      VertexFormat vertexFormat,
      BlendModeOverride blendModeOverride,
      IrisRenderingPipeline parent,
      FogMode fogMode,
      boolean entityLighting,
      boolean isGlint,
      boolean isText,
      boolean intensityTex,
      boolean isFullbright
   ) throws IOException {
      ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright, false, isGlint, isText, false);
      boolean isLeash = vertexFormat == VertexFormats.POSITION_COLOR_LIGHT;
      String vertex = ShaderSynthesizer.vsh(true, inputs, fogMode, entityLighting, isLeash);
      String fragment = ShaderSynthesizer.fsh(inputs, fogMode, alpha, intensityTex, isLeash);
      String shaderJsonString = String.format(
         "    {\n    \"blend\": {\n        \"func\": \"add\",\n        \"srcrgb\": \"srcalpha\",\n        \"dstrgb\": \"1-srcalpha\"\n    },\n    \"vertex\": \"%s\",\n    \"fragment\": \"%s\",\n    \"attributes\": [\n        \"Position\",\n        \"Color\",\n        \"UV0\",\n        \"UV1\",\n        \"UV2\",\n        \"Normal\"\n    ],\n    \"uniforms\": [\n        \t\t{ \"name\": \"TextureMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ModelViewMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ProjMat\", \"type\": \"matrix4x4\", \"count\": 16, \"values\": [ 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0 ] },\n        \t\t{ \"name\": \"ModelOffset\", \"type\": \"float\", \"count\": 3, \"values\": [ 0.0, 0.0, 0.0 ] },\n        \t\t{ \"name\": \"ColorModulator\", \"type\": \"float\", \"count\": 4, \"values\": [ 1.0, 1.0, 1.0, 1.0 ] },\n        \t\t{ \"name\": \"GlintAlpha\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"Light0_Direction\", \"type\": \"float\", \"count\": 3, \"values\": [0.0, 0.0, 0.0] },\n        \t\t{ \"name\": \"Light1_Direction\", \"type\": \"float\", \"count\": 3, \"values\": [0.0, 0.0, 0.0] },\n        \t\t{ \"name\": \"FogStart\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n        \t\t{ \"name\": \"FogEnd\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"FogDensity\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"FogIsExp2\", \"type\": \"int\", \"count\": 1, \"values\": [ 0 ] },\n        \t\t{ \"name\": \"AlphaTestValue\", \"type\": \"float\", \"count\": 1, \"values\": [ 0.0 ] },\n        \t\t{ \"name\": \"LineWidth\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ] },\n        \t\t{ \"name\": \"ScreenSize\", \"type\": \"float\", \"count\": 2, \"values\": [ 1.0, 1.0 ] },\n        \t\t{ \"name\": \"FogColor\", \"type\": \"float\", \"count\": 4, \"values\": [ 0.0, 0.0, 0.0, 0.0 ] }\n    ]\n}",
         name,
         name
      );
      ShaderPrinter.printProgram(name).addSource(PatchShaderType.VERTEX, vertex).addSource(PatchShaderType.FRAGMENT, fragment).print();
      JsonElement jsonElement = JsonParser.parseString(shaderJsonString);
      ShaderProgramDefinition shaderProgramConfig = (ShaderProgramDefinition)ShaderProgramDefinition.CODEC
         .parse(JsonOps.INSTANCE, jsonElement)
         .getOrThrow(JsonSyntaxException::new);
      ResourceFactory shaderResourceFactory = new ShaderCreator.IrisProgramResourceFactory(shaderJsonString, vertex, null, null, null, fragment);
      int id = link(name, vertex, null, null, null, fragment, vertexFormat, true);
      return new ShaderSupplier(
         shaderKey,
         id,
         () -> {
            try {
               GlFramebuffer framebuffer = shadowSupplier.get().createShadowFramebuffer(ImmutableSet.of(), new int[]{0});
               return new FallbackShader(
                  id, shaderProgramConfig, shaderResourceFactory, name, vertexFormat, framebuffer, framebuffer, blendModeOverride, alpha.reference(), parent
               );
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         }
      );
   }

   public static ShaderSupplier createShadow(
      WorldRenderingPipeline pipeline,
      String name,
      ShaderKey shaderKey,
      ProgramSource source,
      ProgramId programId,
      Supplier<ShadowRenderTargets> shadowSupplier,
      AlphaTest fallbackAlpha,
      VertexFormat vertexFormat,
      ShaderAttributeInputs inputs,
      FrameUpdateNotifier updateNotifier,
      IrisRenderingPipeline parent,
      Supplier<ImmutableSet<Integer>> flipped,
      FogMode fogMode,
      boolean isIntensity,
      boolean isFullbright,
      boolean isShadowPass,
      boolean isLines,
      CustomUniforms customUniforms
   ) throws IOException {
      AlphaTest alpha = source.getDirectives().getAlphaTestOverride().orElse(fallbackAlpha);
      BlendModeOverride blendModeOverride = source.getDirectives().getBlendModeOverride().orElse(programId.getBlendModeOverride());
      Map<PatchShaderType, String> transformed = TransformPatcher.patchVanilla(
         name,
         source.getVertexSource().orElseThrow(RuntimeException::new),
         source.getGeometrySource().orElse(null),
         source.getTessControlSource().orElse(null),
         source.getTessEvalSource().orElse(null),
         source.getFragmentSource().orElseThrow(RuntimeException::new),
         alpha,
         isLines,
         true,
         inputs,
         pipeline.getTextureMap()
      );
      String vertex = transformed.get(PatchShaderType.VERTEX);
      String geometry = transformed.get(PatchShaderType.GEOMETRY);
      String tessControl = transformed.get(PatchShaderType.TESS_CONTROL);
      String tessEval = transformed.get(PatchShaderType.TESS_EVAL);
      String fragment = transformed.get(PatchShaderType.FRAGMENT);
      ShaderPrinter.printProgram(name).addSources(transformed).print();
      ResourceFactory shaderResourceFactory = new ShaderCreator.IrisProgramResourceFactory("", vertex, geometry, tessControl, tessEval, fragment);
      List<BufferBlendOverride> overrides = new ArrayList<>();
      source.getDirectives().getBufferBlendOverrides().forEach(information -> {
         int index = Ints.indexOf(source.getDirectives().getDrawBuffers(), information.index());
         if (index > -1) {
            overrides.add(new BufferBlendOverride(index, information.blendMode()));
         }
      });
      int id = link(name, vertex, geometry, tessControl, tessEval, fragment, vertexFormat, false);
      return new ShaderSupplier(
         shaderKey,
         id,
         () -> {
            GlFramebuffer framebuffer = shadowSupplier.get()
               .createShadowFramebuffer(
                  ImmutableSet.of(), source.getDirectives().hasUnknownDrawBuffers() ? new int[]{0, 1} : source.getDirectives().getDrawBuffers()
               );

            try {
               return new ExtendedShader(
                  id,
                  shaderResourceFactory,
                  name,
                  vertexFormat,
                  tessControl != null || tessEval != null,
                  framebuffer,
                  framebuffer,
                  blendModeOverride,
                  alpha,
                  uniforms -> {
                     CommonUniforms.addDynamicUniforms(uniforms, FogMode.PER_VERTEX);
                     customUniforms.assignTo(uniforms);
                     BuiltinReplacementUniforms.addBuiltinReplacementUniforms(uniforms);
                     VanillaUniforms.addVanillaUniforms(uniforms);
                  },
                  (samplerHolder, imageHolder) -> parent.addGbufferOrShadowSamplers(
                     samplerHolder, imageHolder, flipped, isShadowPass, inputs.hasTex(), inputs.hasLight(), inputs.hasOverlay()
                  ),
                  isIntensity,
                  parent,
                  overrides,
                  customUniforms
               );
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         }
      );
   }

   private record IrisProgramResourceFactory(String json, String vertex, String geometry, String tessControl, String tessEval, String fragment)
      implements ResourceFactory {
      public Optional<Resource> getResource(Identifier id) {
         String path = id.getPath();
         if (path.endsWith("json")) {
            return Optional.of(new ShaderCreator.StringResource(id, this.json));
         } else if (path.endsWith("vsh")) {
            return Optional.of(new ShaderCreator.StringResource(id, this.vertex));
         } else if (path.endsWith("gsh")) {
            return this.geometry == null ? Optional.empty() : Optional.of(new ShaderCreator.StringResource(id, this.geometry));
         } else if (path.endsWith("tcs")) {
            return this.tessControl == null ? Optional.empty() : Optional.of(new ShaderCreator.StringResource(id, this.tessControl));
         } else if (path.endsWith("tes")) {
            return this.tessEval == null ? Optional.empty() : Optional.of(new ShaderCreator.StringResource(id, this.tessEval));
         } else {
            return path.endsWith("fsh") ? Optional.of(new ShaderCreator.StringResource(id, this.fragment)) : Optional.empty();
         }
      }
   }

   private static class StringResource extends Resource {
      private final String content;

      private StringResource(Identifier id, String content) {
         super(
            new DirectoryResourcePack(
               new ResourcePackInfo(
                  "<iris shaderpack shaders>", Text.literal("iris"), ResourcePackSource.BUILTIN, Optional.of(new VersionedIdentifier("iris", "shader", "1.0"))
               ),
               IrisPlatformHelpers.getInstance().getConfigDir()
            ),
            () -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
         );
         this.content = content;
      }

      public InputStream getInputStream() {
         return IOUtils.toInputStream(this.content, StandardCharsets.UTF_8);
      }
   }
}
