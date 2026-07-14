package com.viaversion.viafabricplus.features.emulation.armor_hud;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.data.ArmorTypes1_8;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public final class ArmorHudEmulation1_8 {
   private static final UUID ARMOR_POINTS_UUID = UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150");
   private static double previousArmorPoints = 0.0;

   public static void init() {
   }

   public static void tick(ClientWorld world) {
      if (DebugSettings.INSTANCE.emulateArmorHud.isEnabled()) {
         if (MinecraftClient.getInstance().player != null) {
            UserConnection connection = ProtocolTranslator.getPlayNetworkUserConnection();
            if (connection != null) {
               try {
                  sendArmorUpdate(connection);
               } catch (Throwable throwable) {
                  ViaFabricPlusImpl.INSTANCE.logger().error("Error sending armor update", throwable);
               }
            }
         } else {
            previousArmorPoints = 0.0;
         }
      }
   }

   private static void sendArmorUpdate(UserConnection connection) {
      int armor = 0;

      for (ItemStack stack : MinecraftClient.getInstance().player.getInventory().armor) {
         armor += ArmorTypes1_8.findByType(Registries.ITEM.getId(stack.getItem()).toString()).getArmorPoints();
      }

      if (armor != previousArmorPoints) {
         previousArmorPoints = armor;
         PacketWrapper updateAttributes = PacketWrapper.create(ClientboundPackets1_9.UPDATE_ATTRIBUTES, connection);
         updateAttributes.write(Types.VAR_INT, MinecraftClient.getInstance().player.getId());
         updateAttributes.write(Types.INT, 1);
         updateAttributes.write(Types.STRING, "generic.armor");
         updateAttributes.write(Types.DOUBLE, 0.0);
         updateAttributes.write(Types.VAR_INT, 1);
         updateAttributes.write(Types.UUID, ARMOR_POINTS_UUID);
         updateAttributes.write(Types.DOUBLE, (double)armor);
         updateAttributes.write(Types.BYTE, (byte)0);
         updateAttributes.scheduleSend(Protocol1_8To1_9.class);
      }
   }

}
