package net.irisshaders.iris.api.v0.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.joml.Vector3f;

public interface IrisItemLightProvider {
   Vector3f DEFAULT_LIGHT_COLOR = new Vector3f(1.0F, 1.0F, 1.0F);

   default int getLightEmission(PlayerEntity player, ItemStack stack) {
      return stack.getItem() instanceof BlockItem item ? item.getBlock().getDefaultState().getLuminance() : 0;
   }

   default Vector3f getLightColor(PlayerEntity player, ItemStack stack) {
      return DEFAULT_LIGHT_COLOR;
   }
}
