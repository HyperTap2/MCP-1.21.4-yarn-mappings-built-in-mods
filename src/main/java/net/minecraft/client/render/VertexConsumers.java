package net.minecraft.client.render;

import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import java.util.function.Consumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class VertexConsumers {
   public static VertexConsumer union() {
      throw new IllegalArgumentException();
   }

   public static VertexConsumer union(VertexConsumer first) {
      return first;
   }

   public static VertexConsumer union(VertexConsumer first, VertexConsumer second) {
      return new VertexConsumers.Dual(first, second);
   }

   public static VertexConsumer union(VertexConsumer... delegates) {
      return new VertexConsumers.Union(delegates);
   }

   static class Dual implements VertexConsumer {
      private final VertexConsumer first;
      private final VertexConsumer second;

      public Dual(VertexConsumer first, VertexConsumer second) {
         if (first == second) {
            throw new IllegalArgumentException("Duplicate delegates");
         }

         this.first = first;
         this.second = second;
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         this.first.vertex(x, y, z);
         this.second.vertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         this.first.color(red, green, blue, alpha);
         this.second.color(red, green, blue, alpha);
         return this;
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         this.first.texture(u, v);
         this.second.texture(u, v);
         return this;
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         this.first.overlay(u, v);
         this.second.overlay(u, v);
         return this;
      }

      @Override
      public VertexConsumer light(int u, int v) {
         this.first.light(u, v);
         this.second.light(u, v);
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         this.first.normal(x, y, z);
         this.second.normal(x, y, z);
         return this;
      }

      @Override
      public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
         this.first.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
         this.second.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
      }

      @Override
      public boolean isAccelerated() {
         return this.first.isAccelerated() && this.second.isAccelerated();
      }

      @Override
      public <T> void doRender(
         IAcceleratedRenderer<T> renderer, T context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color
      ) {
         this.first.doRender(renderer, context, transform, normal, light, overlay, color);
         this.second.doRender(renderer, context, transform, normal, light, overlay, color);
      }
   }

   record Union(VertexConsumer[] delegates) implements VertexConsumer {
      Union(VertexConsumer[] delegates) {
         for (int i = 0; i < delegates.length; i++) {
            for (int j = i + 1; j < delegates.length; j++) {
               if (delegates[i] == delegates[j]) {
                  throw new IllegalArgumentException("Duplicate delegates");
               }
            }
         }

         this.delegates = delegates;
      }

      private void delegate(Consumer<VertexConsumer> action) {
         for (VertexConsumer vertexConsumer : this.delegates) {
            action.accept(vertexConsumer);
         }
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         this.delegate(vertexConsumer -> vertexConsumer.vertex(x, y, z));
         return this;
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         this.delegate(vertexConsumer -> vertexConsumer.color(red, green, blue, alpha));
         return this;
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         this.delegate(vertexConsumer -> vertexConsumer.texture(u, v));
         return this;
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         this.delegate(vertexConsumer -> vertexConsumer.overlay(u, v));
         return this;
      }

      @Override
      public VertexConsumer light(int u, int v) {
         this.delegate(vertexConsumer -> vertexConsumer.light(u, v));
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         this.delegate(vertexConsumer -> vertexConsumer.normal(x, y, z));
         return this;
      }

      @Override
      public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
         this.delegate(vertexConsumer -> vertexConsumer.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ));
      }

      @Override
      public boolean isAccelerated() {
         for (VertexConsumer delegate : this.delegates) {
            if (!delegate.isAccelerated()) {
               return false;
            }
         }

         return true;
      }

      @Override
      public <T> void doRender(
         IAcceleratedRenderer<T> renderer, T context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color
      ) {
         for (VertexConsumer delegate : this.delegates) {
            delegate.doRender(renderer, context, transform, normal, light, overlay, color);
         }
      }
   }
}
