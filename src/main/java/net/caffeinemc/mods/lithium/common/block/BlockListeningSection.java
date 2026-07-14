package net.caffeinemc.mods.lithium.common.block;

import net.caffeinemc.mods.lithium.common.tracking.block.SectionedBlockChangeTracker;
import net.minecraft.world.World;

public interface BlockListeningSection {
   void lithium$addToCallback(SectionedBlockChangeTracker tracker, long sectionPos, World world);

   void lithium$removeFromCallback(SectionedBlockChangeTracker tracker);
}
