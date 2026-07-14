package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.ViaFabricPlus;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.github.argon4w.acceleratedrendering.AcceleratedTextCache;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class TextRenderer {
   private static final float Z_INDEX = 0.01F;
   public static final float FORWARD_SHIFT = 0.03F;
   public static final int field_55090 = 0;
   public static final int ARABIC_SHAPING_LETTERS_SHAPE = 8;
   public final int fontHeight = 9;
   public final Random random = Random.create();
   private final Function<Identifier, FontStorage> fontStorageAccessor;
   final boolean validateAdvance;
   private final TextHandler handler;

   public TextRenderer(Function<Identifier, FontStorage> fontStorageAccessor, boolean validateAdvance) {
      this.fontStorageAccessor = fontStorageAccessor;
      this.validateAdvance = validateAdvance;
      this.handler = new TextHandler(
         (codePoint, style) -> this.getFontStorage(style.getFont()).getGlyph(codePoint, this.validateAdvance).getAdvance(style.isBold())
      );
   }

   FontStorage getFontStorage(Identifier id) {
      return this.fontStorageAccessor.apply(id);
   }

   public String mirror(String text) {
      try {
         Bidi bidi = new Bidi(new ArabicShaping(8).shape(text), 127);
         bidi.setReorderingMode(0);
         return bidi.writeReordered(2);
      } catch (ArabicShapingException var3) {
         return text;
      }
   }

   public int draw(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light
   ) {
      if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         String renderedText = this.isRightToLeft() ? this.mirror(text) : text;
         List<OrderedText> lines = this.wrapLines(StringVisitable.plain(renderedText), Integer.MAX_VALUE);
         if (lines.size() > 1) {
            int offsetX = 0;

            for (int i = 0; i < lines.size(); i++) {
               offsetX = this.drawInternal(
                  lines.get(i),
                  x,
                  y - lines.size() * (this.fontHeight + 2) + i * (this.fontHeight + 2),
                  color,
                  shadow,
                  new Matrix4f(matrix),
                  vertexConsumers,
                  layerType,
                  backgroundColor,
                  light,
                  true
               );
            }

            return offsetX;
         }
      }

      if (this.isRightToLeft()) {
         text = this.mirror(text);
      }

      return this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
   }

   public int draw(
      Text text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light
   ) {
      if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         List<OrderedText> lines = this.wrapLines(text, Integer.MAX_VALUE);
         if (lines.size() > 1) {
            int offsetX = 0;

            for (int i = 0; i < lines.size(); i++) {
               offsetX = this.draw(
                  lines.get(i),
                  x,
                  y - lines.size() * (this.fontHeight + 2) + i * (this.fontHeight + 2),
                  color,
                  shadow,
                  new Matrix4f(matrix),
                  vertexConsumers,
                  layerType,
                  backgroundColor,
                  light
               );
            }

            return offsetX;
         }
      }

      return this.draw(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
   }

   public int draw(
      Text text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light,
      boolean swapZIndex
   ) {
      return this.drawInternal(text.asOrderedText(), x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, swapZIndex);
   }

   public int draw(
      OrderedText text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light
   ) {
      return this.drawInternal(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, true);
   }

   public void drawWithOutline(
      OrderedText text, float x, float y, int color, int outlineColor, Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light
   ) {
      int i = tweakTransparency(outlineColor);
      TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumers, 0.0F, 0.0F, i, false, matrix, TextRenderer.TextLayerType.NORMAL, light);

      for (int j = -1; j <= 1; j++) {
         for (int k = -1; k <= 1; k++) {
            if (j != 0 || k != 0) {
               float[] fs = new float[]{x};
               int l = j;
               int m = k;
               text.accept((index, style, codePoint) -> {
                  boolean bl = style.isBold();
                  FontStorage fontStorage = this.getFontStorage(style.getFont());
                  Glyph glyph = fontStorage.getGlyph(codePoint, this.validateAdvance);
                  drawer.x = fs[0] + l * glyph.getShadowOffset();
                  drawer.y = y + m * glyph.getShadowOffset();
                  fs[0] += glyph.getAdvance(bl);
                  return drawer.accept(index, style.withColor(i), codePoint);
               });
            }
         }
      }

      drawer.drawGlyphs();
      TextRenderer.Drawer drawer2 = new TextRenderer.Drawer(
         vertexConsumers, x, y, tweakTransparency(color), false, matrix, TextRenderer.TextLayerType.POLYGON_OFFSET, light
      );
      text.accept(drawer2);
      drawer2.drawLayer(x);
   }

   private static int tweakTransparency(int argb) {
      return (argb & -67108864) == 0 ? ColorHelper.fullAlpha(argb) : argb;
   }

   private int drawInternal(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumers,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light,
      boolean mirror
   ) {
      color = tweakTransparency(color);
      x = this.drawLayer(text, x, y, color, shadow, matrix, vertexConsumers, layerType, backgroundColor, light, mirror);
      return (int)x + (shadow ? 1 : 0);
   }

   private int drawInternal(
      OrderedText text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumerProvider,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light,
      boolean swapZIndex
   ) {
      color = tweakTransparency(color);
      x = this.drawLayer(text, x, y, color, shadow, matrix, vertexConsumerProvider, layerType, backgroundColor, light, swapZIndex);
      return (int)x + (shadow ? 1 : 0);
   }

   private float drawLayer(
      String text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumerProvider,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light,
      boolean swapZIndex
   ) {
      TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumerProvider, x, y, color, backgroundColor, shadow, matrix, layerType, light, swapZIndex);
      AcceleratedTextCache.visitFormatted(text, drawer);
      return drawer.drawLayer(x);
   }

   private float drawLayer(
      OrderedText text,
      float x,
      float y,
      int color,
      boolean shadow,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumerProvider,
      TextRenderer.TextLayerType layerType,
      int backgroundColor,
      int light,
      boolean swapZIndex
   ) {
      TextRenderer.Drawer drawer = new TextRenderer.Drawer(vertexConsumerProvider, x, y, color, backgroundColor, shadow, matrix, layerType, light, swapZIndex);
      AcceleratedTextCache.visitOrdered(text, drawer);
      return drawer.drawLayer(x);
   }

   public int getWidth(String text) {
      return MathHelper.ceil(AcceleratedTextCache.getWidth(text, this.handler::getWidth));
   }

   public int getWidth(StringVisitable text) {
      if (MinecraftClient.getInstance().world != null
         && ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
         int width = 0;

         for (OrderedText line : this.wrapLines(text, Integer.MAX_VALUE)) {
            width = Math.max(width, this.getWidth(line));
         }

         return MathHelper.ceil(width);
      }

      return MathHelper.ceil(this.handler.getWidth(text));
   }

   public int getWidth(OrderedText text) {
      return MathHelper.ceil(AcceleratedTextCache.getWidth(text, this.handler::getWidth));
   }

   public String trimToWidth(String text, int maxWidth, boolean backwards) {
      return backwards ? this.handler.trimToWidthBackwards(text, maxWidth, Style.EMPTY) : this.handler.trimToWidth(text, maxWidth, Style.EMPTY);
   }

   public String trimToWidth(String text, int maxWidth) {
      return this.handler.trimToWidth(text, maxWidth, Style.EMPTY);
   }

   public StringVisitable trimToWidth(StringVisitable text, int width) {
      return this.handler.trimToWidth(text, width, Style.EMPTY);
   }

   public int getWrappedLinesHeight(String text, int maxWidth) {
      return 9 * this.handler.wrapLines(text, maxWidth, Style.EMPTY).size();
   }

   public int getWrappedLinesHeight(StringVisitable text, int maxWidth) {
      return 9 * this.handler.wrapLines(text, maxWidth, Style.EMPTY).size();
   }

   public List<OrderedText> wrapLines(StringVisitable text, int width) {
      return Language.getInstance().reorder(this.handler.wrapLines(text, width, Style.EMPTY));
   }

   public boolean isRightToLeft() {
      return Language.getInstance().isRightToLeft();
   }

   public TextHandler getTextHandler() {
      return this.handler;
   }

   class Drawer implements CharacterVisitor {
      final VertexConsumerProvider vertexConsumers;
      private final boolean shadow;
      private final int color;
      private final int backgroundColor;
      private final Matrix4f matrix;
      private final TextRenderer.TextLayerType layerType;
      private final int light;
      private final boolean swapZIndex;
      float x;
      float y;
      private final List<BakedGlyph.DrawnGlyph> glyphs = new ArrayList<>();
      @Nullable
      private List<BakedGlyph.Rectangle> rectangles;

      private void addRectangle(BakedGlyph.Rectangle rectangle) {
         if (this.rectangles == null) {
            this.rectangles = Lists.newArrayList();
         }

         this.rectangles.add(rectangle);
      }

      public Drawer(
         final VertexConsumerProvider vertexConsumers,
         final float x,
         final float y,
         final int color,
         final boolean shadow,
         final Matrix4f matrix,
         final TextRenderer.TextLayerType layerType,
         final int light
      ) {
         this(vertexConsumers, x, y, color, 0, shadow, matrix, layerType, light, true);
      }

      public Drawer(
         final VertexConsumerProvider vertexConsumers,
         final float x,
         final float y,
         final int color,
         final int backgroundColor,
         final boolean shadow,
         final Matrix4f matrix,
         final TextRenderer.TextLayerType layerType,
         final int light,
         final boolean swapZIndex
      ) {
         this.vertexConsumers = vertexConsumers;
         this.x = x;
         this.y = y;
         this.shadow = shadow;
         this.color = color;
         this.backgroundColor = backgroundColor;
         this.matrix = matrix;
         this.layerType = layerType;
         this.light = light;
         this.swapZIndex = swapZIndex;
      }

      public boolean accept(int i, Style style, int j) {
         FontStorage fontStorage = TextRenderer.this.getFontStorage(style.getFont());
         Glyph glyph = fontStorage.getGlyph(j, TextRenderer.this.validateAdvance);
         BakedGlyph bakedGlyph = style.isObfuscated() && j != 32 ? fontStorage.getObfuscatedBakedGlyph(glyph) : fontStorage.getBaked(j);
         boolean bl = style.isBold();
         TextColor textColor = style.getColor();
         int k = this.getRenderColor(textColor);
         int l = this.getShadowColor(style, k);
         float f = glyph.getAdvance(bl);
         float g = i == 0 ? this.x - 1.0F : this.x;
         float h = glyph.getShadowOffset();
         if (!(bakedGlyph instanceof EmptyBakedGlyph)) {
            float m = bl ? glyph.getBoldOffset() : 0.0F;
            this.glyphs.add(new BakedGlyph.DrawnGlyph(this.x, this.y, k, l, bakedGlyph, style, m, h));
         }

         if (style.isStrikethrough()) {
            float offset = ViaFabricPlus.getImpl().getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) ? 0.5F : 0.0F;
            this.addRectangle(
               new BakedGlyph.Rectangle(g, this.y + 4.5F - offset, this.x + f, this.y + 4.5F - 1.0F - offset, this.getForegroundZIndex(), k, l, h)
            );
         }

         if (style.isUnderlined()) {
            this.addRectangle(new BakedGlyph.Rectangle(g, this.y + 9.0F, this.x + f, this.y + 9.0F - 1.0F, this.getForegroundZIndex(), k, l, h));
         }

         this.x += f;
         return true;
      }

      float drawLayer(float x) {
         BakedGlyph bakedGlyph = null;
         if (this.backgroundColor != 0) {
            BakedGlyph.Rectangle rectangle = new BakedGlyph.Rectangle(
               x - 1.0F, this.y + 9.0F, this.x, this.y - 1.0F, this.getBackgroundZIndex(), this.backgroundColor
            );
            bakedGlyph = TextRenderer.this.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
            bakedGlyph.drawRectangle(rectangle, this.matrix, vertexConsumer, this.light);
         }

         this.drawGlyphs();
         if (this.rectangles != null) {
            if (bakedGlyph == null) {
               bakedGlyph = TextRenderer.this.getFontStorage(Style.DEFAULT_FONT_ID).getRectangleBakedGlyph();
            }

            VertexConsumer vertexConsumer2 = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));

            for (BakedGlyph.Rectangle rectangle2 : this.rectangles) {
               bakedGlyph.drawRectangle(rectangle2, this.matrix, vertexConsumer2, this.light);
            }
         }

         return this.x;
      }

      private int getRenderColor(@Nullable TextColor override) {
         if (override != null) {
            int i = ColorHelper.getAlpha(this.color);
            int j = override.getRgb();
            return ColorHelper.withAlpha(i, j);
         } else {
            return this.color;
         }
      }

      private int getShadowColor(Style style, int textColor) {
         Integer integer = style.getShadowColor();
         if (integer != null) {
            float f = ColorHelper.getAlphaFloat(textColor);
            float g = ColorHelper.getAlphaFloat(integer);
            return f != 1.0F ? ColorHelper.withAlpha(ColorHelper.channelFromFloat(f * g), integer) : integer;
         } else {
            return this.shadow ? ColorHelper.scaleRgb(textColor, 0.25F) : 0;
         }
      }

      void drawGlyphs() {
         for (BakedGlyph.DrawnGlyph drawnGlyph : this.glyphs) {
            BakedGlyph bakedGlyph = drawnGlyph.glyph();
            VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(bakedGlyph.getLayer(this.layerType));
            bakedGlyph.draw(drawnGlyph, this.matrix, vertexConsumer, this.light);
         }
      }

      private float getForegroundZIndex() {
         return this.swapZIndex ? 0.01F : -0.01F;
      }

      private float getBackgroundZIndex() {
         return this.swapZIndex ? -0.01F : 0.01F;
      }
   }

   public enum TextLayerType {
      NORMAL,
      SEE_THROUGH,
      POLYGON_OFFSET;
   }
}
