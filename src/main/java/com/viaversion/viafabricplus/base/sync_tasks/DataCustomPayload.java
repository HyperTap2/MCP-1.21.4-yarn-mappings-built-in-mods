package com.viaversion.viafabricplus.base.sync_tasks;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;

public record DataCustomPayload(PacketByteBuf buf) implements CustomPayload {
   public static final Id<DataCustomPayload> ID = new Id(Identifier.of(SyncTasks.PACKET_SYNC_IDENTIFIER));
   public static final PacketCodec<PacketByteBuf, DataCustomPayload> CODEC = CustomPayload.codecOf((value, buf) -> {
      throw new UnsupportedOperationException("DataCustomPayload is a read-only packet");
   }, buf -> new DataCustomPayload(new PacketByteBuf(Unpooled.copiedBuffer(buf.readSlice(buf.readableBytes())))));

   public static void init() {
   }

   public Id<? extends CustomPayload> getId() {
      return ID;
   }
}
