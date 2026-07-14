package com.viaversion.viafabricplus.features.entity.attribute;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.GameMode;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.exception.InformativeException;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import java.util.UUID;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public final class LegacyInteractionRangeAttributes {
   private static final UUID CREATIVE_BLOCK_RANGE = UUID.fromString("736565d2-e1a7-403d-a3f8-1aeb3e302542");
   private static final UUID CREATIVE_ENTITY_RANGE = UUID.fromString("98491ef6-97b1-4584-ae82-71a8cc85cf73");

   private LegacyInteractionRangeAttributes() {
   }

   public static void register(Protocol1_20_3To1_20_5 protocol) {
      protocol.appendClientbound(ClientboundPackets1_20_3.LOGIN, wrapper -> sendIfRequired(wrapper.user(), isCreative(wrapper.get(Types.BYTE, 0))));
      protocol.appendClientbound(ClientboundPackets1_20_3.RESPAWN, wrapper -> sendIfRequired(wrapper.user(), isCreative(wrapper.get(Types.BYTE, 0))));
      protocol.appendClientbound(ClientboundPackets1_20_3.GAME_EVENT, wrapper -> {
         if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 3) {
            sendIfRequired(wrapper.user(), isCreative((byte)Math.floor(wrapper.get(Types.FLOAT, 0) + 0.5F)));
         }
      });
   }

   private static boolean isCreative(byte gameMode) {
      return gameMode == GameMode.CREATIVE.id();
   }

   private static void sendIfRequired(UserConnection connection, boolean creativeMode) throws InformativeException {
      ProtocolVersion serverVersion = connection.getProtocolInfo().serverProtocolVersion();
      if (serverVersion.newerThan(ProtocolVersion.v1_13_2)) {
         return;
      }

      PacketWrapper attributes = PacketWrapper.create(ClientboundPackets1_20_5.UPDATE_ATTRIBUTES, connection);
      attributes.write(Types.VAR_INT, connection.getEntityTracker(Protocol1_20_3To1_20_5.class).clientEntityId());
      if (serverVersion.olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
         attributes.write(Types.VAR_INT, 3);
         writeAttribute(attributes, "generic.step_height", 0.5, null, 0.0);
      } else {
         attributes.write(Types.VAR_INT, 2);
      }

      boolean preRelease = serverVersion.olderThan(LegacyProtocolVersion.r1_0_0tor1_0_1);
      writeAttribute(attributes, "player.block_interaction_range", preRelease ? 4.0 : 4.5,
         creativeMode ? CREATIVE_BLOCK_RANGE : null, preRelease ? 1.0 : 0.5);

      double entityRangeBonus = serverVersion.olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)
         ? 3.0
         : 1.0;
      writeAttribute(attributes, "player.entity_interaction_range", 3.0,
         creativeMode ? CREATIVE_ENTITY_RANGE : null, entityRangeBonus);
      attributes.scheduleSend(Protocol1_20_3To1_20_5.class);
   }

   private static void writeAttribute(PacketWrapper wrapper, String key, double baseValue, UUID modifierId, double modifierAmount) {
      wrapper.write(Types.STRING, key);
      wrapper.write(Types.DOUBLE, baseValue);
      wrapper.write(Types.VAR_INT, modifierId == null ? 0 : 1);
      if (modifierId != null) {
         wrapper.write(Types.UUID, modifierId);
         wrapper.write(Types.DOUBLE, modifierAmount);
         wrapper.write(Types.BYTE, (byte)0);
      }
   }
}
