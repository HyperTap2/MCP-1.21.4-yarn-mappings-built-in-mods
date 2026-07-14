package net.minecraft.network;

import java.util.function.Supplier;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;

public interface PacketCallbacks {
   static PacketCallbacks always(Runnable runnable) {
      return new PacketCallbacks() {
         @Override
         public void onSuccess() {
            runnable.run();
         }

         @Nullable
         @Override
         public Packet<?> getFailurePacket() {
            runnable.run();
            return null;
         }
      };
   }

   static PacketCallbacks of(Supplier<Packet<?>> failurePacket) {
      return new PacketCallbacks() {
         @Nullable
         @Override
         public Packet<?> getFailurePacket() {
            return failurePacket.get();
         }
      };
   }

   default void onSuccess() {
   }

   @Nullable
   default Packet<?> getFailurePacket() {
      return null;
   }
}
