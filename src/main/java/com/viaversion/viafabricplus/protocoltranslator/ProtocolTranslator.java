package com.viaversion.viafabricplus.protocoltranslator;

import com.mojang.authlib.GameProfile;
import com.viaversion.viafabricplus.api.events.ChangeProtocolVersionCallback;
import com.viaversion.viafabricplus.base.Events;
import com.viaversion.viafabricplus.base.sync_tasks.SyncTasks;
import com.viaversion.viafabricplus.features.classic.cpe_extension.CPEAdditions;
import com.viaversion.viafabricplus.features.entity.attribute.LegacyInteractionRangeAttributes;
import com.viaversion.viafabricplus.features.limitation.max_chat_length.MaxChatLength;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viafabricplus.util.NotificationUtil;
import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import com.viaversion.viafabricplus.protocoltranslator.impl.command.ViaFabricPlusVLCommandHandler;
import com.viaversion.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaLegacyPlatformImpl;
import com.viaversion.viafabricplus.protocoltranslator.impl.platform.ViaFabricPlusViaVersionPlatformImpl;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLInjector;
import com.viaversion.viafabricplus.protocoltranslator.impl.viaversion.ViaFabricPlusVLLoader;
import com.viaversion.viafabricplus.protocoltranslator.netty.ViaFabricPlusVLLegacyPipeline;
import com.viaversion.viafabricplus.protocoltranslator.protocol.ViaFabricPlusProtocol;
import com.viaversion.viafabricplus.protocoltranslator.translator.BlockStateTranslator;
import com.viaversion.viafabricplus.protocoltranslator.translator.TextComponentTranslator;
import com.viaversion.viafabricplus.protocoltranslator.util.NoPacketSendChannel;
import com.viaversion.vialoader.ViaLoader;
import com.viaversion.vialoader.impl.platform.ViaAprilFoolsPlatformImpl;
import com.viaversion.vialoader.impl.platform.ViaBackwardsPlatformImpl;
import com.viaversion.vialoader.impl.platform.ViaBedrockPlatformImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.ProtocolPipeline;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.connection.UserConnectionImpl;
import io.netty.buffer.ByteBuf;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ServerboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.Protocol1_20To1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.storage.ConfigurationState;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21_5;
import com.viaversion.viaversion.rewriter.text.JsonNBTComponentRewriter;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.stream.field.FieldWrapper;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;

public final class ProtocolTranslator {
   private static final JsonNBTComponentRewriter<ClientboundPackets1_13> OPEN_SCREEN_COMPONENT_REWRITER = new JsonNBTComponentRewriter<>(
      null, JsonNBTComponentRewriter.ReadType.JSON
   ) {
      @Override
      protected void handleTranslate(JsonObject object, String translate) {
         super.handleTranslate(object, translate);
         if (translate.startsWith("block.") && translate.endsWith(".name")) {
            object.addProperty("translate", translate.substring(0, translate.length() - 5));
         }
      }
   };
   public static final AttributeKey<ClientConnection> CLIENT_CONNECTION_ATTRIBUTE_KEY = AttributeKey.newInstance("viafabricplus-clientconnection");
   public static final AttributeKey<ProtocolVersion> TARGET_VERSION_ATTRIBUTE_KEY = AttributeKey.newInstance("viafabricplus-targetversion");
   public static final ProtocolVersion NATIVE_VERSION = ProtocolVersion.v1_21_4;
   public static final ProtocolVersion AUTO_DETECT_PROTOCOL = new ProtocolVersion(VersionType.SPECIAL, -2, -1, "Auto Detect (1.7+ servers)", null) {
      protected Comparator<ProtocolVersion> customComparator() {
         return (o1, o2) -> {
            if (o1 == ProtocolTranslator.AUTO_DETECT_PROTOCOL) {
               return 1;
            } else {
               return o2 == ProtocolTranslator.AUTO_DETECT_PROTOCOL ? -1 : 0;
            }
         };
      }

      public boolean isKnown() {
         return false;
      }
   };
   private static ProtocolVersion targetVersion = NATIVE_VERSION;
   private static ProtocolVersion previousVersion = null;

