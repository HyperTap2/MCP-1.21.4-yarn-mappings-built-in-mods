package com.viaversion.viafabricplus.protocoltranslator.util;

import com.google.gson.JsonObject;
import com.viaversion.viafabricplus.save.AbstractSave;
import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.util.Formatting;

public final class ProtocolVersionDetector {
   private static final int TIMEOUT = 3000;

   public static ProtocolVersion get(ServerAddress serverAddress, InetSocketAddress socketAddress, ProtocolVersion clientVersion) throws Exception {
      try (
         Socket socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
         DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
         DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         DataOutputStream handshakePacket = new DataOutputStream(byteArrayOutputStream);
      ) {
         socket.setTcpNoDelay(true);
         socket.setSoTimeout(3000);
         handshakePacket.writeByte(0);
         writeVarInt(handshakePacket, clientVersion.getOriginalVersion());
         if (clientVersion.olderThanOrEqualTo(ProtocolVersion.v1_17)) {
            writeVarString(handshakePacket, serverAddress.getAddress());
            handshakePacket.writeShort(serverAddress.getPort());
         } else {
            writeVarString(handshakePacket, socketAddress.getHostString());
            handshakePacket.writeShort(socketAddress.getPort());
         }

         writeVarInt(handshakePacket, ConnectionIntent.STATUS.getId());
         writeVarInt(dataOutputStream, byteArrayOutputStream.size());
         dataOutputStream.write(byteArrayOutputStream.toByteArray());
         dataOutputStream.writeByte(1);
         dataOutputStream.writeByte(0);
         int size = readVarInt(dataInputStream);
         if (size <= 0) {
            throw new IllegalStateException("Invalid packet size");
         }

         int id = readVarInt(dataInputStream);
         if (id != 0) {
            throw new IllegalStateException("Invalid packet ID");
         }

         String response = readVarString(dataInputStream);
         JsonObject object = (JsonObject)AbstractSave.GSON.fromJson(response, JsonObject.class);
         if (!object.has("version")) {
            throw new IllegalStateException("Invalid ping response");
         }

         JsonObject version = object.getAsJsonObject("version");
         if (!version.has("name") || !version.has("protocol")) {
            throw new IllegalStateException("Invalid ping response");
         }

         int serverVersion = version.get("protocol").getAsInt();
         if (clientVersion.getOriginalVersion() == serverVersion) {
            return clientVersion;
         }

         if (ProtocolVersion.isRegistered(serverVersion)) {
            return ProtocolVersion.getProtocol(serverVersion);
         }

         String name = version.get("name").getAsString();

         for (ProtocolVersion protocol : ProtocolVersionList.getProtocolsNewToOld()) {
            for (String includedVersion : protocol.getIncludedVersions()) {
               if (name.contains(includedVersion)) {
                  return protocol;
               }
            }
         }

         throw new RuntimeException(
            "Unable to detect the server version\nServer sent an invalid protocol id: " + serverAddress + " (" + name + Formatting.RESET + ")"
         );
      }
   }

   private static int readVarInt(DataInputStream in) throws IOException {
      int i = 0;
      int j = 0;

      byte b;
      do {
         b = in.readByte();
         i |= (b & 127) << j++ * 7;
         if (j > 5) {
            throw new IOException("Var int too big");
         }
      } while ((b & 128) == 128);

      return i;
   }

   private static String readVarString(DataInputStream in) throws IOException {
      int length = readVarInt(in);
      if (length > 131068) {
         throw new IOException("Cannot receive string longer than Short.MAX_VALUE * 4 bytes (got " + length + " bytes)");
      } else if (length < 0) {
         throw new IOException("Cannot receive string shorter than 0 bytes (got " + length + " bytes)");
      } else {
         byte[] bytes = new byte[length];
         in.readFully(bytes);
         String string = new String(bytes, StandardCharsets.UTF_8);
         if (string.length() > 32767) {
            throw new IOException("Cannot receive string longer than Short.MAX_VALUE characters (got " + string.length() + " bytes)");
         } else {
            return string;
         }
      }
   }

   private static void writeVarInt(DataOutputStream out, int value) throws IOException {
      while ((value & -128) != 0) {
         out.writeByte(value & 127 | 128);
         value >>>= 7;
      }

      out.writeByte(value);
   }

   private static void writeVarString(DataOutputStream out, String value) throws IOException {
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      writeVarInt(out, bytes.length);
      out.write(bytes);
   }
}
