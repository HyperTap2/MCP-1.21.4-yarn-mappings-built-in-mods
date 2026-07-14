package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.framebuffer.ViewportData;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.gl.program.Program;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pathways.FullScreenQuadRenderer;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.samplers.IrisImages;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.FilledIndirectPointer;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackRenderTargetDirectives;
import net.irisshaders.iris.shaderpack.properties.ProgramDirectives;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.RenderTarget;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;

public class CompositeRenderer {
   private final RenderTargets renderTargets;
   private final ImmutableList<CompositeRenderer.Pass> passes;
   private final TextureAccess noiseTexture;
   private final CenterDepthSampler centerDepthSampler;
   private final Object2ObjectMap<String, TextureAccess> customTextureIds;
   private final ImmutableSet<Integer> flippedAtLeastOnceFinal;
   private final CustomUniforms customUniforms;
   private final Object2ObjectMap<String, TextureAccess> irisCustomTextures;
   private final Set<GlImage> customImages;
   private final TextureStage textureStage;
   private final WorldRenderingPipeline pipeline;
   private final CompositePass compositePass;

   public CompositeRenderer(
      WorldRenderingPipeline pipeline,
      CompositePass compositePass,
      PackDirectives packDirectives,
      ProgramSource[] sources,
      ComputeSource[][] computes,
      RenderTargets renderTargets,
      ShaderStorageBufferHolder holder,
      TextureAccess noiseTexture,
      FrameUpdateNotifier updateNotifier,
      CenterDepthSampler centerDepthSampler,
      BufferFlipper bufferFlipper,
      Supplier<ShadowRenderTargets> shadowTargetsSupplier,
      TextureStage textureStage,
      Object2ObjectMap<String, TextureAccess> customTextureIds,
      Object2ObjectMap<String, TextureAccess> irisCustomTextures,
      Set<GlImage> customImages,
      ImmutableMap<Integer, Boolean> explicitPreFlips,
      CustomUniforms customUniforms
   ) {
      this.pipeline = pipeline;
      this.compositePass = compositePass;
      this.noiseTexture = noiseTexture;
      this.centerDepthSampler = centerDepthSampler;
      this.renderTargets = renderTargets;
      this.customTextureIds = customTextureIds;
      this.customUniforms = customUniforms;
      this.irisCustomTextures = irisCustomTextures;
      this.customImages = customImages;
      this.textureStage = textureStage;
      PackRenderTargetDirectives renderTargetDirectives = packDirectives.getRenderTargetDirectives();
      Map<Integer, PackRenderTargetDirectives.RenderTargetSettings> renderTargetSettings = renderTargetDirectives.getRenderTargetSettings();
      Builder<CompositeRenderer.Pass> passes = ImmutableList.builder();
      com.google.common.collect.ImmutableSet.Builder<Integer> flippedAtLeastOnce = new com.google.common.collect.ImmutableSet.Builder();
      explicitPreFlips.forEach((bufferx, shouldFlip) -> {
         if (shouldFlip) {
            bufferFlipper.flip(bufferx);
         }
      });

      for (int i = 0; i < sources.length; i++) {
         ProgramSource source = sources[i];
         ImmutableSet<Integer> flipped = bufferFlipper.snapshot();
         ImmutableSet<Integer> flippedAtLeastOnceSnapshot = flippedAtLeastOnce.build();
         if (source != null && source.isValid()) {
            CompositeRenderer.Pass pass = new CompositeRenderer.Pass();
            ProgramDirectives directives = source.getDirectives();
            pass.name = source.getName();
            pass.program = this.createProgram(source, flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier);
            pass.blendModeOverride = source.getDirectives().getBlendModeOverride().orElse(null);
            if (computes.length != 0) {
               pass.computes = this.createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier, holder);
            } else {
               pass.computes = new ComputeProgram[0];
            }

            int[] drawBuffers = directives.getDrawBuffers();
            int passWidth = 0;
            int passHeight = 0;
            ImmutableMap<Integer, Boolean> explicitFlips = directives.getExplicitFlips();
            GlFramebuffer framebuffer = renderTargets.createColorFramebuffer(flipped, drawBuffers);

            for (int buffer : drawBuffers) {
               RenderTarget target = renderTargets.get(buffer);
               if (passWidth > 0 && passWidth != target.getWidth() || passHeight > 0 && passHeight != target.getHeight()) {
                  throw new IllegalStateException(
                     "Pass sizes must match for drawbuffers "
                        + Arrays.toString(drawBuffers)
                        + "\nOriginal width: "
                        + passWidth
                        + " New width: "
                        + target.getWidth()
                        + " Original height: "
                        + passHeight
                        + " New height: "
                        + target.getHeight()
                  );
               }

               passWidth = target.getWidth();
               passHeight = target.getHeight();
               if (explicitFlips.get(buffer) != Boolean.FALSE) {
                  bufferFlipper.flip(buffer);
                  flippedAtLeastOnce.add(buffer);
               }
            }

            explicitFlips.forEach((bufferx, shouldFlip) -> {
               if (shouldFlip) {
                  bufferFlipper.flip(bufferx);
                  flippedAtLeastOnce.add(bufferx);
               }
            });
            pass.drawBuffers = directives.getDrawBuffers();
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
            pass.stageReadsFromAlt = flipped;
            pass.framebuffer = framebuffer;
            pass.viewportScale = directives.getViewportScale();
            pass.mipmappedBuffers = directives.getMipmappedBuffers();
            pass.flippedAtLeastOnce = flippedAtLeastOnceSnapshot;
            passes.add(pass);
         } else if (computes.length != 0 && computes[i] != null && computes[i].length > 0) {
            CompositeRenderer.ComputeOnlyPass pass = new CompositeRenderer.ComputeOnlyPass();
            pass.name = computes[i].length > 0
               ? Arrays.stream(computes[i]).filter(Objects::nonNull).findFirst().map(ComputeSource::getName).orElse("unknown")
               : "unknown";
            pass.computes = this.createComputes(computes[i], flipped, flippedAtLeastOnceSnapshot, shadowTargetsSupplier, holder);
            passes.add(pass);
         }
      }

