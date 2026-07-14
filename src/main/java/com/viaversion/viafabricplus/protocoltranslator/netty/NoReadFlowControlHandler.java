package com.viaversion.viafabricplus.protocoltranslator.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.flow.FlowControlHandler;

public final class NoReadFlowControlHandler extends FlowControlHandler {
   public void read(ChannelHandlerContext ctx) throws Exception {
      if (ctx.channel().config().isAutoRead()) {
         super.read(ctx);
      }
   }
}
