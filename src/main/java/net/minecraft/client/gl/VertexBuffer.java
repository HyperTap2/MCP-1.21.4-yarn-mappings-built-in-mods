package net.minecraft.client.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.irisshaders.iris.helpers.VertexBufferHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.BufferAllocator;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class VertexBuffer implements AutoCloseable, VertexBufferHelper {
   @Nullable
   private static VertexBuffer iris$current;
   @Nullable
   private static VertexBuffer iris$saved;
   private final GlUsage usage;
   private final GpuBuffer vertexBuffer;
   @Nullable
   private GpuBuffer indexBuffer = null;
   private int vertexArrayId;
   @Nullable
   private VertexFormat vertexFormat;
   @Nullable
   private RenderSystem.ShapeIndexBuffer sharedSequentialIndexBuffer;
   private VertexFormat.IndexType indexType;
   private int indexCount;
   private VertexFormat.DrawMode drawMode;

   public VertexBuffer(GlUsage usage) {
      this.usage = usage;
      RenderSystem.assertOnRenderThread();
      this.vertexBuffer = new GpuBuffer(GlBufferTarget.VERTICES, usage, 0);
      this.vertexArrayId = GlStateManager._glGenVertexArrays();
   }

   public static VertexBuffer createAndUpload(VertexFormat.DrawMode drawMode, VertexFormat format, Consumer<VertexConsumer> callback) {
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(drawMode, format);
      callback.accept(bufferBuilder);
      VertexBuffer vertexBuffer = new VertexBuffer(GlUsage.STATIC_WRITE);
      vertexBuffer.bind();
      vertexBuffer.upload(bufferBuilder.end());
      unbind();
      return vertexBuffer;
   }

   public void upload(BuiltBuffer data) {
      try (data) {
         if (this.isClosed()) {
            return;
         }

         RenderSystem.assertOnRenderThread();
         BuiltBuffer.DrawParameters drawParameters = data.getDrawParameters();
         this.vertexFormat = this.uploadVertexBuffer(drawParameters, data.getBuffer());
         this.sharedSequentialIndexBuffer = this.uploadIndexBuffer(drawParameters, data.getSortedBuffer());
         this.indexCount = drawParameters.indexCount();
         this.indexType = drawParameters.indexType();
         this.drawMode = drawParameters.mode();
      }
   }

   public void uploadIndexBuffer(BufferAllocator.CloseableBuffer buf) {
      try (buf) {
         if (this.isClosed()) {
            return;
         }

         RenderSystem.assertOnRenderThread();
         if (this.indexBuffer != null) {
            this.indexBuffer.close();
         }

         this.indexBuffer = new GpuBuffer(GlBufferTarget.INDICES, this.usage, buf.getBuffer());
         this.sharedSequentialIndexBuffer = null;
      }
   }

   private VertexFormat uploadVertexBuffer(BuiltBuffer.DrawParameters parameters, @Nullable ByteBuffer vertexBuffer) {
      boolean bl = false;
      if (!parameters.format().equals(this.vertexFormat)) {
         if (this.vertexFormat != null) {
            this.vertexFormat.clearState();
         }

         this.vertexBuffer.bind();
         parameters.format().setupState();
         bl = true;
      }

      if (vertexBuffer != null) {
         if (!bl) {
            this.vertexBuffer.bind();
         }

         this.vertexBuffer.resize(vertexBuffer.remaining());
         this.vertexBuffer.copyFrom(vertexBuffer, 0);
      }

      return parameters.format();
   }

   @Nullable
   private RenderSystem.ShapeIndexBuffer uploadIndexBuffer(BuiltBuffer.DrawParameters parameters, @Nullable ByteBuffer buf) {
      if (buf != null) {
         if (this.indexBuffer != null) {
            this.indexBuffer.close();
         }

         this.indexBuffer = new GpuBuffer(GlBufferTarget.INDICES, this.usage, buf);
         return null;
      } else {
         RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(parameters.mode());
         if (shapeIndexBuffer != this.sharedSequentialIndexBuffer || !shapeIndexBuffer.isLargeEnough(parameters.indexCount())) {
            shapeIndexBuffer.bindAndGrow(parameters.indexCount());
         }

         return shapeIndexBuffer;
      }
   }

   public void bind() {
      iris$current = this;
      BufferRenderer.resetCurrentVertexBuffer();
      GlStateManager._glBindVertexArray(this.vertexArrayId);
   }

   public static void unbind() {
      iris$current = null;
      BufferRenderer.resetCurrentVertexBuffer();
      GlStateManager._glBindVertexArray(0);
   }

   @Override
   public void saveBinding() {
      iris$saved = iris$current;
   }

   @Override
   public void restoreBinding() {
      if (iris$saved != null) {
         iris$saved.bind();
      } else {
         unbind();
      }
   }

   public void draw() {
      RenderSystem.drawElements(this.drawMode.glMode, this.indexCount, this.getIndexType().glType);
   }

   private VertexFormat.IndexType getIndexType() {
      RenderSystem.ShapeIndexBuffer shapeIndexBuffer = this.sharedSequentialIndexBuffer;
      return shapeIndexBuffer != null ? shapeIndexBuffer.getIndexType() : this.indexType;
   }

   public void draw(Matrix4f viewMatrix, Matrix4f projectionMatrix, @Nullable ShaderProgram program) {
      if (program != null) {
         RenderSystem.assertOnRenderThread();
         program.initializeUniforms(this.drawMode, viewMatrix, projectionMatrix, MinecraftClient.getInstance().getWindow());
         program.bind();
         this.draw();
         program.unbind();
      }
   }

   public void draw(RenderLayer layer) {
      layer.startDrawing();
      this.bind();
      this.draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
      unbind();
      layer.endDrawing();
   }

   @Override
   public void close() {
      this.vertexBuffer.close();
      if (this.indexBuffer != null) {
         this.indexBuffer.close();
         this.indexBuffer = null;
      }

      if (this.vertexArrayId >= 0) {
         RenderSystem.glDeleteVertexArrays(this.vertexArrayId);
         this.vertexArrayId = -1;
      }
   }

   public VertexFormat getVertexFormat() {
      return this.vertexFormat;
   }

   public boolean isClosed() {
      return this.vertexArrayId == -1;
   }
}