      this.passes = passes.build();
      this.flippedAtLeastOnceFinal = flippedAtLeastOnce.build();
      GlStateManager._glBindFramebuffer(36008, 0);
   }

   private boolean hasComputes(ComputeSource[][] computes) {
      boolean hasCompute = false;

      for (int i = 0; i < computes.length; i++) {
         if (computes[i].length > 0) {
            for (int j = 0; j < computes[i].length; j++) {
               if (computes[i][j] != null) {
                  hasCompute = true;
                  break;
               }
            }
         }
      }

      return hasCompute;
   }

   private static void setupMipmapping(RenderTarget target, boolean readFromAlt) {
      if (target != null) {
         int texture = readFromAlt ? target.getAltTexture() : target.getMainTexture();
         IrisRenderSystem.generateMipmaps(texture, 3553);
         int filter = 9987;
         if (target.getInternalFormat().getPixelFormat().isInteger()) {
            filter = 9984;
         }

         IrisRenderSystem.texParameteri(texture, 3553, 10241, filter);
      }
   }

   public ImmutableSet<Integer> getFlippedAtLeastOnceFinal() {
      return this.flippedAtLeastOnceFinal;
   }

   public void recalculateSizes() {
      UnmodifiableIterator var1 = this.passes.iterator();

      while (var1.hasNext()) {
         CompositeRenderer.Pass pass = (CompositeRenderer.Pass)var1.next();
         if (!(pass instanceof CompositeRenderer.ComputeOnlyPass)) {
            int passWidth = 0;
            int passHeight = 0;

            for (int buffer : pass.drawBuffers) {
               RenderTarget target = this.renderTargets.get(buffer);
               if (passWidth > 0 && passWidth != target.getWidth() || passHeight > 0 && passHeight != target.getHeight()) {
                  throw new IllegalStateException("Pass widths must match");
               }

               passWidth = target.getWidth();
               passHeight = target.getHeight();
            }

            this.renderTargets.destroyFramebuffer(pass.framebuffer);
            pass.framebuffer = this.renderTargets.createColorFramebuffer(pass.stageReadsFromAlt, pass.drawBuffers);
            pass.viewWidth = passWidth;
            pass.viewHeight = passHeight;
         }
      }
   }

   public void renderAll() {
      GLDebug.pushGroup(20 + this.compositePass.ordinal(), this.compositePass.name().toLowerCase(Locale.ROOT));
      RenderSystem.disableBlend();
      FullScreenQuadRenderer.INSTANCE.begin();
      Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
      int i = 0;

      for (int passesSize = this.passes.size(); i < passesSize; i++) {
         CompositeRenderer.Pass renderPass = (CompositeRenderer.Pass)this.passes.get(i);
         GLDebug.pushGroup(20 * this.compositePass.ordinal() + i, renderPass.name);
         boolean ranCompute = false;

         for (ComputeProgram computeProgram : renderPass.computes) {
            if (computeProgram != null) {
               ranCompute = true;
               computeProgram.use();
               this.customUniforms.push(computeProgram);
               computeProgram.dispatch(main.textureWidth, main.textureHeight);
            }
         }

         if (ranCompute) {
            IrisRenderSystem.memoryBarrier(8232);
         }

         Program.unbind();
         if (renderPass instanceof CompositeRenderer.ComputeOnlyPass) {
            GLDebug.popGroup();
         } else {
            if (!renderPass.mipmappedBuffers.isEmpty()) {
               RenderSystem.activeTexture(33984);
               UnmodifiableIterator var11 = renderPass.mipmappedBuffers.iterator();

               while (var11.hasNext()) {
                  int index = (Integer)var11.next();
                  setupMipmapping(this.renderTargets.get(index), renderPass.stageReadsFromAlt.contains(index));
               }
            }

            float scaledWidth = renderPass.viewWidth * renderPass.viewportScale.scale();
            float scaledHeight = renderPass.viewHeight * renderPass.viewportScale.scale();
            int beginWidth = (int)(renderPass.viewWidth * renderPass.viewportScale.viewportX());
            int beginHeight = (int)(renderPass.viewHeight * renderPass.viewportScale.viewportY());
            RenderSystem.viewport(beginWidth, beginHeight, (int)scaledWidth, (int)scaledHeight);
            renderPass.framebuffer.bind();
            renderPass.program.use();
            if (renderPass.blendModeOverride != null) {
               renderPass.blendModeOverride.apply();
            } else {
               RenderSystem.disableBlend();
            }

            this.customUniforms.push(renderPass.program);
            FullScreenQuadRenderer.INSTANCE.renderQuad();
            BlendModeOverride.restore();
            GLDebug.popGroup();
         }
      }

      FullScreenQuadRenderer.INSTANCE.end();
      MinecraftClient.getInstance().getFramebuffer().beginWrite(true);
      ProgramUniforms.clearActiveUniforms();
      ProgramSamplers.clearActiveSamplers();
      GlStateManager._glUseProgram(0);

      for (int ix = 0; ix < SamplerLimits.get().getMaxTextureUnits(); ix++) {
         if (GlStateManagerAccessor.getTEXTURES()[ix].boundTexture != 0) {
            RenderSystem.activeTexture(33984 + ix);
            RenderSystem.bindTexture(0);
         }
      }

      RenderSystem.activeTexture(33984);
      GLDebug.popGroup();
   }

   private Program createProgram(
      ProgramSource source,
      ImmutableSet<Integer> flipped,
      ImmutableSet<Integer> flippedAtLeastOnceSnapshot,
      Supplier<ShadowRenderTargets> shadowTargetsSupplier
   ) {
      Map<PatchShaderType, String> transformed = TransformPatcher.patchComposite(
         source.getName(),
         source.getVertexSource().orElseThrow(NullPointerException::new),
         source.getGeometrySource().orElse(null),
         source.getFragmentSource().orElseThrow(NullPointerException::new),
         this.textureStage,
         this.pipeline.getTextureMap()
      );
      String vertex = transformed.get(PatchShaderType.VERTEX);
      String geometry = transformed.get(PatchShaderType.GEOMETRY);
      String fragment = transformed.get(PatchShaderType.FRAGMENT);
      ShaderPrinter.printProgram(source.getName()).addSources(transformed).print();
      Objects.requireNonNull(flipped);

      ProgramBuilder builder;
      try {
         builder = ProgramBuilder.begin(source.getName(), vertex, geometry, fragment, IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
      } catch (ShaderCompileException e) {
         throw e;
      } catch (RuntimeException e) {
         throw new RuntimeException("Shader compilation failed for " + source.getName() + "!", e);
      }

      CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
      this.customUniforms.assignTo(builder);
      ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
         builder, this.customTextureIds, flippedAtLeastOnceSnapshot
      );
      IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, this.renderTargets, true, this.pipeline);
      IrisSamplers.addCustomTextures(builder, this.irisCustomTextures);
      IrisSamplers.addCustomImages(customTextureSamplerInterceptor, this.customImages);
      IrisImages.addRenderTargetImages(builder, () -> flipped, this.renderTargets);
      IrisImages.addCustomImages(builder, this.customImages);
      IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, this.noiseTexture);
      IrisSamplers.addCompositeSamplers(customTextureSamplerInterceptor, this.renderTargets);
      if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
         IrisSamplers.addShadowSamplers(
            customTextureSamplerInterceptor, shadowTargetsSupplier.get(), null, this.pipeline.hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS)
         );
         IrisImages.addShadowColorImages(builder, shadowTargetsSupplier.get(), null);
      }

      this.centerDepthSampler.setUsage(builder.addDynamicSampler(this.centerDepthSampler::getCenterDepthTexture, "iris_centerDepthSmooth"));
      Program build = builder.build();
      this.customUniforms.mapholderToPass(builder, build);
      return build;
   }

   private ComputeProgram[] createComputes(
      ComputeSource[] compute,
      ImmutableSet<Integer> flipped,
      ImmutableSet<Integer> flippedAtLeastOnceSnapshot,
      Supplier<ShadowRenderTargets> shadowTargetsSupplier,
      ShaderStorageBufferHolder holder
   ) {
      ComputeProgram[] programs = new ComputeProgram[compute.length];

      for (int i = 0; i < programs.length; i++) {
         ComputeSource source = compute[i];
         if (source != null && !source.getSource().isEmpty()) {
            Objects.requireNonNull(flipped);

            ProgramBuilder builder;
            try {
               String transformed = TransformPatcher.patchCompute(
                  source.getName(), source.getSource().orElse(null), this.textureStage, this.pipeline.getTextureMap()
               );
               ShaderPrinter.printProgram(source.getName()).addSource(PatchShaderType.COMPUTE, transformed).print();
               builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
            } catch (ShaderCompileException e) {
               throw e;
            } catch (RuntimeException e) {
               throw new RuntimeException("Shader compilation failed for compute " + source.getName() + "!", e);
            }

            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(
               builder, this.customTextureIds, flippedAtLeastOnceSnapshot
            );
            CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
            this.customUniforms.assignTo(builder);
            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, () -> flipped, this.renderTargets, true, this.pipeline);
            IrisSamplers.addCustomTextures(builder, this.irisCustomTextures);
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, this.customImages);
            IrisImages.addRenderTargetImages(builder, () -> flipped, this.renderTargets);
            IrisImages.addCustomImages(builder, this.customImages);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, this.noiseTexture);
            IrisSamplers.addCompositeSamplers(customTextureSamplerInterceptor, this.renderTargets);
            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
               IrisSamplers.addShadowSamplers(
                  customTextureSamplerInterceptor, shadowTargetsSupplier.get(), null, this.pipeline.hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS)
               );
               IrisImages.addShadowColorImages(builder, shadowTargetsSupplier.get(), null);
            }

            this.centerDepthSampler.setUsage(builder.addDynamicSampler(this.centerDepthSampler::getCenterDepthTexture, "iris_centerDepthSmooth"));
            programs[i] = builder.buildCompute();
            this.customUniforms.mapholderToPass(builder, programs[i]);
            programs[i]
               .setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), FilledIndirectPointer.basedOff(holder, source.getIndirectPointer()));
         }
      }

      return programs;
   }

   public void destroy() {
      UnmodifiableIterator var1 = this.passes.iterator();

      while (var1.hasNext()) {
         CompositeRenderer.Pass renderPass = (CompositeRenderer.Pass)var1.next();
         renderPass.destroy();
      }
   }

   private static class ComputeOnlyPass extends CompositeRenderer.Pass {
      @Override
      protected void destroy() {
         for (ComputeProgram compute : this.computes) {
            if (compute != null) {
               compute.destroy();
            }
         }
      }
   }

   private static class Pass {
      int[] drawBuffers;
      int viewWidth;
      int viewHeight;
      String name;
      Program program;
      BlendModeOverride blendModeOverride;
      ComputeProgram[] computes;
      GlFramebuffer framebuffer;
      ImmutableSet<Integer> flippedAtLeastOnce;
      ImmutableSet<Integer> stageReadsFromAlt;
      ImmutableSet<Integer> mipmappedBuffers;
      ViewportData viewportScale;

      protected void destroy() {
         this.program.destroy();

         for (ComputeProgram compute : this.computes) {
            if (compute != null) {
               compute.destroy();
            }
         }
      }
   }
}
