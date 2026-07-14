package net.minecraft.client.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.Blaze3dRenderTargetExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Util;
import org.lwjgl.opengl.GL43C;

public abstract class Framebuffer implements Blaze3dRenderTargetExt {
   private static final int field_31901 = 0;
   private static final int field_31902 = 1;
   private static final int field_31903 = 2;
   private static final int field_31904 = 3;
   public int textureWidth;
   public int textureHeight;
   public int viewportWidth;
   public int viewportHeight;
   public final boolean useDepthAttachment;
   public int fbo;
   protected int colorAttachment;
   public int depthAttachment;
   private int iris$depthBufferVersion;
   private int iris$colorBufferVersion;
   private final float[] clearColor = (float[])Util.make(() -> new float[]{1.0F, 1.0F, 1.0F, 0.0F});
   public int texFilter;

   public Framebuffer(boolean useDepth) {
      this.useDepthAttachment = useDepth;
      this.fbo = -1;
      this.colorAttachment = -1;
      this.depthAttachment = -1;
   }

   public void resize(int width, int height) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._enableDepthTest();
      if (this.fbo >= 0) {
         this.delete();
      }

      this.initFbo(width, height);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   public void delete() {
      this.iris$depthBufferVersion++;
      this.iris$colorBufferVersion++;
      RenderSystem.assertOnRenderThreadOrInit();
      this.endRead();
      this.endWrite();
      if (this.depthAttachment > -1) {
         TextureUtil.releaseTextureId(this.depthAttachment);
         this.depthAttachment = -1;
      }

      if (this.colorAttachment > -1) {
         TextureUtil.releaseTextureId(this.colorAttachment);
         this.colorAttachment = -1;
      }

      if (this.fbo > -1) {
         GlStateManager._glBindFramebuffer(36160, 0);
         GlStateManager._glDeleteFramebuffers(this.fbo);
         this.fbo = -1;
      }
   }

   public void copyDepthFrom(Framebuffer framebuffer) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._glBindFramebuffer(36008, framebuffer.fbo);
      GlStateManager._glBindFramebuffer(36009, this.fbo);
      GlStateManager._glBlitFrameBuffer(0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 0, 0, this.textureWidth, this.textureHeight, 256, 9728);
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   public void initFbo(int width, int height) {
      RenderSystem.assertOnRenderThreadOrInit();
      int i = RenderSystem.maxSupportedTextureSize();
      if (width > 0 && width <= i && height > 0 && height <= i) {
         this.viewportWidth = width;
         this.viewportHeight = height;
         this.textureWidth = width;
         this.textureHeight = height;
         this.fbo = GlStateManager.glGenFramebuffers();
         this.colorAttachment = TextureUtil.generateTextureId();
         if (this.useDepthAttachment) {
            this.depthAttachment = TextureUtil.generateTextureId();
            GlStateManager._bindTexture(this.depthAttachment);
            GlStateManager._texParameter(3553, 10241, 9728);
            GlStateManager._texParameter(3553, 10240, 9728);
            GlStateManager._texParameter(3553, 34892, 0);
            GlStateManager._texParameter(3553, 10242, 33071);
            GlStateManager._texParameter(3553, 10243, 33071);
            GlStateManager._texImage2D(3553, 0, 6402, this.textureWidth, this.textureHeight, 0, 6402, 5126, null);
         }

         this.setTexFilter(9728, true);
         GlStateManager._bindTexture(this.colorAttachment);
         GlStateManager._texParameter(3553, 10242, 33071);
         GlStateManager._texParameter(3553, 10243, 33071);
         GlStateManager._texImage2D(3553, 0, 32856, this.textureWidth, this.textureHeight, 0, 6408, 5121, null);
         GlStateManager._glBindFramebuffer(36160, this.fbo);
         GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorAttachment, 0);
         if (this.useDepthAttachment) {
            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthAttachment, 0);
         }

         this.checkFramebufferStatus();
         this.clear();
         this.endRead();
         if (this.useDepthAttachment) {
            GLDebug.nameObject(GL43C.GL_TEXTURE, this.depthAttachment, "Main depth texture");
         }

