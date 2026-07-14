package dev.tr7zw.entityculling;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

final class EntityCullingProvider implements DataProvider {
   private final MinecraftClient client;
   private ClientWorld world;

   EntityCullingProvider(MinecraftClient client) {
      this.client = client;
   }

   @Override
   public boolean prepareChunk(int chunkX, int chunkZ) {
      this.world = this.client.world;
      return this.world != null;
   }

   @Override
   public boolean isOpaqueFullCube(int x, int y, int z) {
      BlockState state = this.world.getBlockState(new BlockPos(x, y, z));
      return EntityCullingManager.getInstance().getConfig().solidLeaves && state.getBlock() instanceof LeavesBlock
         || state.isOpaqueFullCube();
   }

   @Override
   public void cleanup() {
      this.world = null;
   }
}
