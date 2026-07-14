package net.caffeinemc.mods.sodium.client.model.color;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;

public interface ColorProvider<T> {
   void getColors(LevelSlice var1, BlockPos var2, Mutable var3, T var4, ModelQuadView var5, int[] var6);
}
