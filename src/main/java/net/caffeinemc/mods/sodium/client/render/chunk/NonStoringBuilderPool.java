package net.caffeinemc.mods.sodium.client.render.chunk;

import java.util.Collections;
import net.minecraft.client.render.chunk.BlockBufferAllocatorStorage;
import net.minecraft.client.render.chunk.BlockBufferBuilderPool;
import org.jetbrains.annotations.Nullable;

public class NonStoringBuilderPool extends BlockBufferBuilderPool {
   public NonStoringBuilderPool() {
      super(Collections.emptyList());
   }

   @Nullable
   @Override
   public BlockBufferAllocatorStorage acquire() {
      return null;
   }

   @Override
   public void release(BlockBufferAllocatorStorage builders) {
   }

   @Override
   public boolean hasNoAvailableBuilder() {
      return true;
   }

   @Override
   public int getAvailableBuilderCount() {
      return 0;
   }
}
