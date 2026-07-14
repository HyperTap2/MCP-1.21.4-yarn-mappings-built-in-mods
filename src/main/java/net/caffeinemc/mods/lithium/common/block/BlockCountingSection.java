package net.caffeinemc.mods.lithium.common.block;

import net.minecraft.block.BlockState;

public interface BlockCountingSection {
   boolean lithium$mayContainAny(TrackedBlockStatePredicate predicate);

   void lithium$trackBlockStateChange(BlockState newState, BlockState oldState);
}