   public static void injectViaPipeline(ClientConnection connection, Channel channel) {
      IClientConnection mixinClientConnection = (IClientConnection)connection;
      ProtocolVersion serverVersion = mixinClientConnection.viaFabricPlus$getTargetVersion();
      if (serverVersion != NATIVE_VERSION) {
         channel.attr(CLIENT_CONNECTION_ATTRIBUTE_KEY).set(connection);
         channel.attr(TARGET_VERSION_ATTRIBUTE_KEY).set(serverVersion);
         if (serverVersion.equals(BedrockProtocolVersion.bedrockLatest)) {
            channel.config().setOption(RakChannelOption.RAK_PROTOCOL_VERSION, 11);
            channel.config().setOption(RakChannelOption.RAK_COMPATIBILITY_MODE, true);
            channel.config().setOption(RakChannelOption.RAK_CLIENT_INTERNAL_ADDRESSES, 20);
            channel.config().setOption(RakChannelOption.RAK_TIME_BETWEEN_SEND_CONNECTION_ATTEMPTS_MS, 500);
            channel.config()
               .setOption(RakChannelOption.RAK_CONNECT_TIMEOUT, ((Integer)channel.config().getOption(ChannelOption.CONNECT_TIMEOUT_MILLIS)).longValue());
            channel.config().setOption(RakChannelOption.RAK_SESSION_TIMEOUT, 30000L);
            channel.config().setOption(RakChannelOption.RAK_GUID, ThreadLocalRandom.current().nextLong());
         }

         UserConnection user = new UserConnectionImpl(channel, true);
         new ProtocolPipelineImpl(user);
         mixinClientConnection.viaFabricPlus$setUserConnection(user);
         channel.pipeline().addLast(new ChannelHandler[]{new ViaFabricPlusVLLegacyPipeline(user, serverVersion)});
      }
   }

   public static ProtocolVersion getTargetVersion() {
      return targetVersion;
   }

   public static ProtocolVersion getTargetVersion(Channel channel) {
      if (channel != null && channel.hasAttr(TARGET_VERSION_ATTRIBUTE_KEY)) {
         return (ProtocolVersion)channel.attr(TARGET_VERSION_ATTRIBUTE_KEY).get();
      } else {
         throw new IllegalStateException("Target version attribute not set");
      }
   }

   public static void setTargetVersion(ProtocolVersion newVersion) {
      setTargetVersion(newVersion, false);
   }

   public static void setTargetVersion(ProtocolVersion newVersion, boolean revertOnDisconnect) {
      if (newVersion != null) {
         ProtocolVersion oldVersion = targetVersion;
         targetVersion = newVersion;
         if (oldVersion != newVersion) {
            if (revertOnDisconnect) {
               previousVersion = oldVersion;
            }

            ((ChangeProtocolVersionCallback)Events.CHANGE_PROTOCOL_VERSION.invoker()).onChangeProtocolVersion(oldVersion, targetVersion);
         }
      }
   }

   public static void injectPreviousVersionReset(Channel channel) {
      if (previousVersion != null) {
         channel.closeFuture().addListener(future -> {
            setTargetVersion(previousVersion);
            previousVersion = null;
         });
      }
   }

   public static UserConnection createDummyUserConnection(ProtocolVersion clientVersion, ProtocolVersion serverVersion) {
      UserConnection user = new DummyUserConnection();
      ProtocolPipeline pipeline = new ProtocolPipelineImpl(user);
      List<ProtocolPathEntry> path = Via.getManager().getProtocolManager().getProtocolPath(clientVersion, serverVersion);
      if (path != null) {
         for (ProtocolPathEntry pair : path) {
            pipeline.add(pair.protocol());
            pair.protocol().init(user);
         }
      }

      ProtocolInfo info = user.getProtocolInfo();
      info.setState(State.PLAY);
      info.setProtocolVersion(clientVersion);
      info.setServerProtocolVersion(serverVersion);
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.player != null) {
         GameProfile profile = mc.player.getGameProfile();
         info.setUsername(profile.getName());
         info.setUuid(profile.getId());
      }

