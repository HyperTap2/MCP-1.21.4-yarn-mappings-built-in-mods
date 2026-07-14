package net.caffeinemc.mods.lithium.common.world.chunk;

import net.minecraft.world.chunk.Palette;

public interface CompactingPackedIntegerArray {
   <T> void lithium$compact(Palette<T> sourcePalette, Palette<T> destinationPalette, short[] output);
}
