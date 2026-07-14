package com.viaversion.viafabricplus.features.font.replace_blank_glyph;

import java.util.function.Function;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.RenderableGlyph;

public enum BuiltinEmptyGlyph1_12_2 implements Glyph {
   INSTANCE;

   private static final int WIDTH = 0;
   private static final int HEIGHT = 8;

   public float getAdvance() {
      return 0.0F;
   }

   public BakedGlyph bake(Function<RenderableGlyph, BakedGlyph> glyphRendererGetter) {
      return glyphRendererGetter.apply(new RenderableGlyph() {
         public int getWidth() {
            return 0;
         }

         public int getHeight() {
            return 8;
         }

         public float getOversample() {
            return 1.0F;
         }

         public void upload(int x, int y) {
         }

         public boolean hasColor() {
            return true;
         }
      });
   }
}
