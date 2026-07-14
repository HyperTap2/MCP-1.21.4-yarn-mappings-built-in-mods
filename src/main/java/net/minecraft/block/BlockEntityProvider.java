package net.minecraft.block;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

public interface BlockEntityProvider {
   @Nullable
   BlockEntity createBlockEntity(BlockPos pos, BlockState state);

   @Nullable
   default <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return null;
   }

   @Nullable
   default <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
      return blockEntity instanceof GameEventListener.Holder<?> holder ? holder.getEventListener() : null;
   }
}
