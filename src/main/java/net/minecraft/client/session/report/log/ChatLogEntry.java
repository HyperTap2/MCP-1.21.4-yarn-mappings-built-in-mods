package net.minecraft.client.session.report.log;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.util.StringIdentifiable;

public interface ChatLogEntry {
   Codec<ChatLogEntry> CODEC = StringIdentifiable.createCodec(ChatLogEntry.Type::values).dispatch(ChatLogEntry::getType, ChatLogEntry.Type::getCodec);

   ChatLogEntry.Type getType();

   enum Type implements StringIdentifiable {
      PLAYER("player", () -> ReceivedMessage.ChatMessage.CHAT_MESSAGE_CODEC),
      SYSTEM("system", () -> ReceivedMessage.GameMessage.GAME_MESSAGE_CODEC);

      private final String id;
      private final Supplier<MapCodec<? extends ChatLogEntry>> codecSupplier;

      Type(final String id, final Supplier<MapCodec<? extends ChatLogEntry>> codecSupplier) {
         this.id = id;
         this.codecSupplier = codecSupplier;
      }

      private MapCodec<? extends ChatLogEntry> getCodec() {
         return this.codecSupplier.get();
      }

      public String asString() {
         return this.id;
      }
   }
}
