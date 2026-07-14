package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.viaversion.viafabricplus.injection.access.base.IClientConnection;
import com.viaversion.viafabricplus.settings.impl.BedrockSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.ServerMetadata.Players;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import org.slf4j.Logger;

public class MultiplayerServerListPinger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Text CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   private final List<ClientConnection> clientConnections = Collections.synchronizedList(Lists.newArrayList());

   public void add(ServerInfo entry, Runnable saver, Runnable pingCallback) throws UnknownHostException {
      final ServerAddress serverAddress = ServerAddress.parse(
         BedrockSettings.replaceDefaultPort(entry.address, entry.viaFabricPlus$forcedVersion())
      );
      Optional<InetSocketAddress> optional = AllowedAddressResolver.DEFAULT.resolve(serverAddress).map(Address::getInetSocketAddress);
      if (optional.isEmpty()) {
         this.showError(ConnectScreen.UNKNOWN_HOST_TEXT, entry);
      } else {
         final InetSocketAddress inetSocketAddress = optional.get();
         MultiValueDebugSampleLogImpl packetSizeLog = null;
         if (entry.viaFabricPlus$forcedVersion() != null && !entry.viaFabricPlus$passedDirectConnectScreen()) {
            packetSizeLog = new MultiValueDebugSampleLogImpl(1);
            packetSizeLog.viaFabricPlus$setForcedVersion(entry.viaFabricPlus$forcedVersion());
         }
         entry.viaFabricPlus$passDirectConnectScreen(false);
         final ClientConnection clientConnection = ClientConnection.connect(inetSocketAddress, false, packetSizeLog);
         this.clientConnections.add(clientConnection);
         entry.label = Text.translatable("multiplayer.status.pinging");
         entry.playerListSummary = Collections.emptyList();
         ClientQueryPacketListener clientQueryPacketListener = new ClientQueryPacketListener() {
            private boolean sentQuery;
            private boolean received;
            private long startTime;

            public void onResponse(QueryResponseS2CPacket packet) {
               if (this.received) {
                  clientConnection.disconnect(Text.translatable("multiplayer.status.unrequested"));
                } else {
                   this.received = true;
                   entry.viaFabricPlus$setTranslatingVersion(((IClientConnection)clientConnection).viaFabricPlus$getTargetVersion());
                   ServerMetadata serverMetadata = packet.metadata();
                  entry.label = serverMetadata.description();
                  serverMetadata.version().ifPresentOrElse(version -> {
                     entry.version = Text.literal(version.gameVersion());
                     entry.protocolVersion = version.protocolVersion();
                  }, () -> {
                     entry.version = Text.translatable("multiplayer.status.old");
                     entry.protocolVersion = 0;
                  });
                  serverMetadata.players().ifPresentOrElse(players -> {
                     entry.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(players.online(), players.max());
                     entry.players = players;
                     if (!players.sample().isEmpty()) {
                        List<Text> list = new ArrayList<>(players.sample().size());

                        for (GameProfile gameProfile : players.sample()) {
                           list.add(Text.literal(gameProfile.getName()));
                        }

                        if (players.sample().size() < players.online()) {
                           list.add(Text.translatable("multiplayer.status.and_more", new Object[]{players.online() - players.sample().size()}));
                        }

                        entry.playerListSummary = list;
                     } else {
                        entry.playerListSummary = List.of();
                     }
                  }, () -> entry.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY));
                  serverMetadata.favicon().ifPresent(favicon -> {
                     if (!Arrays.equals(favicon.iconBytes(), entry.getFavicon())) {
                        entry.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
                        saver.run();
                     }
                  });
                  this.startTime = Util.getMeasuringTimeMs();
                   clientConnection.send(new QueryPingC2SPacket(this.startTime));
                   ProtocolVersion translatingVersion = ((IClientConnection)clientConnection).viaFabricPlus$getTargetVersion();
                   if (translatingVersion != null && translatingVersion.getVersion() == entry.protocolVersion) {
                      entry.protocolVersion = net.minecraft.SharedConstants.getProtocolVersion();
                   }
                   this.sentQuery = true;
               }
            }

            public void onPingResult(PingResultS2CPacket packet) {
               long l = this.startTime;
               long m = Util.getMeasuringTimeMs();
               entry.ping = m - l;
               clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
               pingCallback.run();
            }

            public void onDisconnected(DisconnectionInfo info) {
               if (!this.sentQuery) {
                  MultiplayerServerListPinger.this.showError(info.reason(), entry);
                  MultiplayerServerListPinger.this.ping(inetSocketAddress, serverAddress, entry);
               }
            }

            public boolean isConnectionOpen() {
               return clientConnection.isOpen();
            }
         };

         try {
            clientConnection.connect(serverAddress.getAddress(), serverAddress.getPort(), clientQueryPacketListener);
            clientConnection.send(QueryRequestC2SPacket.INSTANCE);
         } catch (Throwable throwable) {
            LOGGER.error("Failed to ping server {}", serverAddress, throwable);
         }
      }
   }

   void showError(Text error, ServerInfo info) {
      LOGGER.error("Can't ping {}: {}", info.address, error.getString());
      info.label = CANNOT_CONNECT_TEXT;
      info.playerCountLabel = ScreenTexts.EMPTY;
   }

   void ping(InetSocketAddress socketAddress, ServerAddress address, ServerInfo serverInfo) {
   }

   public static Text createPlayerCountText(int current, int max) {
      Text text = Text.literal(Integer.toString(current)).formatted(Formatting.GRAY);
      Text text2 = Text.literal(Integer.toString(max)).formatted(Formatting.GRAY);
      return Text.translatable("multiplayer.status.player_count", new Object[]{text, text2}).formatted(Formatting.DARK_GRAY);
   }

   public void tick() {
      synchronized (this.clientConnections) {
         Iterator<ClientConnection> iterator = this.clientConnections.iterator();

         while (iterator.hasNext()) {
            ClientConnection clientConnection = iterator.next();
            if (clientConnection.isOpen()) {
               clientConnection.tick();
            } else {
               iterator.remove();
               clientConnection.handleDisconnection();
            }
         }
      }
   }

   public void cancel() {
      synchronized (this.clientConnections) {
         Iterator<ClientConnection> iterator = this.clientConnections.iterator();

         while (iterator.hasNext()) {
            ClientConnection clientConnection = iterator.next();
            if (clientConnection.isOpen()) {
               iterator.remove();
               clientConnection.disconnect(Text.translatable("multiplayer.status.cancelled"));
            }
         }
      }
   }
}
