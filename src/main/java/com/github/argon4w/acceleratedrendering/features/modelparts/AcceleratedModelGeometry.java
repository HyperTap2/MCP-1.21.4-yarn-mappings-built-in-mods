package com.github.argon4w.acceleratedrendering.features.modelparts;

import java.util.List;
import java.util.Map;
import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.CulledMeshCollector;
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Immutable, flattened form of a model part's cuboids. */
public final class AcceleratedModelGeometry implements IAcceleratedRenderer<Void> {
   private static final int FLOATS_PER_VERTEX = 8;
   private final float[] vertices;
   private final Map<IBufferGraph, IMesh> meshes = new Object2ObjectOpenHashMap<>();
   private long resourceGeneration = -1L;

   public AcceleratedModelGeometry(List<ModelPart.Cuboid> cuboids) {
      int vertexCount = 0;
      for (ModelPart.Cuboid cuboid : cuboids) {
         for (ModelPart.Quad quad : cuboid.sides) {
            vertexCount += quad.vertices().length;
         }
      }

      this.vertices = new float[vertexCount * FLOATS_PER_VERTEX];
      int offset = 0;
      for (ModelPart.Cuboid cuboid : cuboids) {
         for (ModelPart.Quad quad : cuboid.sides) {
            Vector3f normal = quad.direction();
            for (ModelPart.Vertex vertex : quad.vertices()) {
               Vector3f position = vertex.pos();
               this.vertices[offset++] = position.x() / 16.0F;
               this.vertices[offset++] = position.y() / 16.0F;
               this.vertices[offset++] = position.z() / 16.0F;
               this.vertices[offset++] = vertex.u();
               this.vertices[offset++] = vertex.v();
               this.vertices[offset++] = normal.x();
               this.vertices[offset++] = normal.y();
               this.vertices[offset++] = normal.z();
            }
         }
      }
   }

   public boolean isEmpty() {
      return this.vertices.length == 0;
   }

   public void render(MatrixStack.Entry entry, VertexConsumer consumer, int light, int overlay, int color) {
      if ((CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui())
         && AcceleratedEntityRenderingFeature.isEnabled()
         && AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline()
         && consumer.isAccelerated()) {
         consumer.doRender(this, null, entry.getPositionMatrix(), entry.getNormalMatrix(), light, overlay, color);
         return;
      }

      Matrix4f transform = entry.getPositionMatrix();
      Vector3f transformedPosition = new Vector3f();
      Vector3f transformedNormal = new Vector3f();
      float lastNormalX = Float.NaN;
      float lastNormalY = Float.NaN;
      float lastNormalZ = Float.NaN;

      for (int offset = 0; offset < this.vertices.length; offset += FLOATS_PER_VERTEX) {
         float normalX = this.vertices[offset + 5];
         float normalY = this.vertices[offset + 6];
         float normalZ = this.vertices[offset + 7];
         if (normalX != lastNormalX || normalY != lastNormalY || normalZ != lastNormalZ) {
            entry.transformNormal(normalX, normalY, normalZ, transformedNormal);
            lastNormalX = normalX;
            lastNormalY = normalY;
            lastNormalZ = normalZ;
         }

         transform.transformPosition(this.vertices[offset], this.vertices[offset + 1], this.vertices[offset + 2], transformedPosition);
         consumer.vertex(
            transformedPosition.x(),
            transformedPosition.y(),
            transformedPosition.z(),
            color,
            this.vertices[offset + 3],
            this.vertices[offset + 4],
            overlay,
            light,
            transformedNormal.x(),
            transformedNormal.y(),
            transformedNormal.z()
         );
      }
   }

   @Override
   public void render(
      VertexConsumer consumer, Void context, Matrix4f transform, Matrix3f normal, int light, int overlay, int color
   ) {
      long generation = AcceleratedRendering.getResourceGeneration();
      if (this.resourceGeneration != generation) {
         this.meshes.clear();
         this.resourceGeneration = generation;
      }

      IMesh mesh = this.meshes.get(consumer);
      consumer.beginTransform(transform, normal);
      try {
         if (mesh == null) {
            CulledMeshCollector collector = new CulledMeshCollector(
               consumer.getRenderType(), consumer.getBufferSet().getBufferEnvironment().getLayout()
            );
            VertexConsumer builder = consumer.decorate(collector);

            for (int offset = 0; offset < this.vertices.length; offset += FLOATS_PER_VERTEX) {
               builder.vertex(
                  this.vertices[offset], this.vertices[offset + 1], this.vertices[offset + 2], -1,
                  this.vertices[offset + 3], this.vertices[offset + 4], overlay, 0,
                  this.vertices[offset + 5], this.vertices[offset + 6], this.vertices[offset + 7]
               );
            }

            collector.flush();
            mesh = AcceleratedEntityRenderingFeature.getMeshType().getBuilder().build(collector);
            this.meshes.put(consumer, mesh);
         }

         mesh.write(consumer, color, light, overlay);
      } finally {
         consumer.endTransform();
      }
   }
}
