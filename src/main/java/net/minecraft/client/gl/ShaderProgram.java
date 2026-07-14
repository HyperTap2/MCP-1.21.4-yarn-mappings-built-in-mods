package net.minecraft.client.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Matrix4f;

public class ShaderProgram implements AutoCloseable, ShaderInstanceInterface {
   private static final Uniform DEFAULT_UNIFORM = new Uniform();
   private static final int field_53837 = -1;
   private static ShaderProgram iris$lastAppliedShader;
   public final List<ShaderProgramDefinition.Sampler> samplers = new ArrayList<>();
   private final Object2IntMap<String> samplerTextures = new Object2IntArrayMap();
   public final IntList samplerLocations = new IntArrayList();
   public final List<GlUniform> uniforms = new ArrayList<>();
   public final Map<String, GlUniform> uniformsByName = new HashMap<>();
   private final Map<String, ShaderProgramDefinition.Uniform> uniformDefinitionsByName = new HashMap<>();
   private final int glRef;
   @Nullable
   public GlUniform modelViewMat;
   @Nullable
   public GlUniform projectionMat;
   @Nullable
   public GlUniform textureMat;
   @Nullable
   public GlUniform screenSize;
   @Nullable
   public GlUniform colorModulator;
   @Nullable
   public GlUniform light0Direction;
   @Nullable
   public GlUniform light1Direction;
   @Nullable
   public GlUniform glintAlpha;
   @Nullable
   public GlUniform fogStart;
   @Nullable
   public GlUniform fogEnd;
   @Nullable
   public GlUniform fogColor;
   @Nullable
   public GlUniform fogShape;
   @Nullable
   public GlUniform lineWidth;
   @Nullable
   public GlUniform gameTime;
   @Nullable
   public GlUniform modelOffset;
   private MethodHandle iris$shouldSkip;

   public ShaderProgram(int glRef) {
      this.glRef = glRef;
      this.samplerTextures.defaultReturnValue(-1);
   }

   @Override
   public void setShouldSkip(MethodHandle shouldSkip) {
      this.iris$shouldSkip = shouldSkip;
   }

   public boolean iris$shouldSkipThis() {
      if (Iris.getIrisConfig().shouldAllowUnknownShaders()) {
         if (ShadowRenderer.ACTIVE) {
            return true;
         }

         if (!iris$shouldOverrideShaders()) {
            return false;
         }

         if (this.iris$shouldSkip == SkipList.NONE) {
            return false;
         }

         if (this.iris$shouldSkip == SkipList.ALWAYS) {
            return true;
         }

         try {
            return (boolean)this.iris$shouldSkip.invoke(this);
         } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
         }
      }

