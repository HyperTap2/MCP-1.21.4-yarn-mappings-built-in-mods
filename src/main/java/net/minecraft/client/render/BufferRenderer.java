package net.minecraft.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import org.jetbrains.annotations.Nullable;

public class BufferRenderer {
   @Nullable
   private static VertexBuffer currentVertexBuffer;

   public static void reset() {
      if (currentVertexBuffer != null) {
         resetCurrentVertexBuffer();
         VertexBuffer.unbind();
      }
   }

   public static void resetCurrentVertexBuffer() {
      currentVertexBuffer = null;
   }

   public static void drawWithGlobalProgram(BuiltBuffer buffer) {
      RenderSystem.assertOnRenderThread();
      VertexBuffer vertexBuffer = upload(buffer);
      vertexBuffer.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
   }

   public static void draw(BuiltBuffer buffer) {
      RenderSystem.assertOnRenderThread();
      VertexBuffer vertexBuffer = upload(buffer);
      vertexBuffer.draw();
   }

   private static VertexBuffer upload(BuiltBuffer buffer) {
      VertexBuffer vertexBuffer = bind(buffer.getDrawParameters().format());
      vertexBuffer.upload(buffer);
      return vertexBuffer;
   }

   private static VertexBuffer bind(VertexFormat vertexFormat) {
      VertexBuffer vertexBuffer = vertexFormat.getBuffer();
      bind(vertexBuffer);
      return vertexBuffer;
   }

   private static void bind(VertexBuffer vertexBuffer) {
      if (vertexBuffer != currentVertexBuffer) {
         vertexBuffer.bind();
         currentVertexBuffer = vertexBuffer;
      }
   }
}
