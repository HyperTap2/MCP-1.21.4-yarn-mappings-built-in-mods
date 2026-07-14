package net.minecraft.client.font;

import java.util.function.Function;

public interface Glyph {
   float getAdvance();

   default float getAdvance(boolean bold) {
      return this.getAdvance() + (bold ? this.getBoldOffset() : 0.0F);
   }

   default float getBoldOffset() {
      return 1.0F;
   }

   default float getShadowOffset() {
      return 1.0F;
   }

   BakedGlyph bake(Function<RenderableGlyph, BakedGlyph> glyphRendererGetter);

   interface EmptyGlyph extends Glyph {
      @Override
      default BakedGlyph bake(Function<RenderableGlyph, BakedGlyph> function) {
         return EmptyBakedGlyph.INSTANCE;
      }
   }
}