         GLDebug.nameObject(GL43C.GL_TEXTURE, this.colorAttachment, "Main color texture");
         GLDebug.nameObject(GL43C.GL_FRAMEBUFFER, this.fbo, "Main framebuffer");
      } else {
         throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
      }
   }

   public void setTexFilter(int texFilter) {
      this.setTexFilter(texFilter, false);
   }

   private void setTexFilter(int texFilter, boolean force) {
      RenderSystem.assertOnRenderThreadOrInit();
      if (force || texFilter != this.texFilter) {
         this.texFilter = texFilter;
         GlStateManager._bindTexture(this.colorAttachment);
         GlStateManager._texParameter(3553, 10241, texFilter);
         GlStateManager._texParameter(3553, 10240, texFilter);
         GlStateManager._bindTexture(0);
      }
   }

   public void checkFramebufferStatus() {
      RenderSystem.assertOnRenderThreadOrInit();
      int i = GlStateManager.glCheckFramebufferStatus(36160);
      if (i != 36053) {
         if (i == 36054) {
            throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
         } else if (i == 36055) {
            throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
         } else if (i == 36059) {
            throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
         } else if (i == 36060) {
            throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
         } else if (i == 36061) {
            throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
         } else if (i == 1285) {
            throw new RuntimeException("GL_OUT_OF_MEMORY");
         } else {
            throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
         }
      }
   }

   public void beginRead() {
      RenderSystem.assertOnRenderThread();
      GlStateManager._bindTexture(this.colorAttachment);
   }

   public void endRead() {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._bindTexture(0);
   }

   public void beginWrite(boolean setViewport) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._glBindFramebuffer(36160, this.fbo);
      if (setViewport) {
         GlStateManager._viewport(0, 0, this.viewportWidth, this.viewportHeight);
      }

      if (Iris.getPipelineManager().getPipelineNullable() instanceof IrisRenderingPipeline pipeline) {
         pipeline.setIsMainBound(this == MinecraftClient.getInstance().getFramebuffer());
      }
   }

   public void endWrite() {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._glBindFramebuffer(36160, 0);
   }

   public void setClearColor(float r, float g, float b, float a) {
      this.clearColor[0] = r;
      this.clearColor[1] = g;
      this.clearColor[2] = b;
      this.clearColor[3] = a;
   }

   public void draw(int width, int height) {
      GlStateManager._glBindFramebuffer(36008, this.fbo);
      GlStateManager._glBlitFrameBuffer(0, 0, this.textureWidth, this.textureHeight, 0, 0, width, height, 16384, 9728);
      GlStateManager._glBindFramebuffer(36008, 0);
   }

   public void drawInternal(int width, int height) {
      RenderSystem.assertOnRenderThread();
      GlStateManager._colorMask(true, true, true, false);
      GlStateManager._disableDepthTest();
      GlStateManager._depthMask(false);
      GlStateManager._viewport(0, 0, width, height);
      ShaderProgram shaderProgram = Objects.requireNonNull(RenderSystem.setShader(ShaderProgramKeys.BLIT_SCREEN), "Blit shader not loaded");
      shaderProgram.addSamplerTexture("InSampler", this.colorAttachment);
      BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.BLIT_SCREEN);
      bufferBuilder.vertex(0.0F, 0.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 0.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
      bufferBuilder.vertex(0.0F, 1.0F, 0.0F);
      BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      GlStateManager._depthMask(true);
      GlStateManager._colorMask(true, true, true, true);
   }

   public void clear() {
      RenderSystem.assertOnRenderThreadOrInit();
      this.beginWrite(true);
      GlStateManager._clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
      int i = 16384;
      if (this.useDepthAttachment) {
         GlStateManager._clearDepth(1.0);
         i |= 256;
      }

      GlStateManager._clear(i);
      this.endWrite();
   }

   public int getColorAttachment() {
      return this.colorAttachment;
   }

   public int getDepthAttachment() {
      return this.depthAttachment;
   }

   @Override
   public int iris$getDepthBufferVersion() {
      return this.iris$depthBufferVersion;
   }

   @Override
   public int iris$getColorBufferVersion() {
      return this.iris$colorBufferVersion;
   }
}
