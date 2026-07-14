package net.caffeinemc.mods.sodium.client.model.color;

import java.util.Arrays;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;

public class DefaultColorProviders {
   public static ColorProvider<BlockState> adapt(BlockColorProvider color) {
      return new DefaultColorProviders.VanillaAdapter(color);
   }

   public static class FoliageColorProvider<T> extends BlendedColorProvider<T> {
      public static final ColorProvider<BlockState> BLOCKS = new DefaultColorProviders.FoliageColorProvider<>();

      private FoliageColorProvider() {
      }

      @Override
      protected int getColor(LevelSlice slice, T state, BlockPos pos) {
         return 0xFF000000 | BiomeColors.getFoliageColor(slice, pos);
      }
   }

   public static class GrassColorProvider<T> extends BlendedColorProvider<T> {
      public static final ColorProvider<BlockState> BLOCKS = new DefaultColorProviders.GrassColorProvider<>();

      private GrassColorProvider() {
      }

      @Override
      protected int getColor(LevelSlice slice, T state, BlockPos pos) {
         return 0xFF000000 | BiomeColors.getGrassColor(slice, pos);
      }
   }

   private static class VanillaAdapter implements ColorProvider<BlockState> {
      private final BlockColorProvider color;

      private VanillaAdapter(BlockColorProvider color) {
         this.color = color;
      }

      public void getColors(LevelSlice slice, BlockPos pos, Mutable scratchPos, BlockState state, ModelQuadView quad, int[] output) {
         Arrays.fill(output, 0xFF000000 | this.color.getColor(state, slice, pos, quad.getTintIndex()));
      }
   }
}
