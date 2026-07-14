package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramDefinition;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceFactory;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class FallbackShader extends ShaderProgram {
   private final IrisRenderingPipeline parent;
   private final BlendModeOverride blendModeOverride;
   private final GlFramebuffer writingToBeforeTranslucent;
   private final GlFramebuffer writingToAfterTranslucent;
   @Nullable
   private final GlUniform FOG_DENSITY;
   @Nullable
   private final GlUniform FOG_IS_EXP2;
   private final int gtexture;
   private final int overlay;
   private final int lightmap;

   public FallbackShader(
      int programId,
      ShaderProgramDefinition shaderProgramConfig,
      ResourceFactory resourceFactory,
      String string,
      VertexFormat vertexFormat,
      GlFramebuffer writingToBeforeTranslucent,
      GlFramebuffer writingToAfterTranslucent,
      BlendModeOverride blendModeOverride,
      float alphaValue,
      IrisRenderingPipeline parent
   ) throws IOException {
      super(programId);
      ((ShaderInstanceInterface)this).setShouldSkip(SkipList.NONE);
      this.set(shaderProgramConfig.uniforms(), shaderProgramConfig.samplers());
      this.parent = parent;
      this.blendModeOverride = blendModeOverride;
      this.writingToBeforeTranslucent = writingToBeforeTranslucent;
      this.writingToAfterTranslucent = writingToAfterTranslucent;
      this.FOG_DENSITY = this.getUniform("FogDensity");
      this.FOG_IS_EXP2 = this.getUniform("FogIsExp2");
      this.gtexture = GlStateManager._glGetUniformLocation(programId, "gtexture");
      this.overlay = GlStateManager._glGetUniformLocation(programId, "overlay");
      this.lightmap = GlStateManager._glGetUniformLocation(programId, "lightmap");
      GlUniform ALPHA_TEST_VALUE = this.getUniform("AlphaTestValue");
      if (ALPHA_TEST_VALUE != null) {
         ALPHA_TEST_VALUE.set(alphaValue);
      }
   }

   public void unbind() {
      super.unbind();
      if (this.blendModeOverride != null) {
         BlendModeOverride.restore();
      }

      MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
   }

   public void initializeUniforms(DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window) {
      super.initializeUniforms(drawMode, viewMatrix, projectionMatrix, window);
   }

   public void bind() {
      if (this.FOG_DENSITY != null && this.FOG_IS_EXP2 != null) {
         float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();
         if (fogDensity >= 0.0) {
            this.FOG_DENSITY.set(fogDensity);
            this.FOG_IS_EXP2.set(1);
         } else {
            this.FOG_DENSITY.set(0.0F);
            this.FOG_IS_EXP2.set(0);
         }
      }

      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, RenderSystem.getShaderTexture(0));
      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 1, RenderSystem.getShaderTexture(1));
      IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 2, RenderSystem.getShaderTexture(2));
      GlStateManager._glUseProgram(this.getGlRef());

      for (GlUniform uniform : super.uniforms) {
         this.uploadIfNotNull(uniform);
      }

      GlStateManager._glUniform1i(this.gtexture, 0);
      GlStateManager._glUniform1i(this.overlay, 1);
      GlStateManager._glUniform1i(this.lightmap, 2);
      if (this.blendModeOverride != null) {
         this.blendModeOverride.apply();
      }

      if (this.parent.isBeforeTranslucent) {
         this.writingToBeforeTranslucent.bind();
      } else {
         this.writingToAfterTranslucent.bind();
      }
   }

   private void uploadIfNotNull(GlUniform uniform) {
      if (uniform != null) {
         uniform.upload();
      }
   }
}
