package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.helpers.VertexBufferHelper;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;

public class FullScreenQuadRenderer {
   public static final FullScreenQuadRenderer INSTANCE = new FullScreenQuadRenderer();
   private final VertexBuffer quad;

   private FullScreenQuadRenderer() {
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      bufferBuilder.vertex(0.0F, 0.0F, 0.0F).texture(0.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 0.0F, 0.0F).texture(1.0F, 0.0F);
      bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F);
      bufferBuilder.vertex(0.0F, 1.0F, 0.0F).texture(0.0F, 1.0F);
      BuiltBuffer meshData = bufferBuilder.endNullable();
      this.quad = new VertexBuffer(GlUsage.STATIC_WRITE);
      this.quad.bind();
      this.quad.upload(meshData);
      Tessellator.getInstance().clear();
      VertexBuffer.unbind();
   }

   public void render() {
      this.begin();
      this.renderQuad();
      this.end();
   }

   public void begin() {
      ((VertexBufferHelper)this.quad).saveBinding();
      RenderSystem.disableDepthTest();
      BufferRenderer.reset();
      this.quad.bind();
   }

   public void renderQuad() {
      IrisRenderSystem.overridePolygonMode();
      this.quad.draw();
      IrisRenderSystem.restorePolygonMode();
   }

   public void end() {
      RenderSystem.enableDepthTest();
      ((VertexBufferHelper)this.quad).restoreBinding();
   }
}
