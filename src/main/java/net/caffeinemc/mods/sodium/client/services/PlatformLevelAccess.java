package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

public interface PlatformLevelAccess {
   PlatformLevelAccess INSTANCE = Services.load(PlatformLevelAccess.class);

   static PlatformLevelAccess getInstance() {
      return INSTANCE;
   }

   @Nullable
   Object getBlockEntityData(BlockEntity var1);

   @Nullable
   SodiumAuxiliaryLightManager getLightManager(WorldChunk var1, ChunkSectionPos var2);
}
