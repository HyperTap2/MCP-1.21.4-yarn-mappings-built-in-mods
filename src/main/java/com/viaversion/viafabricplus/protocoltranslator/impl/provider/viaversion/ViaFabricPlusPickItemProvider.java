package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion;

import com.viaversion.viafabricplus.util.NotificationUtil;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.provider.PickItemProvider;

public final class ViaFabricPlusPickItemProvider extends PickItemProvider {
   public void pickItemFromBlock(UserConnection connection, BlockPosition blockPosition, boolean includeData) {
      NotificationUtil.warnIncompatibilityPacket("1.21.4", "PICK_ITEM_FROM_BLOCK", "MinecraftClient#doItemPick", "Minecraft#pickBlock");
   }

   public void pickItemFromEntity(UserConnection connection, int entityId, boolean includeData) {
      NotificationUtil.warnIncompatibilityPacket("1.21.4", "PICK_ITEM_FROM_ENTITY", "MinecraftClient#doItemPick", "Minecraft#pickBlock");
   }
}