      return !this.iris$isKnownShader() && iris$shouldOverrideShaders();
   }

   private static boolean iris$shouldOverrideShaders() {
      WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
      return pipeline instanceof ShaderRenderingPipeline shaderPipeline && shaderPipeline.shouldOverrideShaders();
   }

   private boolean iris$isKnownShader() {
      return this instanceof ExtendedShader || this instanceof FallbackShader;
   }

   public static ShaderProgram create(CompiledShader vertexShader, CompiledShader fragmentShader, VertexFormat format) throws ShaderLoader.LoadException {
      int i = GlStateManager.glCreateProgram();
      if (i <= 0) {
         throw new ShaderLoader.LoadException("Could not create shader program (returned program ID " + i + ")");
      } else {
         format.bindAttributes(i);
         GlStateManager.glAttachShader(i, vertexShader.getHandle());
         GlStateManager.glAttachShader(i, fragmentShader.getHandle());
         GlStateManager.glLinkProgram(i);
         int j = GlStateManager.glGetProgrami(i, 35714);
         if (j == 0) {
            String string = GlStateManager.glGetProgramInfoLog(i, 32768);
            throw new ShaderLoader.LoadException(
               "Error encountered when linking program containing VS " + vertexShader.getId() + " and FS " + fragmentShader.getId() + ". Log output: " + string
            );
         } else {
            return new ShaderProgram(i);
         }
      }
   }

   public void set(List<ShaderProgramDefinition.Uniform> uniforms, List<ShaderProgramDefinition.Sampler> samplers) {
      RenderSystem.assertOnRenderThread();

      for (ShaderProgramDefinition.Uniform uniform : uniforms) {
         String string = uniform.name();
         int i = GlUniform.getUniformLocation(this.glRef, string);
         if (i != -1) {
            GlUniform glUniform = this.createGlUniform(uniform);
            glUniform.setLocation(i);
            this.uniforms.add(glUniform);
            this.uniformsByName.put(string, glUniform);
            this.uniformDefinitionsByName.put(string, uniform);
         }
      }

      for (ShaderProgramDefinition.Sampler sampler : samplers) {
         int j = GlUniform.getUniformLocation(this.glRef, sampler.name());
         if (j != -1) {
            this.samplers.add(sampler);
            this.samplerLocations.add(j);
         }
      }

      this.modelViewMat = this.getUniform("ModelViewMat");
      this.projectionMat = this.getUniform("ProjMat");
      this.textureMat = this.getUniform("TextureMat");
      this.screenSize = this.getUniform("ScreenSize");
      this.colorModulator = this.getUniform("ColorModulator");
      this.light0Direction = this.getUniform("Light0_Direction");
      this.light1Direction = this.getUniform("Light1_Direction");
      this.glintAlpha = this.getUniform("GlintAlpha");
      this.fogStart = this.getUniform("FogStart");
      this.fogEnd = this.getUniform("FogEnd");
      this.fogColor = this.getUniform("FogColor");
      this.fogShape = this.getUniform("FogShape");
      this.lineWidth = this.getUniform("LineWidth");
      this.gameTime = this.getUniform("GameTime");
      this.modelOffset = this.getUniform("ModelOffset");
   }

   @Override
   public void close() {
      this.uniforms.forEach(GlUniform::close);
      GlStateManager.glDeleteProgram(this.glRef);
   }

   public void unbind() {
      if (!this.iris$shouldSkipThis()) {
         if (!this.iris$isKnownShader() && iris$shouldOverrideShaders()) {
            WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
            if (pipeline instanceof IrisRenderingPipeline) {
               MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
            }
         }
      } else {
         DepthColorStorage.unlockDepthColor();
      }

      RenderSystem.assertOnRenderThread();
      GlStateManager._glUseProgram(0);
      int i = GlStateManager._getActiveTexture();

      for (int j = 0; j < this.samplerLocations.size(); j++) {
         ShaderProgramDefinition.Sampler sampler = this.samplers.get(j);
         if (!this.samplerTextures.containsKey(sampler.name())) {
            GlStateManager._activeTexture(33984 + j);
            GlStateManager._bindTexture(0);
         }
      }

      GlStateManager._activeTexture(i);
   }

   public void bind() {
      if (iris$lastAppliedShader != null) {
         iris$lastAppliedShader.unbind();
         iris$lastAppliedShader = null;
      }

      RenderSystem.assertOnRenderThread();
      GlStateManager._glUseProgram(this.glRef);
      int i = GlStateManager._getActiveTexture();

      for (int j = 0; j < this.samplerLocations.size(); j++) {
         String string = this.samplers.get(j).name();
         int k = this.samplerTextures.getInt(string);
         if (k != -1) {
            int l = this.samplerLocations.getInt(j);
            GlUniform.uniform1(l, j);
            RenderSystem.activeTexture(33984 + j);
            RenderSystem.bindTexture(k);
         }
      }

      GlStateManager._activeTexture(i);

      for (GlUniform glUniform : this.uniforms) {
         glUniform.upload();
      }

      if (!this.iris$shouldSkipThis()) {
         if (!this.iris$isKnownShader() && iris$shouldOverrideShaders()) {
            WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
            if (pipeline instanceof IrisRenderingPipeline irisPipeline && !ShadowRenderer.ACTIVE) {
               irisPipeline.bindDefault();
            }
         }
      } else {
         DepthColorStorage.disableDepthColor();
      }
   }

   @Nullable
   public GlUniform getUniform(String name) {
      RenderSystem.assertOnRenderThread();
      return this.uniformsByName.get(name);
   }

   @Nullable
   public ShaderProgramDefinition.Uniform getUniformDefinition(String name) {
      return this.uniformDefinitionsByName.get(name);
   }

   public Uniform getUniformOrDefault(String name) {
      GlUniform glUniform = this.getUniform(name);
      return glUniform == null ? DEFAULT_UNIFORM : glUniform;
   }

   public void addSamplerTexture(String name, int texture) {
      this.samplerTextures.put(name, texture);
   }

   public GlUniform createGlUniform(ShaderProgramDefinition.Uniform uniform) {
      int i = GlUniform.getTypeIndex(uniform.type());
      int j = uniform.count();
      int k = j > 1 && j <= 4 && i < 8 ? j - 1 : 0;
      GlUniform glUniform = new GlUniform(uniform.name(), i + k, j);
      glUniform.set(uniform);
      return glUniform;
   }

   public void initializeUniforms(VertexFormat.DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window) {
      for (int i = 0; i < 12; i++) {
         int j = RenderSystem.getShaderTexture(i);
         this.addSamplerTexture("Sampler" + i, j);
      }

      if (this.modelViewMat != null) {
         this.modelViewMat.set(viewMatrix);
      }

      if (this.projectionMat != null) {
         this.projectionMat.set(projectionMatrix);
      }

      if (this.colorModulator != null) {
         this.colorModulator.set(RenderSystem.getShaderColor());
      }

      if (this.glintAlpha != null) {
         this.glintAlpha.set(RenderSystem.getShaderGlintAlpha());
      }

      Fog fog = RenderSystem.getShaderFog();
      if (this.fogStart != null) {
         this.fogStart.set(fog.start());
      }

      if (this.fogEnd != null) {
         this.fogEnd.set(fog.end());
      }

      if (this.fogColor != null) {
         this.fogColor.setAndFlip(fog.red(), fog.green(), fog.blue(), fog.alpha());
      }

      if (this.fogShape != null) {
         this.fogShape.set(fog.shape().getId());
      }

      if (this.textureMat != null) {
         this.textureMat.set(RenderSystem.getTextureMatrix());
      }

      if (this.gameTime != null) {
         this.gameTime.set(RenderSystem.getShaderGameTime());
      }

      if (this.screenSize != null) {
         this.screenSize.set((float)window.getFramebufferWidth(), (float)window.getFramebufferHeight());
      }

      if (this.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
         this.lineWidth.set(RenderSystem.getShaderLineWidth());
      }

      RenderSystem.setupShaderLights(this);
   }

   @VisibleForTesting
   public void addUniform(GlUniform uniform) {
      this.uniforms.add(uniform);
      this.uniformsByName.put(uniform.getName(), uniform);
   }

   @VisibleForTesting
   public int getGlRef() {
      return this.glRef;
   }

   static {
      SkipList.shouldSkipList.put(ExtendedShader.class, SkipList.NONE);
      SkipList.shouldSkipList.put(FallbackShader.class, SkipList.NONE);
   }
}
