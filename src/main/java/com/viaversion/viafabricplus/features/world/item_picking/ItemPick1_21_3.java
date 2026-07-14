package com.viaversion.viafabricplus.features.world.item_picking;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.Protocol1_21_2To1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

public final class ItemPick1_21_3 {
   private static void addPickBlock(PlayerInventory inventory, ItemStack stack) {
      int index = inventory.getSlotWithStack(stack);
      if (PlayerInventory.isValidHotbarIndex(index)) {
         inventory.selectedSlot = index;
      } else if (index != -1) {
         inventory.swapSlotWithHotbar(index);
      } else {
         inventory.swapStackWithHotbar(stack);
      }
   }

   private static void addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity, DynamicRegistryManager manager) {
      NbtCompound nbtCompound = blockEntity.createComponentlessNbtWithIdentifyingData(manager);
      blockEntity.removeFromCopiedStackNbt(nbtCompound);
      BlockItem.setBlockEntityData(stack, blockEntity.getType(), nbtCompound);
      stack.applyComponentsFrom(blockEntity.createComponentMap());
   }

   public static void doItemPick(MinecraftClient client) {
      boolean creativeMode = client.player.getAbilities().creativeMode;
      HitResult crosshairTarget = client.crosshairTarget;
      ItemStack itemStack;
      if (crosshairTarget.getType() == Type.BLOCK) {
         BlockPos blockPos = ((BlockHitResult)crosshairTarget).getBlockPos();
         BlockState blockState = client.world.getBlockState(blockPos);
         if (blockState.isAir()) {
            return;
         }

         Block block = blockState.getBlock();
         itemStack = block.viaFabricPlus$getPickStack(client.world, blockPos, blockState, false);
         if (itemStack.isEmpty()) {
            return;
         }

         if (creativeMode && Screen.hasControlDown() && blockState.hasBlockEntity()) {
            BlockEntity blockEntity = client.world.getBlockEntity(blockPos);
            if (blockEntity != null) {
               addBlockEntityNbt(itemStack, blockEntity, client.world.getRegistryManager());
            }
         }
      } else {
         if (crosshairTarget.getType() != Type.ENTITY || !creativeMode) {
            return;
         }

         Entity entity = ((EntityHitResult)crosshairTarget).getEntity();
         itemStack = entity.getPickBlockStack();
         if (itemStack == null) {
            return;
         }
      }

      if (!itemStack.isEmpty()) {
         PlayerInventory inventory = client.player.getInventory();
         int index = inventory.getSlotWithStack(itemStack);
         if (creativeMode) {
            addPickBlock(inventory, itemStack);
            client.interactionManager.clickCreativeStack(client.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
         } else if (index != -1) {
            if (PlayerInventory.isValidHotbarIndex(index)) {
               inventory.selectedSlot = index;
               return;
            }

            PacketWrapper pickFromInventory = PacketWrapper.create(ServerboundPackets1_21_2.PICK_ITEM, ProtocolTranslator.getPlayNetworkUserConnection());
            pickFromInventory.write(Types.VAR_INT, index);
            pickFromInventory.scheduleSendToServer(Protocol1_21_2To1_21_4.class);
         }
      }
   }
}
