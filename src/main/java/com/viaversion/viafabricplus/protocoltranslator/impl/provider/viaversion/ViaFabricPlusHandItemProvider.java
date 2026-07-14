package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viaversion;

import com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocols.v1_8to1_9.provider.HandItemProvider;
import net.minecraft.item.ItemStack;

public final class ViaFabricPlusHandItemProvider extends HandItemProvider {
   public static ItemStack lastUsedItem = null;

   public Item getHandItem(UserConnection info) {
      return lastUsedItem != null && !lastUsedItem.isEmpty() ? ItemTranslator.mcToVia(lastUsedItem, ProtocolVersion.v1_8) : null;
   }
}
