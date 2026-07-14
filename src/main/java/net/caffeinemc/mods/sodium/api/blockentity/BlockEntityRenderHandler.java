package net.caffeinemc.mods.sodium.api.blockentity;

import net.caffeinemc.mods.sodium.api.internal.DependencyInjection;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
@AvailableSince("0.6.0")
public interface BlockEntityRenderHandler {
   BlockEntityRenderHandler INSTANCE = DependencyInjection.load(
      BlockEntityRenderHandler.class, "net.caffeinemc.mods.sodium.client.render.chunk.BlockEntityRenderHandlerImpl"
   );

   static BlockEntityRenderHandler instance() {
      return INSTANCE;
   }

   <T extends BlockEntity> void addRenderPredicate(BlockEntityType<T> var1, BlockEntityRenderPredicate<T> var2);

   <T extends BlockEntity> boolean removeRenderPredicate(BlockEntityType<T> var1, BlockEntityRenderPredicate<T> var2);
}
