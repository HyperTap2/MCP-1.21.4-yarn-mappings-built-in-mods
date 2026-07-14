package com.viaversion.viafabricplus.protocoltranslator.impl.provider.vialegacy;

import com.viaversion.viafabricplus.protocoltranslator.translator.ItemTranslator;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.alpha.a1_2_3_5_1_2_6tob1_0_1_1_1.Protocola1_2_3_5_1_2_6Tob1_0_1_1_1;
import net.raphimc.vialegacy.protocol.alpha.a1_2_3_5_1_2_6tob1_0_1_1_1.provider.AlphaInventoryProvider;

public final class ViaFabricPlusAlphaInventoryProvider extends AlphaInventoryProvider {
   public boolean usesInventoryTracker() {
      return false;
   }

   private Item[] getMinecraftContainerItems(List<ItemStack> trackingItems) {
      Item[] items = new Item[trackingItems.size()];

      for (int i = 0; i < items.length; i++) {
         ItemStack alphaItem = trackingItems.get(i);
         if (!alphaItem.isEmpty()) {
            items[i] = ItemTranslator.mcToVia(alphaItem, LegacyProtocolVersion.b1_8tob1_8_1);
         }
      }

      return Protocola1_2_3_5_1_2_6Tob1_0_1_1_1.copyItems(items);
   }

   public Item[] getMainInventoryItems(UserConnection connection) {
      return this.getPlayer() == null ? new Item[37] : this.getMinecraftContainerItems(this.getPlayer().getInventory().main);
   }

   public Item[] getCraftingInventoryItems(UserConnection connection) {
      return this.getPlayer() == null ? new Item[4] : this.getMinecraftContainerItems(this.getPlayer().playerScreenHandler.getCraftingInput().getHeldStacks());
   }

   public Item[] getArmorInventoryItems(UserConnection connection) {
      return this.getPlayer() == null ? new Item[4] : this.getMinecraftContainerItems(this.getPlayer().getInventory().armor);
   }

   public Item[] getContainerItems(UserConnection connection) {
      return this.getPlayer() == null ? new Item[37] : this.getMinecraftContainerItems(this.getPlayer().currentScreenHandler.getStacks());
   }

   public void addToInventory(UserConnection connection, Item item) {
      this.getPlayer().getInventory().insertStack(ItemTranslator.viaToMc(item, LegacyProtocolVersion.b1_8tob1_8_1));
   }

   private ClientPlayerEntity getPlayer() {
      return MinecraftClient.getInstance().player;
   }
}
