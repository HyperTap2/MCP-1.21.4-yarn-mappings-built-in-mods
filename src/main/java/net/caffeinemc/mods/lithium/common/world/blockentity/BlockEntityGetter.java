package net.caffeinemc.mods.lithium.common.world.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface BlockEntityGetter {
   @Nullable
   BlockEntity lithium$getLoadedExistingBlockEntity(BlockPos pos);
}
