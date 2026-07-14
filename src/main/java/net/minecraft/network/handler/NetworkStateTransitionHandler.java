package net.minecraft.network.handler;

import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.packet.Packet;

public interface NetworkStateTransitionHandler {
   static void onDecoded(ChannelHandlerContext context, Packet<?> packet) {
      if (packet.transitionsNetworkState()) {
         if (ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_5)) {
            context.channel().config().setAutoRead(false);
         }
         context.pipeline().addBefore(context.name(), "inbound_config", new NetworkStateTransitions.InboundConfigurer());
         context.pipeline().remove(context.name());
      }
   }

   static void onEncoded(ChannelHandlerContext context, Packet<?> packet) {
      if (packet.transitionsNetworkState()) {
         context.pipeline().addAfter(context.name(), "outbound_config", new NetworkStateTransitions.OutboundConfigurer());
         context.pipeline().remove(context.name());
      }
   }
}
