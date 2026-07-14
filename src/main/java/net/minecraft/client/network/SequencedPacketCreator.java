package net.minecraft.client.network;

import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

@FunctionalInterface
public interface SequencedPacketCreator {
   Packet<ServerPlayPacketListener> predict(int sequence);
}
