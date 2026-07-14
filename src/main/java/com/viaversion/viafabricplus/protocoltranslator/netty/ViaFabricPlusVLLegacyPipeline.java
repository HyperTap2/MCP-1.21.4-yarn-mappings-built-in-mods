package com.viaversion.viafabricplus.protocoltranslator.netty;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.protocoltranslator.protocol.ViaFabricPlusProtocol;
import com.viaversion.vialoader.netty.CompressionReorderEvent;
import com.viaversion.vialoader.netty.VLLegacyPipeline;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public final class ViaFabricPlusVLLegacyPipeline extends VLLegacyPipeline {
   public static final String VIA_FLOW_CONTROL = "via-flow-control";
   public static final String VIABEDROCK_COMPRESSION_HANDLER_NAME = "viabedrock-compression";
   public static final String VIABEDROCK_ENCRYPTION_HANDLER_NAME = "viabedrock-encryption";
   public static final String VIABEDROCK_PING_ENCAPSULATION_HANDLER_NAME = "viabedrock-ping-encapsulation";

   public ViaFabricPlusVLLegacyPipeline(UserConnection connection, ProtocolVersion version) {
      super(connection, version);
   }

   public void handlerAdded(ChannelHandlerContext ctx) {
      super.handlerAdded(ctx);
      ctx.pipeline().addAfter("via-decoder", "via-flow-control", new NoReadFlowControlHandler());
      this.connection.getProtocolInfo().getPipeline().add(ViaFabricPlusProtocol.INSTANCE);
   }

   protected ChannelHandler createViaDecoder() {
      return new ViaFabricPlusViaDecoder(this.connection);
   }

   public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt.getClass().getName().equals("me.steinborn.krypton.mod.shared.misc.KryptonPipelineEvent") && evt.toString().equals("COMPRESSION_ENABLED")) {
         super.userEventTriggered(ctx, CompressionReorderEvent.INSTANCE);
         ViaFabricPlusImpl.INSTANCE.logger().info("Compression has been re-ordered after \"Krypton\"");
      } else {
         super.userEventTriggered(ctx, evt);
      }
   }

   protected String decompressName() {
      return "decompress";
   }

   protected String compressName() {
      return "compress";
   }

   protected String packetDecoderName() {
      return "inbound_config";
   }

   protected String packetEncoderName() {
      return "encoder";
   }

   protected String lengthSplitterName() {
      return "splitter";
   }

   protected String lengthPrependerName() {
      return "prepender";
   }
}