      return user;
   }

   private static final class DummyUserConnection extends UserConnectionImpl {
      private DummyUserConnection() {
         super(NoPacketSendChannel.INSTANCE, true);
      }

      @Override
      public void sendRawPacket(ByteBuf packet) {
         packet.release();
      }

      @Override
      public void scheduleSendRawPacket(ByteBuf packet) {
         packet.release();
      }
   }

   public static UserConnection getPlayNetworkUserConnection() {
      ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
      return handler == null ? null : ((IClientConnection)handler.getConnection()).viaFabricPlus$getUserConnection();
   }

   private static void patchConfigs(Path path) {
      try {
         Path viaVersionConfig = path.resolve("viaversion.yml");
         Files.writeString(
            viaVersionConfig,
            "fix-infested-block-breaking: false\nshield-blocking: false\nno-delay-shield-blocking: true\nhandle-invalid-item-count: true\n",
            StandardOpenOption.CREATE_NEW
         );
      } catch (FileAlreadyExistsException var4) {
      } catch (Throwable e) {
         throw new RuntimeException("Failed to patch ViaVersion config", e);
      }

      try {
         Path viaLegacyConfig = path.resolve("vialegacy.yml");
         Files.writeString(viaLegacyConfig, "legacy-skull-loading: true\nlegacy-skin-loading: true\n", StandardOpenOption.CREATE_NEW);
      } catch (FileAlreadyExistsException var2) {
      } catch (Throwable e) {
         throw new RuntimeException("Failed to patch ViaLegacy config", e);
      }
   }

   private static void changeBedrockProtocolName() {
      ProtocolVersion bedrockLatest = (ProtocolVersion)RStream.of(BedrockProtocolVersion.class).fields().by("bedrockLatest").get();
      FieldWrapper name = RStream.of(bedrockLatest).withSuper().fields().by("name");
      name.set(name.get() + " (Work in progress)");
   }

   public static CompletableFuture<Void> init(Path path) {
      if (SharedConstants.getProtocolVersion() != NATIVE_VERSION.getOriginalVersion()) {
         throw new IllegalStateException("Native version is not the same as the current version");
      }

      patchConfigs(path);
      return CompletableFuture.runAsync(
         () -> {
            ViaLoader.init(
               new ViaFabricPlusViaVersionPlatformImpl(path.toFile()),
               new ViaFabricPlusVLLoader(),
               new ViaFabricPlusVLInjector(),
               new ViaFabricPlusVLCommandHandler(),
               new Supplier[]{
                  ViaBackwardsPlatformImpl::new, ViaFabricPlusViaLegacyPlatformImpl::new, ViaAprilFoolsPlatformImpl::new, ViaBedrockPlatformImpl::new
               }
            );
            CPEAdditions.registerPackets();
            patchProtocolPacketHandlers();
            ProtocolVersion.register(AUTO_DETECT_PROTOCOL);
            changeBedrockProtocolName();
            ViaFabricPlusProtocol.INSTANCE.initialize();
         }
      );
   }

   private static void patchProtocolPacketHandlers() {
      Protocol1_11_1To1_12 recipeProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_11_1To1_12.class);
      recipeProtocol.registerClientbound(
         com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3.LOGIN,
         ClientboundPackets1_12.LOGIN,
         wrapper -> {
            wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.UNSIGNED_BYTE);
            int dimensionId = wrapper.passthrough(Types.INT);
            ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_11_1To1_12.class);
            clientWorld.setEnvironment(dimensionId);
         },
         true
      );

      Protocol1_13_2To1_14 largeContainerProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_13_2To1_14.class);
      largeContainerProtocol.registerClientbound(
         ClientboundPackets1_13.OPEN_SCREEN,
         null,
         wrapper -> handleOpenScreen1_14(largeContainerProtocol, wrapper),
         true
      );
      largeContainerProtocol.registerServerbound(ServerboundPackets1_14.SELECT_TRADE, ServerboundPackets1_13.SELECT_TRADE, null, true);

      Protocol1_20To1_20_2 configurationProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_20To1_20_2.class);
      configurationProtocol.registerServerbound(
         State.CONFIGURATION,
         ServerboundConfigurationPackets1_20_2.CUSTOM_PAYLOAD.getId(),
         -1,
         configurationPacketHandler(ServerboundPackets1_20_2.CUSTOM_PAYLOAD, ServerboundPackets1_19_4.CUSTOM_PAYLOAD),
         true
      );
      configurationProtocol.registerServerbound(
         State.CONFIGURATION,
         ServerboundConfigurationPackets1_20_2.KEEP_ALIVE.getId(),
         -1,
         configurationPacketHandler(ServerboundPackets1_20_2.KEEP_ALIVE, ServerboundPackets1_19_4.KEEP_ALIVE),
         true
      );
      configurationProtocol.registerServerbound(
         State.CONFIGURATION,
         ServerboundConfigurationPackets1_20_2.PONG.getId(),
         -1,
         configurationPacketHandler(ServerboundPackets1_20_2.PONG, ServerboundPackets1_19_4.PONG),
         true
      );

      Protocol1_15_2To1_16 swingingProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_15_2To1_16.class);
      swingingProtocol.registerServerbound(ServerboundPackets1_16.SWING, ServerboundPackets1_14.SWING, wrapper -> {}, true);

      Protocol1_10To1_11 chatLengthProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_10To1_11.class);
      chatLengthProtocol.registerServerbound(
         com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3.CHAT,
         com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3.CHAT,
         wrapper -> {
            String message = wrapper.passthrough(Types.STRING);
            int maxLength = MaxChatLength.getChatLength();
            if (message.length() > maxLength) {
               wrapper.set(Types.STRING, 0, message.substring(0, maxLength));
            }
         },
         true
      );

      Protocol1_21_4To1_21_5 creativeSlotProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_21_4To1_21_5.class);
      creativeSlotProtocol.registerServerbound(
         ServerboundPackets1_21_5.SET_CREATIVE_MODE_SLOT,
         ServerboundPackets1_21_4.SET_CREATIVE_MODE_SLOT,
         wrapper -> {
            wrapper.passthrough(Types.SHORT);
            var itemRewriter = creativeSlotProtocol.getItemRewriter();
            var item = itemRewriter.handleItemToServer(wrapper.user(), wrapper.read(Types1_21_5.LENGTH_PREFIXED_ITEM));
            wrapper.write(itemRewriter.itemType(), item);
         },
         true
      );

      Protocol1_18_2To1_19 blockAckProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_18_2To1_19.class);
      blockAckProtocol.registerClientbound(ClientboundPackets1_18.BLOCK_BREAK_ACK, ClientboundPackets1_19.CUSTOM_PAYLOAD, wrapper -> {
         wrapper.resetReader();
         String taskId = SyncTasks.executeSyncTask(data -> {
            try {
               BlockPos pos = data.readBlockPos();
               BlockState blockState = BlockStateTranslator.via1_18_2toMc(data.readVarInt());
               Action action = data.readEnumConstant(Action.class);
               boolean allGood = data.readBoolean();
               MinecraftClient.getInstance()
                  .interactionManager
                  .viaFabricPlus$get1_18_2InteractionManager()
                  .handleBlockBreakAck(pos, blockState, action, allGood);
            } catch (Throwable throwable) {
               throw new RuntimeException("Failed to handle BlockBreakAck packet data", throwable);
            }
         });
         wrapper.write(Types.STRING, SyncTasks.PACKET_SYNC_IDENTIFIER);
         wrapper.write(Types.STRING, taskId);
      }, true);

      Protocol1_16_4To1_17 containerClickProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_16_4To1_17.class);
      containerClickProtocol.registerServerbound(
         ServerboundPackets1_17.CONTAINER_CLICK,
         ServerboundPackets1_16_2.CONTAINER_CLICK,
         wrapper -> {
            NotificationUtil.warnIncompatibilityPacket(
               "1.17", "CONTAINER_CLICK", "ClientPlayerInteractionManager#clickSlot", "MultiPlayerGameMode#handleInventoryMouseClick"
            );
            wrapper.cancel();
         },
         true
      );

      Protocol1_19_3To1_19_4 entityProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_19_3To1_19_4.class);
      entityProtocol.registerClientbound(
         ClientboundPackets1_19_3.TELEPORT_ENTITY, ClientboundPackets1_19_4.TELEPORT_ENTITY, wrapper -> {}, true
      );

      Protocol1_20_3To1_20_5 commandProtocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_20_3To1_20_5.class);
      LegacyInteractionRangeAttributes.register(commandProtocol);
      commandProtocol.registerServerbound(ServerboundPackets1_20_5.CHAT, ServerboundPackets1_20_3.CHAT, null, true);
      commandProtocol.registerServerbound(ServerboundPackets1_20_5.CHAT_ACK, ServerboundPackets1_20_3.CHAT_ACK, null, true);
      commandProtocol.registerServerbound(
         ServerboundPackets1_20_5.CHAT_SESSION_UPDATE, ServerboundPackets1_20_3.CHAT_SESSION_UPDATE, null, true
      );
      commandProtocol.registerServerbound(
         ServerboundPackets1_20_5.CHAT_COMMAND_SIGNED, ServerboundPackets1_20_3.CHAT_COMMAND, null, true
      );
      commandProtocol.registerServerbound(
         ServerboundPackets1_20_5.CHAT_COMMAND,
         ServerboundPackets1_20_3.CHAT_COMMAND,
         wrapper -> {
            NotificationUtil.warnIncompatibilityPacket(
               "1.20.5", "CHAT_COMMAND", "ClientPlayNetworkHandler#sendChatCommand", "ClientPacketListener#sendCommand"
            );
            wrapper.cancel();
         },
         true
      );
   }

   private static void handleOpenScreen1_14(Protocol1_13_2To1_14 protocol, PacketWrapper wrapper) {
      short windowId = wrapper.read(Types.UNSIGNED_BYTE);
      String type = wrapper.read(Types.STRING);
      JsonElement title = wrapper.read(Types.COMPONENT);
      OPEN_SCREEN_COMPONENT_REWRITER.processText(wrapper.user(), title);
      short slots = wrapper.read(Types.UNSIGNED_BYTE);

      if (type.equals("EntityHorse")) {
         wrapper.setPacketType(ClientboundPackets1_14.HORSE_SCREEN_OPEN);
         int entityId = wrapper.read(Types.INT);
         wrapper.write(Types.UNSIGNED_BYTE, windowId);
         wrapper.write(Types.VAR_INT, (int)slots);
         wrapper.write(Types.INT, entityId);
         return;
      }

      if ((type.equals("minecraft:container") || type.equals("minecraft:chest")) && (slots > 54 || slots <= 0)) {
         String taskId = SyncTasks.executeSyncTask(data -> {
            MinecraftClient client = MinecraftClient.getInstance();
            try {
               int syncId = data.readUnsignedByte();
               int size = data.readUnsignedByte();
               Text screenTitle = (Text)TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC.decode(data);
               GenericContainerScreenHandler screenHandler = new GenericContainerScreenHandler(
                  null,
                  syncId,
                  client.player.getInventory(),
                  new SimpleInventory(size),
                  MathHelper.ceil(size / 9.0F)
               );
               client.player.currentScreenHandler = screenHandler;
               client.setScreen(new GenericContainerScreen(screenHandler, client.player.getInventory(), screenTitle));
            } catch (Throwable throwable) {
               throw new RuntimeException("Failed to handle OpenWindow packet data", throwable);
            }
         });
         wrapper.clearPacket();
         wrapper.setPacketType(ClientboundPackets1_14.CUSTOM_PAYLOAD);
         wrapper.write(Types.STRING, SyncTasks.PACKET_SYNC_IDENTIFIER);
         wrapper.write(Types.STRING, taskId);
         wrapper.write(Types.UNSIGNED_BYTE, windowId);
         wrapper.write(Types.UNSIGNED_BYTE, slots);
         wrapper.write(Types.TAG, TextComponentTranslator.via1_14toViaLatest(title));
         return;
      }

      wrapper.setPacketType(ClientboundPackets1_14.OPEN_SCREEN);
      wrapper.write(Types.VAR_INT, (int)windowId);
      int typeId = switch (type) {
         case "minecraft:crafting_table" -> 11;
         case "minecraft:furnace" -> 13;
         case "minecraft:dropper", "minecraft:dispenser" -> 6;
         case "minecraft:enchanting_table" -> 12;
         case "minecraft:brewing_stand" -> 10;
         case "minecraft:villager" -> 18;
         case "minecraft:beacon" -> 8;
         case "minecraft:anvil" -> 7;
         case "minecraft:hopper" -> 15;
         case "minecraft:shulker_box" -> 19;
         default -> slots > 0 && slots <= 54 ? slots / 9 - 1 : -1;
      };
      if (typeId == -1) {
         protocol.getLogger().warning("Can't open inventory for player! Type: " + type + " Size: " + slots);
      }

      wrapper.write(Types.VAR_INT, typeId);
      wrapper.write(Types.COMPONENT, title);
   }

   private static PacketHandler configurationPacketHandler(
      ServerboundPackets1_20_2 configurationPacketType, ServerboundPackets1_19_4 playPacketType
   ) {
      return wrapper -> {
         if (Boolean.TRUE.equals(DebugSettings.INSTANCE.queueConfigPackets.getValue())) {
            wrapper.setPacketType(configurationPacketType);
            wrapper.user().get(ConfigurationState.class).addPacketToQueue(wrapper, false);
            wrapper.cancel();
         } else {
            wrapper.setPacketType(playPacketType);
         }
      };
   }
}
