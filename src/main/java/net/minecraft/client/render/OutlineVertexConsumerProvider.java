package net.minecraft.client.render;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.AcceleratedEntityOutlineGenerator;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.DecoratedRenderer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import java.util.Optional;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class OutlineVertexConsumerProvider implements VertexConsumerProvider {
   private final VertexConsumerProvider.Immediate parent;
   private final VertexConsumerProvider.Immediate plainDrawer = VertexConsumerProvider.immediate(new BufferAllocator(1536));
   private int red = 255;
   private int green = 255;
   private int blue = 255;
   private int alpha = 255;

   public OutlineVertexConsumerProvider(VertexConsumerProvider.Immediate parent) {
      this.parent = parent;
   }

   @Override
   public VertexConsumer getBuffer(RenderLayer renderLayer) {
      if (renderLayer.isOutline()) {
         VertexConsumer vertexConsumer = this.plainDrawer.getBuffer(renderLayer);
         return new OutlineVertexConsumerProvider.OutlineVertexConsumer(vertexConsumer, this.red, this.green, this.blue, this.alpha);
      } else {
         VertexConsumer vertexConsumer = this.parent.getBuffer(renderLayer);
         Optional<RenderLayer> optional = renderLayer.getAffectedOutline();
         if (optional.isPresent()) {
            VertexConsumer vertexConsumer2 = this.plainDrawer.getBuffer(optional.get());
            OutlineVertexConsumerProvider.OutlineVertexConsumer outlineVertexConsumer = new OutlineVertexConsumerProvider.OutlineVertexConsumer(
               vertexConsumer2, this.red, this.green, this.blue, this.alpha
            );
            return VertexConsumers.union(outlineVertexConsumer, vertexConsumer);
         } else {
            return vertexConsumer;
         }
      }
   }

   public void setColor(int red, int green, int blue, int alpha) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.alpha = alpha;
   }

   public void draw() {
      this.plainDrawer.draw();
   }

   public VertexConsumerProvider.Immediate iris$getPlainDrawer() {
      return this.plainDrawer;
   }

   record OutlineVertexConsumer(VertexConsumer delegate, int color) implements VertexConsumer {
      public OutlineVertexConsumer(VertexConsumer delegate, int red, int green, int blue, int alpha) {
         this(delegate, ColorHelper.getArgb(alpha, red, green, blue));
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         this.delegate.vertex(x, y, z).color(this.color);
         return this;
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         return this;
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         this.delegate.texture(u, v);
         return this;
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer light(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         return this;
      }

      @Override
      public VertexConsumer decorate(VertexConsumer buffer) {
         return new AcceleratedEntityOutlineGenerator(buffer, this.color);
      }

      @Override
      public boolean isAccelerated() {
         return this.delegate.isAccelerated();
      }

      @Override
      public <T> void doRender(
         IAcceleratedRenderer<T> renderer, T context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color
      ) {
         this.delegate.doRender(new DecoratedRenderer<>(renderer, this), context, transform, normal, light, overlay, color);
      }
   }
}
