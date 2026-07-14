package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.Nullable;

public interface Font extends AutoCloseable {
   float field_48382 = 7.0F;

   @Override
   default void close() {
   }

   @Nullable
   default Glyph getGlyph(int codePoint) {
      return null;
   }

   IntSet getProvidedGlyphs();

   record FontFilterPair(Font provider, FontFilterType.FilterMap filter) implements AutoCloseable {
      @Override
      public void close() {
         this.provider.close();
      }
   }
}
