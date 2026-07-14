package net.caffeinemc.mods.sodium.client.model.light.data;

import java.util.Arrays;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class SingleBlockLightDataCache extends LightDataAccess {
   private static final int NEIGHBOR_BLOCK_RADIUS = 2;
   private static final int BLOCK_LENGTH = 5;
   private final int[] light = new int[125];
   private int xOffset;
   private int yOffset;
   private int zOffset;

   public void reset(BlockPos origin, BlockRenderView blockView) {
      this.xOffset = origin.getX() - 2;
      this.yOffset = origin.getY() - 2;
      this.zOffset = origin.getZ() - 2;
      Arrays.fill(this.light, 0);
      this.level = blockView;
   }

   public void release() {
      this.level = null;
   }

   private int index(int x, int y, int z) {
      int x2 = x - this.xOffset;
      int y2 = y - this.yOffset;
      int z2 = z - this.zOffset;
      return z2 * 5 * 5 + y2 * 5 + x2;
   }

   @Override
   public int get(int x, int y, int z) {
      int l = this.index(x, y, z);
      int word = this.light[l];
      return word != 0 ? word : (this.light[l] = this.compute(x, y, z));
   }
}
