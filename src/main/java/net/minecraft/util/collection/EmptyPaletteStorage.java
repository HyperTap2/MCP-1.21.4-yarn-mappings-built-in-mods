package net.minecraft.util.collection;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntConsumer;
import net.caffeinemc.mods.sodium.client.world.BitStorageExtension;
import net.minecraft.world.chunk.Palette;
import org.apache.commons.lang3.Validate;

public class EmptyPaletteStorage implements PaletteStorage, BitStorageExtension {
   public static final long[] EMPTY_DATA = new long[0];
   private final int size;

   public EmptyPaletteStorage(int size) {
      this.size = size;
   }

   @Override
   public int swap(int index, int value) {
      return 0;
   }

   @Override
   public void set(int index, int value) {
   }

   @Override
   public int get(int index) {
      return 0;
   }

   @Override
   public long[] getData() {
      return EMPTY_DATA;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public int getElementBits() {
      return 0;
   }

   @Override
   public void forEach(IntConsumer action) {
      for (int i = 0; i < this.size; i++) {
         action.accept(0);
      }
   }

   @Override
   public void writePaletteIndices(int[] out) {
      Arrays.fill(out, 0, this.size, 0);
   }

   @Override
   public PaletteStorage copy() {
      return this;
   }

   @Override
   public <T> void sodium$unpack(T[] out, Palette<T> palette) {
      if (this.size != out.length) {
         throw new IllegalArgumentException("Array has mismatched size");
      }
      Arrays.fill(out, Objects.requireNonNull(palette.get(0), "Palette must have default entry"));
   }
}
