package com.viaversion.viafabricplus.protocoltranslator.translator;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.protocoltranslator.protocol.ViaFabricPlusProtocol;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.Direction;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_4;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocol.beta.b1_8_0_1tor1_0_0_1.types.Typesb1_8_0_1;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.types.Types1_2_4;
import net.raphimc.vialegacy.protocol.release.r1_4_2tor1_4_4_5.types.Types1_4_2;
import net.raphimc.vialegacy.protocol.release.r1_7_6_10tor1_8.types.Types1_7_6;

public final class ItemTranslator {
   public static Item mcToVia(ItemStack stack, ProtocolVersion targetVersion) {
      UserConnection connection = ProtocolTranslator.createDummyUserConnection(ProtocolTranslator.NATIVE_VERSION, targetVersion);

      try {
         RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), MinecraftClient.getInstance().getNetworkHandler().getRegistryManager());
         buf.writeShort(0);
         ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, stack);
         PacketWrapper setCreativeModeSlot = PacketWrapper.create(ViaFabricPlusProtocol.getSetCreativeModeSlot(), buf, connection);
         connection.getProtocolInfo().getPipeline().transform(Direction.SERVERBOUND, State.PLAY, setCreativeModeSlot);
         setCreativeModeSlot.read(Types.SHORT);
         return (Item)setCreativeModeSlot.read(getServerboundItemType(targetVersion));
      } catch (Throwable t) {
         ViaFabricPlusImpl.INSTANCE.logger().error("Error converting native item stack to ViaVersion {} item stack", targetVersion, t);
         return null;
      }
   }

   public static ItemStack viaToMc(Item item, ProtocolVersion sourceVersion) {
      UserConnection connection = ProtocolTranslator.createDummyUserConnection(ProtocolTranslator.NATIVE_VERSION, sourceVersion);

      try {
         Protocol<?, ?, ?, ?> sourceProtocol = connection.getProtocolInfo()
            .getPipeline()
            .reversedPipes()
            .stream()
            .filter(p -> !p.isBaseProtocol())
            .findFirst()
            .orElseThrow();
         PacketWrapper containerSetSlot = PacketWrapper.create(
            sourceProtocol.getPacketTypesProvider().unmappedClientboundType(State.PLAY, ClientboundPackets1_12_1.CONTAINER_SET_SLOT.getName()), connection
         );
         if (sourceVersion.newerThanOrEqualTo(ProtocolVersion.v1_8)) {
            containerSetSlot.write(Types.UNSIGNED_BYTE, (short)0);
         } else {
            containerSetSlot.write(Types.BYTE, (byte)0);
         }

         containerSetSlot.write(Types.SHORT, (short)0);
         containerSetSlot.write(getClientboundItemType(sourceVersion), item != null ? item.copy() : null);
         containerSetSlot.resetReader();
         containerSetSlot.user().getProtocolInfo().getPipeline().transform(Direction.CLIENTBOUND, State.PLAY, containerSetSlot);
         RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), MinecraftClient.getInstance().getNetworkHandler().getRegistryManager());
         containerSetSlot.setPacketType(null);
         containerSetSlot.writeToBuffer(buf);
         buf.readUnsignedByte();
         buf.readVarInt();
         buf.readShort();
         return (ItemStack)ItemStack.OPTIONAL_PACKET_CODEC.decode(buf);
      } catch (Throwable t) {
         ViaFabricPlusImpl.INSTANCE.logger().error("Error converting ViaVersion {} item to native item stack", sourceVersion, t);
         return ItemStack.EMPTY;
      }
   }

   public static Type<Item> getServerboundItemType(ProtocolVersion targetVersion) {
      return targetVersion.olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1) ? Typesb1_8_0_1.CREATIVE_ITEM : getClientboundItemType(targetVersion);
   }

   public static Type<Item> getClientboundItemType(ProtocolVersion targetVersion) {
      if (targetVersion.olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
         return Types1_4_2.NBTLESS_ITEM;
      } else if (targetVersion.olderThanOrEqualTo(LegacyProtocolVersion.r1_2_4tor1_2_5)) {
         return Types1_2_4.NBT_ITEM;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_8)) {
         return Types1_7_6.ITEM;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_13)) {
         return Types.ITEM1_8;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_13_2)) {
         return Types.ITEM1_13;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_20_2)) {
         return Types.ITEM1_13_2;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_20_5)) {
         return Types.ITEM1_20_2;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_21)) {
         return Types1_20_5.ITEM;
      } else if (targetVersion.olderThan(ProtocolVersion.v1_21_2)) {
         return Types1_21.ITEM;
      } else {
         return targetVersion.olderThan(ProtocolVersion.v1_21_4) ? Types1_21_2.ITEM : Types1_21_4.ITEM;
      }
   }
}
