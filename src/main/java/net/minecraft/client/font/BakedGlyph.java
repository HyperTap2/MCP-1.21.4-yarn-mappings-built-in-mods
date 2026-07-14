package net.minecraft.client.font;

import com.github.argon4w.acceleratedrendering.AcceleratedRendering;
import com.github.argon4w.acceleratedrendering.core.CoreFeature;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IBufferGraph;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.builders.IAcceleratedVertexConsumer;
import com.github.argon4w.acceleratedrendering.core.buffers.accelerated.renderers.IAcceleratedRenderer;
import com.github.argon4w.acceleratedrendering.core.meshes.IMesh;
import com.github.argon4w.acceleratedrendering.core.meshes.collectors.SimpleMeshCollector;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedBakedGlyphRenderer;
import com.github.argon4w.acceleratedrendering.features.text.AcceleratedTextRenderingFeature;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.Style;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public class BakedGlyph implements IAcceleratedRenderer<BakedGlyph.Rectangle> {
   public static final float field_55098 = 0.001F;
   private static final Matrix3f ACCELERATED_RECTANGLE_NORMAL = new Matrix3f();
   private final TextRenderLayerSet textRenderLayers;
   private final float minU;
   private final float maxU;
   private final float minV;
   private final float maxV;
   private final float minX;
   private final float maxX;
   private final float minY;
   private final float maxY;
   private final AcceleratedBakedGlyphRenderer acceleratedRendering$normalRenderer;
   private final AcceleratedBakedGlyphRenderer acceleratedRendering$italicRenderer;
   private final AcceleratedBakedGlyphRenderer acceleratedRendering$boldRenderer;
   private final AcceleratedBakedGlyphRenderer acceleratedRendering$italicBoldRenderer;
   private final Map<BakedGlyph.Rectangle, Map<IBufferGraph, IMesh>> acceleratedRendering$rectangleMeshes = new Object2ObjectOpenHashMap<>();
   private long acceleratedRendering$resourceGeneration = -1L;

   public BakedGlyph(TextRenderLayerSet textRenderLayers, float minU, float maxU, float minV, float maxV, float minX, float maxX, float minY, float maxY) {
      this.textRenderLayers = textRenderLayers;
      this.minU = minU;
      this.maxU = maxU;
      this.minV = minV;
      this.maxV = maxV;
      this.minX = minX;
      this.maxX = maxX;
      this.minY = minY;
      this.maxY = maxY;
      this.acceleratedRendering$normalRenderer = new AcceleratedBakedGlyphRenderer(this, false, false);
      this.acceleratedRendering$italicRenderer = new AcceleratedBakedGlyphRenderer(this, true, false);
      this.acceleratedRendering$boldRenderer = new AcceleratedBakedGlyphRenderer(this, false, true);
      this.acceleratedRendering$italicBoldRenderer = new AcceleratedBakedGlyphRenderer(this, true, true);
   }

   public void draw(BakedGlyph.DrawnGlyph glyph, Matrix4f matrix, VertexConsumer vertexConsumer, int light) {
      Style style = glyph.style();
      boolean bl = style.isItalic();
      float f = glyph.x();
      float g = glyph.y();
      int i = glyph.color();
      int j = glyph.shadowColor();
      boolean bl2 = style.isBold();
      if (glyph.hasShadow()) {
         this.draw(bl, f + glyph.shadowOffset(), g + glyph.shadowOffset(), matrix, vertexConsumer, j, bl2, light);
         this.draw(bl, f, g, 0.03F, matrix, vertexConsumer, i, bl2, light);
      } else {
         this.draw(bl, f, g, matrix, vertexConsumer, i, bl2, light);
      }

      if (bl2) {
         if (glyph.hasShadow()) {
            this.draw(bl, f + glyph.boldOffset() + glyph.shadowOffset(), g + glyph.shadowOffset(), 0.001F, matrix, vertexConsumer, j, true, light);
            this.draw(bl, f + glyph.boldOffset(), g, 0.03F, matrix, vertexConsumer, i, true, light);
         } else {
            this.draw(bl, f + glyph.boldOffset(), g, matrix, vertexConsumer, i, true, light);
         }
      }
   }

   private void draw(boolean italic, float x, float y, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean bold, int light) {
      this.draw(italic, x, y, 0.0F, matrix, vertexConsumer, color, bold, light);
   }

   private void draw(boolean italic, float x, float y, float z, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean bold, int light) {
      if (AcceleratedRendering.isAvailable()
         && (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
         && AcceleratedTextRenderingFeature.isEnabled()
         && AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
         && vertexConsumer.isAccelerated()) {
         AcceleratedBakedGlyphRenderer renderer = italic
            ? (bold ? this.acceleratedRendering$italicBoldRenderer : this.acceleratedRendering$italicRenderer)
            : (bold ? this.acceleratedRendering$boldRenderer : this.acceleratedRendering$normalRenderer);
         vertexConsumer.doRender(renderer, new Vector3f(x, y, z), matrix, null, light, OverlayTexture.DEFAULT_UV, color);
         return;
      }

      float f = x + this.minX;
      float g = x + this.maxX;
      float h = y + this.minY;
      float i = y + this.maxY;
      float j = italic ? 1.0F - 0.25F * this.minY : 0.0F;
      float k = italic ? 1.0F - 0.25F * this.maxY : 0.0F;
      float l = bold ? 0.1F : 0.0F;
      vertexConsumer.vertex(matrix, f + j - l, h - l, z).color(color).texture(this.minU, this.minV).light(light);
      vertexConsumer.vertex(matrix, f + k - l, i + l, z).color(color).texture(this.minU, this.maxV).light(light);
      vertexConsumer.vertex(matrix, g + k + l, i + l, z).color(color).texture(this.maxU, this.maxV).light(light);
      vertexConsumer.vertex(matrix, g + j + l, h - l, z).color(color).texture(this.maxU, this.minV).light(light);
   }

   public void drawRectangle(BakedGlyph.Rectangle rectangle, Matrix4f matrix, VertexConsumer vertexConsumer, int light) {
      if (rectangle.hasShadow()) {
         this.drawRectangle(rectangle, rectangle.shadowOffset(), 0.0F, rectangle.shadowColor(), vertexConsumer, light, matrix);
         this.drawRectangle(rectangle, 0.0F, 0.03F, rectangle.color, vertexConsumer, light, matrix);
      } else {
         this.drawRectangle(rectangle, 0.0F, 0.0F, rectangle.color, vertexConsumer, light, matrix);
      }
   }

   private void drawRectangle(
      BakedGlyph.Rectangle rectangle, float shadowOffset, float zOffset, int color, VertexConsumer vertexConsumer, int light, Matrix4f matrix
   ) {
      if (AcceleratedRendering.isAvailable()
         && (CoreFeature.isRenderingLevel() || CoreFeature.isRenderingGui())
         && AcceleratedTextRenderingFeature.isEnabled()
         && AcceleratedTextRenderingFeature.shouldUseAcceleratedPipeline()
         && vertexConsumer.isAccelerated()) {
         BakedGlyph.Rectangle transformed = new BakedGlyph.Rectangle(
            rectangle.minX + shadowOffset,
            rectangle.minY + shadowOffset,
            rectangle.maxX + shadowOffset,
            rectangle.maxY + shadowOffset,
            rectangle.zIndex + zOffset,
            color
         );
         vertexConsumer.doRender(this, transformed, matrix, ACCELERATED_RECTANGLE_NORMAL, light, OverlayTexture.DEFAULT_UV, color);
         return;
      }

      vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.minY + shadowOffset, rectangle.zIndex + zOffset)
         .color(color)
         .texture(this.minU, this.minV)
         .light(light);
      vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.minY + shadowOffset, rectangle.zIndex + zOffset)
         .color(color)
         .texture(this.minU, this.maxV)
         .light(light);
      vertexConsumer.vertex(matrix, rectangle.maxX + shadowOffset, rectangle.maxY + shadowOffset, rectangle.zIndex + zOffset)
         .color(color)
         .texture(this.maxU, this.maxV)
         .light(light);
      vertexConsumer.vertex(matrix, rectangle.minX + shadowOffset, rectangle.maxY + shadowOffset, rectangle.zIndex + zOffset)
         .color(color)
         .texture(this.maxU, this.minV)
         .light(light);
   }

   @Override
   public void render(
      VertexConsumer vertexConsumer,
      BakedGlyph.Rectangle rectangle,
      Matrix4f transform,
      Matrix3f normal,
      int light,
      int overlay,
      int color
   ) {
      long generation = AcceleratedRendering.getResourceGeneration();
      if (this.acceleratedRendering$resourceGeneration != generation) {
         this.acceleratedRendering$rectangleMeshes.clear();
         this.acceleratedRendering$resourceGeneration = generation;
      }

      IAcceleratedVertexConsumer extension = (IAcceleratedVertexConsumer)vertexConsumer;
      Map<IBufferGraph, IMesh> meshes = this.acceleratedRendering$rectangleMeshes.computeIfAbsent(
         rectangle, ignored -> new Object2ObjectOpenHashMap<>()
      );
      IMesh mesh = meshes.get(extension);
      extension.beginTransform(transform, normal);

      try {
         if (mesh == null) {
            SimpleMeshCollector collector = new SimpleMeshCollector(extension.getBufferSet().getBufferEnvironment().getLayout());
            VertexConsumer builder = extension.decorate(collector);
            builder.vertex(rectangle.minX, rectangle.minY, rectangle.zIndex, -1, this.minU, this.minV, overlay, 0, 0.0F, 0.0F, 0.0F);
            builder.vertex(rectangle.maxX, rectangle.minY, rectangle.zIndex, -1, this.minU, this.maxV, overlay, 0, 0.0F, 0.0F, 0.0F);
            builder.vertex(rectangle.maxX, rectangle.maxY, rectangle.zIndex, -1, this.maxU, this.maxV, overlay, 0, 0.0F, 0.0F, 0.0F);
            builder.vertex(rectangle.minX, rectangle.maxY, rectangle.zIndex, -1, this.maxU, this.minV, overlay, 0, 0.0F, 0.0F, 0.0F);
            mesh = AcceleratedTextRenderingFeature.getMeshType().getBuilder().build(collector);
            meshes.put(extension, mesh);
         }

         mesh.write(extension, color, light, overlay);
      } finally {
         extension.endTransform();
      }
   }

   public RenderLayer getLayer(TextRenderer.TextLayerType layerType) {
      return this.textRenderLayers.getRenderLayer(layerType);
   }

   public float acceleratedRendering$getMinU() {
      return this.minU;
   }

   public float acceleratedRendering$getMaxU() {
      return this.maxU;
   }

   public float acceleratedRendering$getMinV() {
      return this.minV;
   }

   public float acceleratedRendering$getMaxV() {
      return this.maxV;
   }

   public float acceleratedRendering$getMinX() {
      return this.minX;
   }

   public float acceleratedRendering$getMaxX() {
      return this.maxX;
   }

   public float acceleratedRendering$getMinY() {
      return this.minY;
   }

   public float acceleratedRendering$getMaxY() {
      return this.maxY;
   }

   public record DrawnGlyph(float x, float y, int color, int shadowColor, BakedGlyph glyph, Style style, float boldOffset, float shadowOffset) {
      boolean hasShadow() {
         return this.shadowColor() != 0;
      }
   }

   public record Rectangle(float minX, float minY, float maxX, float maxY, float zIndex, int color, int shadowColor, float shadowOffset) {

      public Rectangle(float minX, float minY, float maxX, float maxY, float zIndex, int color) {
         this(minX, minY, maxX, maxY, zIndex, color, 0, 0.0F);
      }

      boolean hasShadow() {
         return this.shadowColor() != 0;
      }
   }
}
