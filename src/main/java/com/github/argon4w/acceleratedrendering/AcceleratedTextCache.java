package com.github.argon4w.acceleratedrendering;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.function.ToDoubleFunction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;

/** Small bounded cache for immutable raw strings used by HUD and nameplate text. */
public final class AcceleratedTextCache {
   private static final int MAX_ENTRIES = Integer.getInteger("acceleratedrendering.textCacheSize", 4096);
   private static final Map<String, Float> WIDTHS = new LinkedHashMap<>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, Float> eldest) {
         return this.size() > MAX_ENTRIES;
      }
   };
   private static final Map<String, CachedText> RUNS = new LinkedHashMap<>(256, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, CachedText> eldest) {
         return this.size() > MAX_ENTRIES;
      }
   };
   private static final Map<OrderedText, CachedText> ORDERED_RUNS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<OrderedText, Float> ORDERED_WIDTHS = Collections.synchronizedMap(new WeakHashMap<>());

   private AcceleratedTextCache() {
   }

   public static float getWidth(String text, ToDoubleFunction<String> computer) {
      if (!AcceleratedRendering.isAvailable() || text.length() > 512) {
         return (float)computer.applyAsDouble(text);
      }

      synchronized (WIDTHS) {
         Float cached = WIDTHS.get(text);
         if (cached != null) {
            return cached;
         }
      }

      float width = (float)computer.applyAsDouble(text);
      synchronized (WIDTHS) {
         WIDTHS.put(text, width);
      }
      return width;
   }

   public static void visitFormatted(String text, CharacterVisitor visitor) {
      if (!AcceleratedRendering.isAvailable() || text.length() > 512) {
         TextVisitFactory.visitFormatted(text, Style.EMPTY, visitor);
         return;
      }

      CachedText run;
      synchronized (RUNS) {
         run = RUNS.get(text);
      }

      if (run == null) {
         List<StyledCodePoint> codePoints = new ArrayList<>(text.length());
         TextVisitFactory.visitFormatted(text, Style.EMPTY, (index, style, codePoint) -> {
            codePoints.add(new StyledCodePoint(index, style, codePoint));
            return true;
         });
         run = new CachedText(codePoints.toArray(StyledCodePoint[]::new));
         synchronized (RUNS) {
            RUNS.put(text, run);
         }
      }

      run.accept(visitor);
   }

   public static void visitOrdered(OrderedText text, CharacterVisitor visitor) {
      if (!AcceleratedRendering.isAvailable()) {
         text.accept(visitor);
         return;
      }

      CachedText run = ORDERED_RUNS.get(text);
      if (run == null) {
         List<StyledCodePoint> codePoints = new ArrayList<>();
         text.accept((index, style, codePoint) -> {
            codePoints.add(new StyledCodePoint(index, style, codePoint));
            return true;
         });
         run = new CachedText(codePoints.toArray(StyledCodePoint[]::new));
         ORDERED_RUNS.put(text, run);
      }

      run.accept(visitor);
   }

   public static float getWidth(OrderedText text, ToDoubleFunction<OrderedText> computer) {
      if (!AcceleratedRendering.isAvailable()) {
         return (float)computer.applyAsDouble(text);
      }

      Float cached = ORDERED_WIDTHS.get(text);
      if (cached != null) {
         return cached;
      }

      float width = (float)computer.applyAsDouble(text);
      ORDERED_WIDTHS.put(text, width);
      return width;
   }

   public static void clear() {
      synchronized (WIDTHS) {
         WIDTHS.clear();
      }
      synchronized (RUNS) {
         RUNS.clear();
      }
      ORDERED_RUNS.clear();
      ORDERED_WIDTHS.clear();
   }

   private record StyledCodePoint(int index, Style style, int codePoint) {
   }

   private record CachedText(StyledCodePoint[] codePoints) {
      void accept(CharacterVisitor visitor) {
         for (StyledCodePoint codePoint : this.codePoints) {
            if (!visitor.accept(codePoint.index, codePoint.style, codePoint.codePoint)) {
               return;
            }
         }
      }
   }
}
