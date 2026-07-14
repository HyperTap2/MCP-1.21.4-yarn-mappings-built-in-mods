package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gl.uniform.DynamicLocationalUniformHolder;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramDefinition.Sampler;
import net.minecraft.client.gl.ShaderProgramDefinition.Uniform;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ExtendedShader extends ShaderProgram {
   private static final Matrix4f identity = new Matrix4f();
   private static ExtendedShader lastApplied;
   private final boolean intensitySwizzle;
   private final List<BufferBlendOverride> bufferBlendOverrides;
   private final boolean hasOverrides;
   private final GlUniform modelViewInverse;
   private final GlUniform projectionInverse;
   private final GlUniform normalMatrix;
   private final CustomUniforms customUniforms;
   private final IrisRenderingPipeline parent;
   private final ProgramUniforms uniforms;
   private final ProgramSamplers samplers;
   private final ProgramImages images;
   private final GlFramebuffer writingToBeforeTranslucent;
   private final GlFramebuffer writingToAfterTranslucent;
   private final BlendModeOverride blendModeOverride;
   private final float alphaTest;
   private final boolean usesTessellation;
   private final Matrix4f tempMatrix4f = new Matrix4f();
   private final Matrix3f tempMatrix3f = new Matrix3f();
   private final float[] tempFloats = new float[16];
   private final float[] tempFloats2 = new float[9];
   private int textureToUnswizzle;

   public ExtendedShader(
      int programId,
      ResourceFactory resourceFactory,
      String string,
      VertexFormat vertexFormat,
      boolean usesTessellation,
      GlFramebuffer writingToBeforeTranslucent,
      GlFramebuffer writingToAfterTranslucent,
      BlendModeOverride blendModeOverride,
      AlphaTest alphaTest,
      Consumer<DynamicLocationalUniformHolder> uniformCreator,
      BiConsumer<SamplerHolder, ImageHolder> samplerCreator,
      boolean isIntensity,
      IrisRenderingPipeline parent,
      @Nullable List<BufferBlendOverride> bufferBlendOverrides,
      CustomUniforms customUniforms
   ) throws IOException {
      super(programId);
      ((ShaderInstanceInterface)this).setShouldSkip(SkipList.NONE);
      List<Uniform> uniformList = new ArrayList<>();
      List<Sampler> samplerList = new ArrayList<>();
      uniformList.add(
         new Uniform(
            "iris_ModelViewMat", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
      uniformList.add(new Uniform("iris_NormalMat", "matrix3x3", 9, List.of(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F)));
      uniformList.add(
         new Uniform("iris_ProjMat", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F))
      );
      uniformList.add(
         new Uniform(
            "iris_TextureMat", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
      uniformList.add(new Uniform("iris_ColorModulator", "float", 4, List.of(1.0F, 1.0F, 1.0F, 1.0F)));
      uniformList.add(new Uniform("iris_FogColor", "float", 4, List.of(1.0F, 1.0F, 1.0F, 1.0F)));
      uniformList.add(new Uniform("iris_ModelOffset", "float", 3, List.of(0.0F, 0.0F, 0.0F)));
      uniformList.add(new Uniform("iris_FogStart", "float", 1, List.of(0.0F)));
      uniformList.add(new Uniform("iris_FogEnd", "float", 1, List.of(1.0F)));
      uniformList.add(new Uniform("iris_GlintAlpha", "float", 1, List.of(0.0F)));
      uniformList.add(
         new Uniform(
            "iris_ModelViewMatInverse",
            "matrix4x4",
            16,
            List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
      uniformList.add(
         new Uniform(
            "iris_ProjMatInverse", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)
         )
      );
      samplerList.add(new Sampler("Sampler0"));
      this.set(uniformList, samplerList);
      GLDebug.nameObject(33506, programId, string);
      ProgramUniforms.Builder uniformBuilder = ProgramUniforms.builder(string, programId);
      ProgramSamplers.Builder samplerBuilder = ProgramSamplers.builder(programId, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);
      uniformCreator.accept(uniformBuilder);
      ProgramImages.Builder builder = ProgramImages.builder(programId);
      samplerCreator.accept(samplerBuilder, builder);
      customUniforms.mapholderToPass(uniformBuilder, this);
      this.usesTessellation = usesTessellation;
      this.uniforms = uniformBuilder.buildUniforms();
      this.customUniforms = customUniforms;
      this.samplers = samplerBuilder.build();
      this.images = builder.build();
      this.writingToBeforeTranslucent = writingToBeforeTranslucent;
      this.writingToAfterTranslucent = writingToAfterTranslucent;
      this.blendModeOverride = blendModeOverride;
      this.bufferBlendOverrides = bufferBlendOverrides;
      this.hasOverrides = bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty();
      this.alphaTest = alphaTest.reference();
      this.parent = parent;
      this.modelViewInverse = this.getUniform("ModelViewMatInverse");
      this.projectionInverse = this.getUniform("ProjMatInverse");
      this.normalMatrix = this.getUniform("NormalMat");
      this.intensitySwizzle = isIntensity;
   }

   public boolean isIntensitySwizzle() {
      return this.intensitySwizzle;
   }

   public void unbind() {
      ProgramUniforms.clearActiveUniforms();
      ProgramSamplers.clearActiveSamplers();
      if (this.blendModeOverride != null || this.hasOverrides) {
         BlendModeOverride.restore();
      }

      MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
      super.unbind();
   }

   public void initializeUniforms(DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window) {
      super.initializeUniforms(drawMode, viewMatrix, projectionMatrix, window);
      if (this.modelViewInverse != null) {
         this.modelViewInverse.set(viewMatrix.invert(this.tempMatrix4f));
      }

      if (this.normalMatrix != null) {
         this.normalMatrix.set(viewMatrix.invert(this.tempMatrix4f).transpose3x3(this.tempMatrix3f));
      }

      if (this.projectionInverse != null) {
         this.projectionInverse.set(projectionMatrix.invert(this.tempMatrix4f));
      }
   }

   public void bind() {
      CapturedRenderingState.INSTANCE.setCurrentAlphaTest(this.alphaTest);
      GlStateManager._glUseProgram(this.getGlRef());
      int i = GlStateManager._getActiveTexture();
      GlStateManager._activeTexture(i);
      if (this.intensitySwizzle) {
         IrisRenderSystem.addUnswizzle(RenderSystem.getShaderTexture(0));
         IrisRenderSystem.texParameteriv(RenderSystem.getShaderTexture(0), TextureType.TEXTURE_2D.getGlType(), 36422, new int[]{6403, 6403, 6403, 6403});
      }

      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, RenderSystem.getShaderTexture(0));
      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 1, RenderSystem.getShaderTexture(1));
      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 2, RenderSystem.getShaderTexture(2));
      ImmediateState.usingTessellation = this.usesTessellation;
      this.uploadIfNotNull(this.projectionInverse);
      this.uploadIfNotNull(this.modelViewInverse);
      this.uploadIfNotNull(this.normalMatrix);
      this.samplers.update();
      this.uniforms.update();

      for (GlUniform uniform : super.uniforms) {
         this.uploadIfNotNull(uniform);
      }

      this.customUniforms.push(this);
      this.images.update();
      if (this.blendModeOverride != null) {
         this.blendModeOverride.apply();
      }

      if (this.hasOverrides) {
         this.bufferBlendOverrides.forEach(BufferBlendOverride::apply);
      }

      if (this.parent.isBeforeTranslucent) {
         this.writingToBeforeTranslucent.bind();
      } else {
         this.writingToAfterTranslucent.bind();
      }
   }

   @Nullable
   public GlUniform getUniform(@NotNull String name) {
      GlUniform uniform = super.getUniform("iris_" + name);
      return uniform != null || !name.equalsIgnoreCase("OverlayUV") && !name.equalsIgnoreCase("LightUV") ? uniform : null;
   }

   public void set(List<Uniform> uniforms, List<Sampler> samplers) {
      RenderSystem.assertOnRenderThread();

      for (Uniform uniform : uniforms) {
         String string = uniform.name();
         int i = GlUniform.getUniformLocation(this.getGlRef(), string);
         if (i != -1) {
            GlUniform uniform2 = this.createGlUniform(uniform);
            uniform2.setLocation(i);
            super.uniforms.add(uniform2);
            super.uniformsByName.put(string, uniform2);
         }
      }

      for (Sampler sampler : samplers) {
         int j = GlUniform.getUniformLocation(this.getGlRef(), sampler.name());
         if (j != -1) {
            super.samplers.add(sampler);
            super.samplerLocations.add(j);
         }
      }

      this.modelViewMat = super.getUniform("iris_ModelViewMat");
      this.projectionMat = super.getUniform("iris_ProjMat");
      this.textureMat = super.getUniform("iris_TextureMat");
      this.screenSize = super.getUniform("iris_ScreenSize");
      this.colorModulator = super.getUniform("iris_ColorModulator");
      this.light0Direction = super.getUniform("iris_Light0_Direction");
      this.light1Direction = super.getUniform("iris_Light1_Direction");
      this.glintAlpha = super.getUniform("iris_GlintAlpha");
      this.fogStart = super.getUniform("iris_FogStart");
      this.fogEnd = super.getUniform("iris_FogEnd");
      this.fogColor = super.getUniform("iris_FogColor");
      this.fogShape = super.getUniform("iris_FogShape");
      this.lineWidth = super.getUniform("iris_LineWidth");
      this.gameTime = super.getUniform("iris_GameTime");
      this.modelOffset = super.getUniform("iris_ModelOffset");
   }

   private void uploadIfNotNull(GlUniform uniform) {
      if (uniform != null) {
         uniform.upload();
      }
   }

   public boolean hasActiveImages() {
      return this.images.getActiveImages() > 0;
   }

   static {
      identity.identity();
   }
}
