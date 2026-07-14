package net.caffeinemc.mods.sodium.api.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Experimental;

@FunctionalInterface
@Experimental
@AvailableSince("0.6.0")
public interface BlockEntityRenderPredicate<T extends BlockEntity> {
   boolean shouldRender(BlockView var1, BlockPos var2, T var3);
}
