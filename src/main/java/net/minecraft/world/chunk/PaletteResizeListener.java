package net.minecraft.world.chunk;

public interface PaletteResizeListener<T> {
   int onResize(int newBits, T object);
}
