package com.viaversion.viafabricplus.protocoltranslator.netty;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.settings.impl.GeneralSettings;
import com.viaversion.viafabricplus.util.ChatUtil;
import com.viaversion.vialoader.netty.ViaDecoder;
import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ViaFabricPlusViaDecoder extends ViaDecoder {
   public ViaFabricPlusViaDecoder(UserConnection connection) {
      super(connection);
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      int mode = GeneralSettings.INSTANCE.ignorePacketTranslationErrors.getIndex();
      if (mode == 0) {
         super.channelRead(ctx, msg);
      } else {
         try {
            super.channelRead(ctx, msg);
         } catch (Throwable t) {
            ViaFabricPlusImpl.INSTANCE.logger().error("Error occurred while decoding packet in ViaFabricPlus decoder", t);
            if (mode == 1) {
               ChatUtil.sendPrefixedMessage(Text.translatable("translation.viafabricplus.packet_error").formatted(Formatting.RED));
            }
         }
      }
   }
}
