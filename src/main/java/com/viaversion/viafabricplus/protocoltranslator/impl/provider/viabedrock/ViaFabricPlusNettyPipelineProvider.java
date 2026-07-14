package com.viaversion.viafabricplus.protocoltranslator.impl.provider.viabedrock;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.channel.Channel;
import javax.crypto.SecretKey;
import net.raphimc.viabedrock.api.io.compression.ProtocolCompression;
import net.raphimc.viabedrock.netty.AesEncryptionCodec;
import net.raphimc.viabedrock.netty.CompressionCodec;
import net.raphimc.viabedrock.protocol.provider.NettyPipelineProvider;

public final class ViaFabricPlusNettyPipelineProvider extends NettyPipelineProvider {
   public void enableCompression(UserConnection user, ProtocolCompression protocolCompression) {
      Channel channel = user.getChannel();
      if (channel.pipeline().names().contains("viabedrock-compression")) {
         throw new IllegalStateException("Compression already enabled");
      }

      channel.pipeline().addBefore("splitter", "viabedrock-compression", new CompressionCodec(protocolCompression));
   }

   public void enableEncryption(UserConnection user, SecretKey key) {
      Channel channel = user.getChannel();
      if (channel.pipeline().names().contains("viabedrock-encryption")) {
         throw new IllegalStateException("Encryption already enabled");
      }

      try {
         channel.pipeline().addAfter("viabedrock-frame-encapsulation", "viabedrock-encryption", new AesEncryptionCodec(key));
      } catch (Throwable e) {
         throw new RuntimeException(e);
      }
   }
}
