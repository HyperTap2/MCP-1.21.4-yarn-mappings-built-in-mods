package net.caffeinemc.mods.sodium.client.model.quad.blender;

import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;

public abstract class BlendedColorProvider<T> implements ColorProvider<T> {
   @Override
   public void getColors(LevelSlice slice, BlockPos pos, Mutable scratchPos, T state, ModelQuadView quad, int[] output) {
      for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
         output[vertexIndex] = this.getVertexColor(slice, pos, scratchPos, quad, state, vertexIndex);
      }
   }

   private int getVertexColor(LevelSlice slice, BlockPos pos, Mutable scratchPos, ModelQuadView quad, T state, int vertexIndex) {
      float x = quad.getX(vertexIndex) - 0.5F;
      float y = quad.getY(vertexIndex) - 0.5F;
      float z = quad.getZ(vertexIndex) - 0.5F;
      int intX = MathHelper.floor(x);
      int intY = MathHelper.floor(y);
      int intZ = MathHelper.floor(z);
      float fracX = x - intX;
      float fracY = y - intY;
      float fracZ = z - intZ;
      int blockX = pos.getX() + intX;
      int blockY = pos.getY() + intY;
      int blockZ = pos.getZ() + intZ;
      int m00 = this.getColor(slice, state, scratchPos.set(blockX + 0, blockY, blockZ + 0));
      int m01 = this.getColor(slice, state, scratchPos.set(blockX + 0, blockY, blockZ + 1));
      int m10 = this.getColor(slice, state, scratchPos.set(blockX + 1, blockY, blockZ + 0));
      int m11 = this.getColor(slice, state, scratchPos.set(blockX + 1, blockY, blockZ + 1));
      return ColorMixer.mix2d(m00, m01, m10, m11, fracX, fracZ);
   }

   protected abstract int getColor(LevelSlice var1, T var2, BlockPos var3);
}
