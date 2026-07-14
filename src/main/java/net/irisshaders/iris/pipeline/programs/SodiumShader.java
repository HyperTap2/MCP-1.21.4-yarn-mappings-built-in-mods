package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.client.gl.device.GLRenderDevice;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat2v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.builtin.BuiltinReplacementUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class SodiumShader implements ChunkShaderInterface {
   private static final int SUB_TEXEL_PRECISION_BITS = 5;
   private final GlUniformMatrix4f uniformModelViewMatrix;
   private final GlUniformMatrix4f uniformModelViewMatrixInv;
   private final GlUniformMatrix4f uniformProjectionMatrix;
   private final GlUniformMatrix4f uniformProjectionMatrixInv;
   private final GlUniformMatrix3f uniformNormalMatrix;
   private final GlUniformFloat3v uniformRegionOffset;
   private final GlUniformFloat2v uniformTexCoordShrink;
   private final ProgramImages images;
   private final ProgramSamplers samplers;
   private final ProgramUniforms uniforms;
   private final CustomUniforms customUniforms;
   private final BlendModeOverride blendModeOverride;
   private final List<BufferBlendOverride> bufferBlendOverrides;
   private final float alphaTest;
   private final boolean containsTessellation;

   public SodiumShader(
      IrisRenderingPipeline pipeline,
      SodiumPrograms.Pass pass,
      ShaderBindingContext context,
      int handle,
      BlendModeOverride blendModeOverride,
      List<BufferBlendOverride> bufferBlendOverrides,
      CustomUniforms customUniforms,
      Supplier<ImmutableSet<Integer>> flipState,
      float alphaTest,
      boolean containsTessellation
   ) {
      this.uniformModelViewMatrix = (GlUniformMatrix4f)context.bindUniformOptional("iris_ModelViewMatrix", GlUniformMatrix4f::new);
      this.uniformModelViewMatrixInv = (GlUniformMatrix4f)context.bindUniformOptional("iris_ModelViewMatrixInverse", GlUniformMatrix4f::new);
      this.uniformNormalMatrix = (GlUniformMatrix3f)context.bindUniformOptional("iris_NormalMatrix", GlUniformMatrix3f::new);
      this.uniformProjectionMatrix = (GlUniformMatrix4f)context.bindUniformOptional("iris_ProjectionMatrix", GlUniformMatrix4f::new);
      this.uniformProjectionMatrixInv = (GlUniformMatrix4f)context.bindUniformOptional("iris_ProjectionMatrixInv", GlUniformMatrix4f::new);
      this.uniformRegionOffset = (GlUniformFloat3v)context.bindUniformOptional("u_RegionOffset", GlUniformFloat3v::new);
      this.uniformTexCoordShrink = (GlUniformFloat2v)context.bindUniformOptional("u_TexCoordShrink", GlUniformFloat2v::new);
      this.alphaTest = alphaTest;
      this.containsTessellation = containsTessellation;
      boolean isShadowPass = pass == SodiumPrograms.Pass.SHADOW || pass == SodiumPrograms.Pass.SHADOW_CUTOUT;
      this.uniforms = this.buildUniforms(pass, handle, customUniforms);
      this.customUniforms = customUniforms;
      this.samplers = this.buildSamplers(pipeline, pass, handle, isShadowPass, flipState);
      this.images = this.buildImages(pipeline, pass, handle, isShadowPass, flipState);
      this.blendModeOverride = blendModeOverride;
      this.bufferBlendOverrides = bufferBlendOverrides;
   }

   private ProgramUniforms buildUniforms(SodiumPrograms.Pass pass, int handle, CustomUniforms customUniforms) {
      ProgramUniforms.Builder builder = ProgramUniforms.builder(pass.name().toLowerCase(Locale.ROOT), handle);
      CommonUniforms.addDynamicUniforms(builder, FogMode.PER_VERTEX);
      customUniforms.assignTo(builder);
      BuiltinReplacementUniforms.addBuiltinReplacementUniforms(builder);
      customUniforms.mapholderToPass(builder, this);
      return builder.buildUniforms();
   }

   private ProgramSamplers buildSamplers(
      IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, int handle, boolean isShadowPass, Supplier<ImmutableSet<Integer>> flipState
   ) {
      ProgramSamplers.Builder builder = ProgramSamplers.builder(handle, IrisSamplers.SODIUM_RESERVED_TEXTURE_UNITS);
      pipeline.addGbufferOrShadowSamplers(builder, ProgramImages.builder(handle), flipState, isShadowPass, true, true, false);
      return builder.build();
   }

   private ProgramImages buildImages(
      IrisRenderingPipeline pipeline, SodiumPrograms.Pass pass, int handle, boolean isShadowPass, Supplier<ImmutableSet<Integer>> flipState
   ) {
      ProgramImages.Builder builder = ProgramImages.builder(handle);
      pipeline.addGbufferOrShadowSamplers(
         ProgramSamplers.builder(handle, IrisSamplers.SODIUM_RESERVED_TEXTURE_UNITS), builder, flipState, isShadowPass, true, true, false
      );
      return builder.build();
   }

   public void setRegionOffset(float x, float y, float z) {
      if (this.uniformRegionOffset != null) {
         this.uniformRegionOffset.set(x, y, z);
      }
   }

   public void setModelViewMatrix(Matrix4fc matrix) {
      if (this.uniformModelViewMatrix != null) {
         this.uniformModelViewMatrix.set(matrix);
      }

      Matrix4f invertedMatrix = matrix.invert(new Matrix4f());
      if (this.uniformModelViewMatrixInv != null) {
         this.uniformModelViewMatrixInv.set(invertedMatrix);
      }

      if (this.uniformNormalMatrix != null) {
         Matrix3f normalMatrix = invertedMatrix.transpose3x3(new Matrix3f());
         this.uniformNormalMatrix.set(normalMatrix);
      }
   }

   public void setProjectionMatrix(Matrix4fc matrix) {
      if (this.uniformProjectionMatrix != null) {
         this.uniformProjectionMatrix.set(matrix);
      }

      if (this.uniformProjectionMatrixInv != null) {
         Matrix4f invertedMatrix = matrix.invert(new Matrix4f());
         this.uniformProjectionMatrixInv.set(invertedMatrix);
      }
   }

   public void setupState() {
      this.applyBlendModes();
      this.updateUniforms();
      this.images.update();
      this.bindTextures();
      SpriteAtlasTexture textureAtlas = (SpriteAtlasTexture)MinecraftClient.getInstance()
         .getTextureManager()
         .getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
      double subTexelPrecision = 1 << GLRenderDevice.INSTANCE.getSubTexelPrecisionBits();
      double subTexelOffset = 3.0517578E-5F;
      if (this.uniformTexCoordShrink != null) {
         this.uniformTexCoordShrink
            .set(
               (float)(subTexelOffset - 1.0 / textureAtlas.getWidth() / subTexelPrecision),
               (float)(subTexelOffset - 1.0 / textureAtlas.getHeight() / subTexelPrecision)
            );
      }

      if (this.containsTessellation) {
         ImmediateState.usingTessellation = true;
      }
   }

   private void bindTextures() {
      IrisRenderSystem.bindTextureToUnit(3553, 0, RenderSystem.getShaderTexture(0));
      IrisRenderSystem.bindTextureToUnit(3553, 2, RenderSystem.getShaderTexture(2));
      GlStateManager._activeTexture(33986);
   }

   private void applyBlendModes() {
      if (this.blendModeOverride != null) {
         this.blendModeOverride.apply();
      }

      this.bufferBlendOverrides.forEach(BufferBlendOverride::apply);
   }

   private void updateUniforms() {
      CapturedRenderingState.INSTANCE.setCurrentAlphaTest(this.alphaTest);
      this.samplers.update();
      this.uniforms.update();
      this.customUniforms.push(this);
   }

   public void resetState() {
      ProgramUniforms.clearActiveUniforms();
      ProgramSamplers.clearActiveSamplers();
      BlendModeOverride.restore();
      MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
      ImmediateState.usingTessellation = false;
   }
}
